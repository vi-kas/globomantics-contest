package com.globomantics

import java.util.UUID

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import com.globomantics.persistance.Model.Participant

object Contest {

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
  sealed trait Command
  case class RegisterParticipant(/*replyTo: ActorRef[RegistrationResponse], */participant: Participant) extends Command
  case class GetRegistrations(replyTo: ActorRef[GetRegistrationsResponse]) extends Command
  case class GetActiveParticipants(replyTo: ActorRef[GetActiveParticipantsResponse]) extends Command
  case class GetStatus(replyTo: ActorRef[GetStatusResponse]) extends Command
  case class Join(replyTo: ActorRef[JoinContestResponse], participant: Participant) extends Command
  case object Start extends Command
  case object Stop extends Command

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
             activeParticipants: Map[UUID, Participant] = Map.empty): Behavior[Command] =
    notStarted(registrations, status, activeParticipants)

  private def notStarted(registrations: Map[UUID, Participant],
                         status: ContestStatus,
                         activeParticipants: Map[UUID, Participant]): Behavior[Command] =
    Behaviors
      .receive {

        case (ctx, RegisterParticipant(participant)) if registrations.contains(participant.id) =>
          ctx.log.info("Participant with username: {} already registered.", participant.username)
//          replyTo ! RegistrationResponse(success = false, "Participant has already registered.")
          notStarted(registrations, status, activeParticipants)

        case (ctx, RegisterParticipant(participant)) =>
          ctx.log.info("Registering Participant with username: {}", participant.username)
//          replyTo ! RegistrationResponse(success = true, "Successfully Registered.")
          notStarted(registrations.+((participant.id, participant)), status, activeParticipants)

        case (ctx, GetRegistrations(replyTo)) =>
          ctx.log.info("Returning Total Registrations.")
          replyTo ! GetRegistrationsResponse(success = true, registrations.values.toList)
          Behaviors.same

        case (ctx, GetStatus(replyTo)) =>
          ctx.log.info("Returning Contest Status.")
          replyTo ! GetStatusResponse(status)
          Behaviors.same

        case (ctx, Start) =>
          ctx.log.info("Starting Contest.")
          active(registrations, Active, activeParticipants)
      }

  private def active(registrations: Map[UUID, Participant],
                     status: ContestStatus,
                     activeParticipants: Map[UUID, Participant]): Behavior[Command] =
    Behaviors
      .receive {
        case (ctx, GetStatus(replyTo)) =>
          ctx.log.info("Returning Contest Status.")
          replyTo ! GetStatusResponse(status)
          Behaviors.same

        case (ctx, Join(replyTo, participant)) if activeParticipants.contains(participant.id) =>
          ctx.log.info("Participant {} already joined contest.", participant.id)
          replyTo ! JoinContestResponse(success = false, "Participant is already in Contest.")
          Behaviors.same

        case (ctx, Join(replyTo, participant)) =>
          ctx.log.info("Participant {} Joining Contest.", participant.id)
          replyTo ! JoinContestResponse(success = true, "Successfully Joined")
          active(registrations, status, activeParticipants + ((participant.id, participant)))

        case (ctx, GetActiveParticipants(replyTo)) =>
          ctx.log.info("Returning Active Participants.")
          replyTo ! GetActiveParticipantsResponse(success = true, activeParticipants.values.toList)
          Behaviors.same

        case (ctx, Stop) =>
          ctx.log.info("Stopping Contest.")
          completed(registrations, Completed, activeParticipants)
      }

  private def completed(registrations: Map[UUID, Participant],
                        status: ContestStatus,
                        activeParticipants: Map[UUID, Participant]): Behavior[Command] =
    Behaviors
      .receive {
        case (ctx, GetStatus(replyTo)) =>
          ctx.log.info("Returning Contest Status.")
          replyTo ! GetStatusResponse(status)
          Behaviors.same

        case (ctx, _) =>
          ctx.log.info("Contest is over, can't serve this request anymore.")
          Behaviors.same
      }
}