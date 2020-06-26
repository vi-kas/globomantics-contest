package com.globomantics.actors

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}

object ParticipantActor {

  sealed trait Command
  case class Run(replyTo: ActorRef[RunResponse], problemId: Int, solution: String)

  case class Progress(scoreInPercentage: Double = 0)

  sealed trait RunResponse
  case class SuccessResponse(scoreInPercentage: Double = 0) extends RunResponse
  case class FailureResponse(message: String, exception: Throwable) extends RunResponse

  def apply(progress: Progress = Progress()): Behavior[Command] =
    Behaviors
      .unhandled
}