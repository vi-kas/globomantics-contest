package com.globomantics.actors

import java.util.UUID

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import com.globomantics.persistance.Model._

object Contest {
  import ContestManager.{JoinContest, RegisterParticipant, StartContest, StopContest}

  /**
    * ContestStatus to depict Status for Contest at any given time.
    * It could be
    * - NotStarted
    * - Active
    * - Completed
    */
  sealed trait ContestStatus
  case object NotStarted extends ContestStatus
  case object Active extends ContestStatus
  case object Completed extends ContestStatus

  /** Commands to depict Behavior of Contest Actor */
  trait Command
  case class GetRegistrations(replyTo: ActorRef[GetRegistrationsResponse]) extends Command
  case class GetActiveParticipants(replyTo: ActorRef[GetActiveParticipantsResponse]) extends Command
  case class GetStatus(replyTo: ActorRef[GetStatusResponse]) extends Command
  case class Join(replyTo: ActorRef[JoinContestResponse], participant: Participant) extends Command

  /** Responses to send wherever required */
  sealed trait Response
  case class RegistrationResponse(success: Boolean, description: String)
  case class GetRegistrationsResponse(success: Boolean, registrations: List[Participant])
  case class GetActiveParticipantsResponse(success: Boolean, participants: List[Participant])
  case class GetStatusResponse(status: ContestStatus)
  case class JoinContestResponse(success: Boolean, description: String)

  def apply(
             registrations: Map[UUID, Participant] = Map.empty,
             status: ContestStatus = NotStarted,
             activeParticipants: Map[UUID, ActorRef[ParticipantActor.Command]] = Map.empty): Behavior[Command] =
    notStarted(registrations, status, activeParticipants)

  private def notStarted(registrations: Map[UUID, Participant],
                         status: ContestStatus,
                         activeParticipants: Map[UUID, ActorRef[ParticipantActor.Command]]): Behavior[Command] =
    Behaviors
      .receive { (ctx, msg) =>
        msg match {
          case RegisterParticipant(_, participant) if registrations.contains(participant.id) =>
            ctx.log.info("Participant with username: {} already registered.", participant.username)
            notStarted(registrations, status, activeParticipants)

          case RegisterParticipant(_, participant) =>
            ctx.log.info("Registering Participant with username: {}", participant.id)
            notStarted(registrations + (participant.id -> participant), status, activeParticipants)

          case GetRegistrations(replyTo) =>
            ctx.log.info("Returning Total Registrations.")
            replyTo ! GetRegistrationsResponse(success = true, registrations.values.toList)
            Behaviors.same

          case GetStatus(replyTo) =>
            ctx.log.info("Returning Contest Status.")
            replyTo ! GetStatusResponse(status)
            Behaviors.same

          case StartContest(_) =>
            ctx.log.info("Starting Contest.")
            active(registrations, Active, activeParticipants)
        }

      }

  private def active(registrations: Map[UUID, Participant],
                     status: ContestStatus,
                     activeParticipants: Map[UUID, ActorRef[ParticipantActor.Command]]): Behavior[Command] =
    Behaviors
      .receive { (ctx, msg) =>
        msg match {
          case GetStatus(replyTo) =>
            ctx.log.info("Returning Contest Status.")
            replyTo ! GetStatusResponse(status)
            Behaviors.same

          case JoinContest(_, participantId) if activeParticipants.contains(participantId) =>
            ctx.log.info("Participant {} already joined contest.", participantId)
            Behaviors.same

          case JoinContest(_, participantId) =>
            ctx.log.info("Participant {} Joining Contest.", participantId)
            val participantActor = ctx.spawn(ParticipantActor(), s"participant-$participantId")
            active(registrations, status, activeParticipants + (participantId -> participantActor))

          case StopContest(_) =>
            ctx.log.info("Stopping Contest.")
            completed(registrations, Completed, activeParticipants)
        }

      }

  private def completed(registrations: Map[UUID, Participant],
                        status: ContestStatus,
                        activeParticipants: Map[UUID, ActorRef[ParticipantActor.Command]]): Behavior[Command] =
    Behaviors
      .receive { (ctx, _) =>
        ctx.log.info("Contest is over, can't serve this request anymore.")
        Behaviors.stopped
      }


}
