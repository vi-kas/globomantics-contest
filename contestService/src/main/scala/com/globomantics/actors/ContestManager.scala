package com.globomantics.actors

import java.util.UUID
import java.util.concurrent.TimeUnit

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import akka.util.Timeout
import com.globomantics.actors.ContestActor.{Active, Completed, ContestResponse}
import com.globomantics.persistance.Model.Contest

import scala.concurrent.duration.FiniteDuration
import scala.util.{Failure, Success}

object ContestManager {

  implicit val timeout: Timeout = Timeout.apply(FiniteDuration(3, TimeUnit.SECONDS))

  sealed trait Command

  case class  CreateContest(id: UUID, title: String, durationInMinutes: Double) extends Command

  case class  GetContest(id: UUID, replyTo: ActorRef[GetContestResponse]) extends Command

  case class StartContest(id: UUID) extends Command

  case class WrappedContestResponse(contestId: UUID, resp: ContestActor.Response) extends Command

  case class RegisterParticipant(contestId: UUID, participantId: UUID) extends Command
  case class AdaptedRegisterCommand(participantId: UUID, success: Boolean) extends Command

  sealed trait Response
  case class  GetContestResponse(success: Boolean, contest: Either[Exception, Contest]) extends Response

  def apply(
             currentContests: Map[UUID, Contest] = Map.empty,
             contests: Map[UUID, ActorRef[ContestActor.Command]] = Map.empty,
             activeContests: Set[UUID] = Set.empty): Behavior[Command] =
    Behaviors
      .receive { (ctx, msg) =>

        msg match {

          case CreateContest(contestId, _, _) if currentContests.contains(contestId) =>
            ctx.log.info("Contest with id: {} is already created ...", contestId)
            Behaviors.same

          case CreateContest(contestId, name, durationInMinutes) =>
            ctx.log.info("Creating Contest with id: {} ...", contestId)

            val contestActor = ctx.spawn(ContestActor(), s"contest-$contestId")

            this(
              currentContests + (contestId -> Contest(contestId, name, durationInMinutes)),
              contests + (contestId -> contestActor),
              activeContests
            )

          case GetContest(contestId, replyTo) =>
            if(currentContests.contains(contestId)){
              ctx.log.info("Contest with id: {} is being returned.", contestId)
              replyTo ! GetContestResponse(success = true, Right(currentContests(contestId)))
            } else {
              ctx.log.info("Contest with id: {} is not available.", contestId)
              replyTo ! GetContestResponse(success = false, Left(new IllegalStateException(s"No contest available with id: $contestId")))
            }
            Behaviors.same

          case RegisterParticipant(contestId, participantId) =>
            if(currentContests.contains(contestId)){
              ctx.log.info("Participant with id: {} is being registered for contestId: {}.", participantId, contestId: Any)

              val contestActorRef = contests(contestId)
              ctx.ask(contestActorRef, ContestActor.Register(participantId, _: ActorRef[ContestActor.Response])){

                case Success(ContestActor.RegisterResponse(pId, success, _)) =>
                  ctx.log.warn(s"Participant Registration for id: $pId Success!")
                  AdaptedRegisterCommand(pId, success)

                case Failure(e) =>
                  ctx.log.warn("Participant Registration Failed: {}", e.getCause)
                  AdaptedRegisterCommand(participantId, false)

                case other =>
                  ctx.log.warn("Received {} during Participant Registration", other)
                  AdaptedRegisterCommand(participantId, false)
              }

            } else {
              ctx.log.info("Contest with id: {} is not available.", contestId)
            }
            Behaviors.same

          case AdaptedRegisterCommand(pId, success) =>
            ctx.log.info("Participant [{}] Registration from CA: {}", pId, success: Any)
            // Can do something else with the response as well.
            Behaviors.same

          case StartContest(contestId) =>
            if(contests.contains(contestId) && !activeContests.contains(contestId)){
              ctx.log.info("Request to start contest: {} received.", contestId)

              val contestResponseMapper: ActorRef[ContestActor.Response] =
                ctx.messageAdapter(r => WrappedContestResponse(contestId, r))

              contests(contestId) ! ContestActor.Start(contestResponseMapper)
            } else {
              ctx.log.info("Contest with id: {} is not available OR already started", contestId)
            }
            Behaviors.same

          case w: WrappedContestResponse =>
            w.resp match {
              case ContestResponse(_, Active) =>
                this(
                  currentContests,
                  contests,
                  activeContests + w.contestId
                )
              case ContestResponse(_, Completed) =>
                this(
                  currentContests,
                  contests,
                  activeContests - w.contestId
                )
              case _ =>
                Behaviors.same
            }

          case other =>
            ctx.log.info("No impl for request: {}", other)
            Behaviors.same
        }

      }

}