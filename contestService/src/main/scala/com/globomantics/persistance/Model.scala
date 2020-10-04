package com.globomantics.persistance

import java.util.UUID

import akka.http.scaladsl.marshallers.sprayjson._
import com.globomantics.services._
import spray.json.{JsValue, _}

object Model extends SprayJsonSupport with DefaultJsonProtocol {

  /*  JsonFormatter for Models  */
  implicit object uuidJsonFormat extends JsonFormat[UUID] {
    def write(uuid: UUID) = JsString(uuid.toString)

    def read(value: JsValue): UUID = {
      value match {
        case JsString(uuid) => UUID.fromString(uuid)
        case _ => throw new DeserializationException("Expected hexadecimal UUID string")
      }
    }
  }

  implicit val errorJsonFormat = jsonFormat2(ErrorResponse)

  implicit val participantJsonFormat = jsonFormat3(Participant)
  implicit val contestJsonFormat = jsonFormat3(Contest)
  implicit val contastRegJsonFormat = jsonFormat2(ContestRegistration)
  implicit val contastCreaJsonFormat = jsonFormat1(ContestCreation)
  implicit val apiResponseJsonFormat = jsonFormat2(ApiResponse)

  trait Entity { def id: UUID }

  case class Participant(id: UUID, username: String, email: String) extends Entity
  case class Contest(id: UUID, title: String, durationInMinutes: Double) extends Entity

  sealed trait ContestHttpRequest
  case class ContestCreation(contest: Contest) extends ContestHttpRequest
  case class ContestRegistration(contestId: UUID, participantId: UUID) extends ContestHttpRequest

  case class ApiResponse(message: String, data: JsValue = JsString(""))
}