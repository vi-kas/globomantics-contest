package com.globomantics.actors

import java.util.UUID

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}

object ContestActor {

  /**
    * ContestStatus to depict Status for Contest at any given time. It could be
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

  case class Register(participantId: UUID, replyTo: ActorRef[Response]) extends Command

  case class Start(replyTo: ActorRef[Response]) extends Command

  case class Stop(replyTo: ActorRef[Response]) extends Command

  /** Responses to send wherever required */
  sealed trait Response

  case class RegisterResponse(participantId: UUID, success: Boolean, description: String = "") extends Response

  case class ContestResponse(success: Boolean, status: ContestStatus) extends Response

  def apply(registrations: Set[UUID] = Set.empty, status: ContestStatus = NotStarted): Behavior[Command] =
    Behaviors.receive { (ctx, message) =>
      message match {

        case Register(participantId, replyTo) =>
          if(registrations.contains(participantId)){
            ctx.log.info("Participant with id: {} already registered.", participantId)
            replyTo ! RegisterResponse(participantId, success = false, s"Participant with id: $participantId already registered.")

            this(registrations, status)
          } else {
            ctx.log.info("Registering Participant with ID: {}", participantId)
            replyTo ! RegisterResponse(participantId, success = true)

            this(registrations + participantId, status)
          }

        case Start(replyTo) =>
          ctx.log.info("Starting Contest!")
          replyTo ! ContestResponse(success = true, Active)
          active(registrations, Active)

        case other =>
          ctx.log.info("Request received | Contest is NotStarted, the command {} is not supported.", other)
          Behaviors.same
      }
    }

  private def active(registrations: Set[UUID],
                     status: ContestStatus): Behavior[Command] =
    Behaviors
      .receive { (ctx, msg) =>
        msg match {

          case Stop(_) =>
            ctx.log.info("Stopping Contest.")
            completed(registrations, Completed)

          case other =>
            ctx.log.info("Request received: {} | Contest is active, try sending a Stop command.", other)
            Behaviors.same
        }

      }

  private def completed(registrations: Set[UUID],
                        status: ContestStatus): Behavior[Command] =
    Behaviors
      .receive { (ctx, _) =>
        ctx.log.info("Contest is over, can't serve this request anymore.")
        Behaviors.stopped
      }

}