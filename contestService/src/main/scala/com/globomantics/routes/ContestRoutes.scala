package com.globomantics.routes

import java.util.UUID

import akka.actor.typed.{ActorRef, ActorSystem}
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.{pathEndOrSingleSlash, pathPrefix, _}
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import com.globomantics.actors.ContestManager
import com.globomantics.actors.ContestManager.GetContestResponse
import com.globomantics.persistance.Model._
import spray.json.JsString

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.language.postfixOps

class ContestRoutes(contest: ActorRef[ContestManager.Command])(implicit val system: ActorSystem[_]) {

  implicit val timeout: Timeout = 3 seconds
  implicit val ec: ExecutionContextExecutor = system.executionContext

  import akka.actor.typed.scaladsl.AskPattern._

  def getContest(id: UUID): Future[ApiResponse] = {

    val contestInfoFuture: Future[ContestManager.Response] =
      contest.ask(ContestManager.GetContest(id, _: ActorRef[ContestManager.Response]))

    contestInfoFuture
      .mapTo[GetContestResponse]
      .map { response =>
        response.contest match {
          case Left(exception) => ApiResponse("failure", JsString(exception.getMessage))
          case Right(value) => ApiResponse("success", value.toJson)
        }
      }
  }

  val routes: Route =
    concat(
      pathPrefix("contest"){
        concat(
          pathEndOrSingleSlash {
            concat(
              (post & entity(as[ContestCreation])){contestCreation =>

                val contestInfo = contestCreation.contest
                val createContestRequest =
                  ContestManager.CreateContest(contestInfo.id, contestInfo.title, contestInfo.durationInMinutes)

                contest.tell(createContestRequest)
                //contest ! createContestRequest
                complete(StatusCodes.Accepted, s"Request has been accepted for Contest Creation.")
              }

            )
          },
          pathPrefix(JavaUUID){ id =>
            concat(
              pathEndOrSingleSlash {
                concat(
                  get(complete(getContest(id)))
                )
              },
              (get & path("start")){
                contest ! ContestManager.StartContest(id)

                complete(StatusCodes.Accepted, s"Request to start contest with Id: $id received.")
              },
              (get & path("stop")){
                contest ! ContestManager.StopContest(id)

                complete(StatusCodes.Accepted, s"Request to stop contest with Id: $id received.")
              }
            )
          }
        )
      },
      pathPrefix("register"){
        pathEndOrSingleSlash {
          concat(
            (post & entity(as[ContestRegistration])){cReg =>

              contest ! ContestManager.RegisterParticipant(cReg.contestId, cReg.participantId)

              complete(StatusCodes.Accepted, s"Request for Registration: $cReg received.")
            }
          )
        }
      }
    )
}






















//val routes: Route =
//concat(
//pathPrefix("contest"){
//concat(
//pathEndOrSingleSlash {
//concat(
//(post & entity(as[ContestCreation])){contestCreation =>
//
//val contestInfo = contestCreation.contest
//val createContestRequest =
//ContestManager.CreateContest(contestInfo.id, contestInfo.title, contestInfo.durationInMinutes)
//
//contest.tell(createContestRequest)
////contest ! createContestRequest
//complete(StatusCodes.Accepted, s"Request has been accepted for Contest Creation.")
//}
//
//)
//},
//pathPrefix(JavaUUID){ id =>
//concat(
//pathEndOrSingleSlash {
//concat(
//get(complete(getContest(id)))
//)
//},
//(get & path("start")){
//contest ! ContestManager.StartContest(id)
//
//complete(StatusCodes.Accepted, s"Request to start contest with Id: $id received.")
//},
//(get & path("stop")){
//complete(StatusCodes.Accepted, s"Request to stop contest with Id: $id received.")
//}
//)
//}
//)
//},
//pathPrefix("register"){
//pathEndOrSingleSlash {
//concat(
//(post & entity(as[ContestRegistration])){cReg =>
//
//contest ! ContestManager.RegisterParticipant(cReg.contestId, cReg.participantId)
//
//complete(StatusCodes.Accepted, s"Request for Registration: $cReg received.")
//}
//)
//}
//}
//)