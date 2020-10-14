package com.globomantics.actors

import java.net.ConnectException
import java.util.UUID
import java.util.concurrent.TimeUnit

import akka.actor.typed._
import akka.actor.typed.scaladsl.{Behaviors, Routers}
import akka.util.Timeout
import com.globomantics.actors.ContestActor.{Active, Completed, ContestResponse}
import com.globomantics.actors.DatabaseManager.DBContestCreated
import com.globomantics.persistance.ContestDaoImpl
import com.globomantics.persistance.Model.Contest

import scala.concurrent.duration.FiniteDuration
import scala.util.{Failure, Success}

object ContestManager {

  implicit val timeout: Timeout = Timeout.apply(FiniteDuration(3, TimeUnit.SECONDS))

  sealed trait Command

  case class  CreateContest(id: UUID, title: String, durationInMinutes: Double) extends Command
  case class  GetContest(id: UUID, replyTo: ActorRef[GetContestResponse]) extends Command

  case class StartContest(id: UUID) extends Command
  case class StopContest(id: UUID) extends Command

  case class WrappedContestResponse(contestId: UUID, resp: ContestActor.Response) extends Command
  case class WrappedDatabaseResult(contestId: UUID, resp: DatabaseManager.Result) extends Command

  case class RegisterParticipant(contestId: UUID, participantId: UUID) extends Command
  case class AdaptedRegisterCommand(participantId: UUID, success: Boolean) extends Command

  sealed trait Response
  case class  GetContestResponse(success: Boolean, contest: Either[Exception, Contest]) extends Response

  def apply(currentContests: Map[UUID, Contest] = Map.empty,
            contests: Map[UUID, ActorRef[ContestActor.Command]] = Map.empty,
            activeContests: Set[UUID] = Set.empty): Behavior[Command] =
    Behaviors.setup {ctx =>

      val supervisedBehavior =
        Behaviors
          .supervise(DatabaseManager(() => new ContestDaoImpl))
          .onFailure[ConnectException](SupervisorStrategy.restart)

      val dispatcher = DispatcherSelector.fromConfig("dbm-pool-dispatcher")

      val dbManagerPool = Routers.pool(4)(supervisedBehavior).withRouteeProps(dispatcher)

      val databaseManager = ctx.spawn(dbManagerPool, "db-manager-pool")

      serving(currentContests, contests, activeContests, databaseManager)
    }

  def serving(currentContests: Map[UUID, Contest] = Map.empty,
              contests: Map[UUID, ActorRef[ContestActor.Command]] = Map.empty,
              activeContests: Set[UUID] = Set.empty,
              databaseManager: ActorRef[DatabaseManager.Command]): Behavior[Command] =
    Behaviors
      .receive[Command] { (ctx, msg) =>

      msg match {

        case CreateContest(contestId, _, _) if currentContests.contains(contestId) =>
          ctx.log.info("Contest with id: {} is already created ...", contestId)
          Behaviors.same

        case CreateContest(contestId, name, durationInMinutes) =>
          ctx.log.info("Creating Contest with id: {} ...", contestId)
          val newContest = Contest(contestId, name, durationInMinutes)

          val dbResultActorRef: ActorRef[DatabaseManager.Result] =
            ctx.messageAdapter(r => WrappedDatabaseResult(contestId, r))

          databaseManager ! DatabaseManager.Create(newContest, dbResultActorRef)
          Behaviors.same

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

        case StopContest(contestId) =>
          if(contests.contains(contestId)){
            ctx.log.info("Request to stop contest: {} received.", contestId)

            val contestResponseMapper: ActorRef[ContestActor.Response] =
              ctx.messageAdapter(r => WrappedContestResponse(contestId, r))

            contests(contestId) ! ContestActor.Stop(contestResponseMapper)
          } else {
            ctx.log.info("Contest with id: {} is not available OR started", contestId)
          }
          Behaviors.same

        case w: WrappedContestResponse =>
          w.resp match {
            case ContestResponse(_, Active) =>
              ctx.log.info("Changing contest {} to Active", w.contestId)
              serving(
                currentContests,
                contests,
                activeContests + w.contestId,
                databaseManager
              )
            case ContestResponse(_, Completed) =>
              ctx.log.info("Changing contest {} to InActive", w.contestId)
              serving(
                currentContests,
                contests,
                activeContests - w.contestId,
                databaseManager
              )
            case _ =>
              ctx.log.info("Don't know what to do.", w.contestId)
              Behaviors.same
          }

        case w: WrappedDatabaseResult =>

          w.resp match {

            case DBContestCreated(success, contest) if success =>
              ctx.log.info("Got Wrapped DbManager result {}. Adding contest to currentContests", w.contestId)

              val contestActor = ctx.spawn(ContestActor(), s"contest-${contest.id}")
              serving(
                currentContests + (contest.id -> contest),
                contests + (contest.id -> contestActor),
                activeContests,
                databaseManager
              )

            case DBContestCreated(success, contest) =>
              ctx.log.info("Got Wrapped DbManager result {}.", w.contestId)
              Behaviors.same

            case _ =>
              ctx.log.info("Don't know what to do.", w.contestId)
              Behaviors.same
          }

        case other =>
          ctx.log.info("No impl for request: {}", other)
          Behaviors.same
      }

    }
      .receiveSignal {

        case (ctx, PostStop) =>
          ctx.log.info("ContestManager stopped")
          Behaviors.same

        case (ctx, PreRestart) =>
          ctx.log.info("[ContestManager] Restarting actor {}", ctx.self.path)
          Behaviors.same
      }


}

//
//val supervisedBehavior =
//Behaviors
//.supervise(DatabaseManager(() => new ContestDaoImpl))
//.onFailure[Exception](SupervisorStrategy.restart)

//      val databaseManager = ctx.spawn(supervisedBehavior, "db-manager")
//
//val pool = Routers.pool(poolSize = 4)(supervisedBehavior)
//
//val databaseManager = ctx.spawn(pool, "db-manager-pool")
//
//val dispatcher: DispatcherSelector = DispatcherSelector.fromConfig("dbm-pool-dispatcher")
//
//val dbManagerPool =
//Routers
//.pool(poolSize = 4)(supervisedBehavior)
//.withRouteeProps(dispatcher)
