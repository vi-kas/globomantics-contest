package com.globomantics.actors

import java.util.UUID

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import com.globomantics.persistance.Model.Participant

object ContestManager {

  sealed trait Command

  case class CreateContest(id: UUID, title: String, durationInMins: Double) extends Command
  case class RemoveContest(id: UUID) extends Command

  case class StartContest(id: UUID) extends ContestManager.Command with Contest.Command
  case class StopContest(id: UUID) extends ContestManager.Command with Contest.Command

  case class RegisterParticipant(contestId: UUID, participant: Participant) extends ContestManager.Command with Contest.Command
  case class UnRegisterParticipant(contestId: UUID, participantId: UUID) extends ContestManager.Command with Contest.Command

  case class JoinContest(contestId: UUID, participantId: UUID) extends ContestManager.Command with Contest.Command
  case class LeaveContest(contestId: UUID, participantId: UUID) extends ContestManager.Command with Contest.Command

  def apply(
             contests: Map[UUID, ActorRef[Contest.Command]] = Map.empty[UUID, ActorRef[Contest.Command]],
             activeContests: Map[UUID, ActorRef[Contest.Command]] = Map.empty[UUID, ActorRef[Contest.Command]]): Behavior[Command] =
    Behaviors
      .receive { (ctx, msg) =>

        msg match {
          case CreateContest(contestId, _, _) if contests.contains(contestId) =>
            ctx.log.info("Contest with id: {} is already created ...", contestId)
            Behaviors.same

          case CreateContest(contestId, _, _) =>
            ctx.log.info("Creating Contest with id: {} ...", contestId)
            val contestActor = ctx.spawn(Contest(), s"contest-$contestId")
            this(contests + (contestId -> contestActor), activeContests)

          case RemoveContest(contestId) if contests.contains(contestId) =>
            ctx.log.info("Removing Contest with id: {} ...", contestId)
            //Gracefully stop contest with id: contestId ToDO
            this(contests.-(contestId), activeContests)

          case RemoveContest(contestId) =>
            ctx.log.info("Contest with id: {} isn't available.", contestId)
            Behaviors.same

          case StartContest(contestId) if contests.contains(contestId) && !activeContests.contains(contestId) =>
            ctx.log.info("Starting Contest with id: {} ...", contestId)
            this(contests, activeContests + (contestId -> contests(contestId)))

          case StartContest(contestId) if activeContests.contains(contestId) =>
            ctx.log.info("Contest with id: {} already started.", contestId)
            Behaviors.same

          case StartContest(contestId) =>
            ctx.log.info("Contest with id: {} doesn't exist.", contestId)
            Behaviors.same

          case StopContest(contestId) if activeContests.contains(contestId) =>
            ctx.log.info("Contest with id: {} is being stopped.", contestId)
            this(contests, activeContests.-(contestId))

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