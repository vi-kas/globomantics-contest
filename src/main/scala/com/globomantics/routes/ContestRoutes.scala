package com.globomantics.routes

import java.util.UUID

import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.{pathEndOrSingleSlash, pathPrefix, _}
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import com.globomantics.actors.ContestManager
import com.globomantics.actors.ContestManager.{GetContestResponse, GetContestsResponse}
import com.globomantics.persistance.Model._
import spray.json.JsString

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.language.postfixOps

class ContestRoutes(contest: ActorRef[ContestManager.Command])(implicit val system: ActorSystem[_]) {

  implicit val timeout: Timeout = 3 seconds
  implicit val ec: ExecutionContextExecutor = system.executionContext

  def getContests: Future[ApiResponse] =
    contest
      .ask(ContestManager.GetContests)
      .mapTo[GetContestsResponse]
      .map { response =>
        response.contests match {
          case Left(exception) => ApiResponse("failure", JsString(exception.getMessage))
          case Right(value) => ApiResponse("success", value.toJson)
        }
      }

  def getContest(id: UUID): Future[ApiResponse] =
    contest
      .ask(ContestManager.GetContest(id, _))
      .mapTo[GetContestResponse]
      .map { response =>
        response.contest match {
          case Left(exception) => ApiResponse("failure", JsString(exception.getMessage))
          case Right(value) => ApiResponse("success", value.toJson)
        }
      }

  val routes: Route =
    pathPrefix("contest"){
      concat(
        pathEndOrSingleSlash {
          concat(
            get(complete(getContests)),
            (post & entity(as[ContestCreation])){contestCreation =>
              val contFromRequest = contestCreation.contest
              contest ! ContestManager.CreateContest(contFromRequest.id, contFromRequest.title, contFromRequest.durationInMinutes)
              complete(StatusCodes.Accepted)
            }
          )
        },
        pathPrefix(JavaUUID){ id =>
          concat(
            pathEndOrSingleSlash {
              concat(
                get(complete(getContest(id))),
                delete {
                  contest ! ContestManager.RemoveContest(id)
                  complete(StatusCodes.Accepted)
                }
              )
            },
            (get & path("price"))(complete(StatusCodes.OK, "Price: DummyPriceValue"))
          )
        },
        (get & path("count"))(complete(StatusCodes.OK, "Count: DummyCountNumber"))
      )
    }
}