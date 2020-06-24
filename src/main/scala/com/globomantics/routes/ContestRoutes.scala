package com.globomantics.routes

import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.{pathEndOrSingleSlash, pathPrefix, _}
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import com.globomantics.Contest
import com.globomantics.Contest.{Command, GetRegistrationsResponse, RegisterParticipant}
import com.globomantics.persistance.Model._

import scala.concurrent.Future
import scala.concurrent.duration._

class ContestRoutes(contest: ActorRef[Command])(implicit val system: ActorSystem[_]) {

  implicit val timeout: Timeout = 3 seconds
  implicit val ec = system.executionContext

  def getRegistrations: Future[List[Participant]] =
    contest
      .ask(Contest.GetRegistrations)
      .mapTo[GetRegistrationsResponse]
      .map(_.registrations)

  val routes: Route =
    pathPrefix("api" / "v1" / "contest" / "register"){

      pathEndOrSingleSlash {
        get {
          complete(getRegistrations)
        } ~
          (post & entity(as[Participant])) { participant =>
            contest ! RegisterParticipant(participant)
            complete(StatusCodes.OK)
          }
      }
    }
}