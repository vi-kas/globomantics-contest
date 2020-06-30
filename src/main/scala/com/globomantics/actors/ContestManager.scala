package com.globomantics.actors

import java.util.UUID

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import com.globomantics.persistance.Model.{Contest, Participant}

object ContestManager {

  sealed trait Command

  case class GetContests(replyTo: ActorRef[GetContestsResponse]) extends Command
  case class GetContestsResponse(success: Boolean, contests: Either[Exception, List[Contest]])

  case class  GetContest(id: UUID, replyTo: ActorRef[GetContestResponse]) extends Command
  case class  GetContestResponse(success: Boolean, contest: Either[Exception, Contest])

  case class  CreateContest(id: UUID, title: String, durationInMinutes: Double) extends Command
  case class  RemoveContest(id: UUID) extends Command

  case class StartContest(id: UUID) extends ContestManager.Command with ContestActor.Command
  case class StopContest(id: UUID) extends ContestManager.Command with ContestActor.Command

  case class RegisterParticipant(contestId: UUID, participant: Participant) extends ContestManager.Command with ContestActor.Command
  case class UnRegisterParticipant(contestId: UUID, participantId: UUID) extends ContestManager.Command with ContestActor.Command

  case class JoinContest(contestId: UUID, participantId: UUID) extends ContestManager.Command with ContestActor.Command
  case class LeaveContest(contestId: UUID, participantId: UUID) extends ContestManager.Command with ContestActor.Command

  def apply(currentContests: Map[UUID, Contest] = Map.empty,
            contests: Map[UUID, ActorRef[ContestActor.Command]] = Map.empty[UUID, ActorRef[ContestActor.Command]],
            activeContests: Map[UUID, ActorRef[ContestActor.Command]] = Map.empty[UUID, ActorRef[ContestActor.Command]]): Behavior[Command] =
    Behaviors
      .receive { (ctx, msg) =>

        msg match {
          case GetContest(contestId, replyTo) if contests.contains(contestId) =>
            ctx.log.info("Contest with id: {} is being returned.", contestId)
            replyTo ! GetContestResponse(success = true, Right(currentContests(contestId)))
            Behaviors.same

          case GetContest(contestId, replyTo) =>
            ctx.log.info("Contest with id: {} is not available.", contestId)
            replyTo ! GetContestResponse(success = false, Left(new IllegalStateException(s"No contest available with id: $contestId")))
            Behaviors.same

          case GetContests(replyTo) =>
            ctx.log.info("Returning all Contests.")
            replyTo ! GetContestsResponse(success = true, Right(currentContests.values.toList))
            Behaviors.same

          case CreateContest(contestId, _, _) if contests.contains(contestId) =>
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

          case RemoveContest(contestId) if contests.contains(contestId) =>
            ctx.log.info("Removing Contest with id: {} ...", contestId)
            //Gracefully stop contest with id: contestId ToDO
            this(currentContests.-(contestId), contests.-(contestId), activeContests)

          case RemoveContest(contestId) =>
            ctx.log.info("Contest with id: {} isn't available.", contestId)
            Behaviors.same

          case StartContest(contestId) if contests.contains(contestId) && !activeContests.contains(contestId) =>
            ctx.log.info("Starting Contest with id: {} ...", contestId)
            this(currentContests, contests, activeContests + (contestId -> contests(contestId)))

          case StartContest(contestId) if activeContests.contains(contestId) =>
            ctx.log.info("Contest with id: {} already started.", contestId)
            Behaviors.same

          case StartContest(contestId) =>
            ctx.log.info("Contest with id: {} doesn't exist.", contestId)
            Behaviors.same

          case StopContest(contestId) if activeContests.contains(contestId) =>
            ctx.log.info("Contest with id: {} is being stopped.", contestId)
            this(currentContests, contests, activeContests.-(contestId))

          case StopContest(contestId) =>
            ctx.log.info("No Contest with id: {} is active.", contestId)
            Behaviors.same

          case regParticipant @ RegisterParticipant(contestId, participant) if contests.contains(contestId) =>
            ctx.log.info("Participant with id: {} is being registered for contestId: {}.", participant.id, contestId: Any)
            contests(contestId) ! regParticipant
            Behaviors.same

          case RegisterParticipant(contestId, _) =>
            ctx.log.info("Contest with id: {} is not available.", contestId)
            Behaviors.same

          case joinContest @ JoinContest(contestId, participantId) if activeContests.contains(contestId) =>
            ctx.log.info(s"Contest with id: {} active, {} requested to join.", contestId, participantId: Any)
            activeContests(contestId) ! joinContest
            Behaviors.same

          case JoinContest(contestId, participantId) =>
            ctx.log.info(s"Contest with id: {} isn't active, {} can't join.", contestId, participantId: Any)
            Behaviors.same

          case other =>
            ctx.log.info("No impl for request: {}", other)
            Behaviors.same
        }

      }
}