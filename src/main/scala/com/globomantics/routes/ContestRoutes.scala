package com.globomantics.routes

import akka.actor.typed.{ActorRef, ActorSystem}
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.{pathEndOrSingleSlash, pathPrefix, _}
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import com.globomantics.actors.ContestManager
import com.globomantics.actors.ContestManager.RegisterParticipant
import com.globomantics.persistance.Model._

import scala.concurrent.duration._

class ContestRoutes(contest: ActorRef[ContestManager.Command])(implicit val system: ActorSystem[_]) {

  implicit val timeout: Timeout = 3 seconds
  implicit val ec = system.executionContext

  val routes: Route =
    pathPrefix("contest"){
      pathEndOrSingleSlash {
        (post & entity(as[ContestRegistration])) { registrationRequest =>
          contest ! RegisterParticipant(registrationRequest.contestId, registrationRequest.participant)
          complete(StatusCodes.OK)
        }
      }
    }
}