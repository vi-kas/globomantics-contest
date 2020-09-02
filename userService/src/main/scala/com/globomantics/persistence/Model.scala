package com.globomantics.persistence


import java.util.UUID

import akka.http.scaladsl.marshallers.sprayjson._
import com.globomantics.services._
import spray.json.{JsValue, _}

object Model extends SprayJsonSupport with DefaultJsonProtocol {

  /**
    * JSON formatter for models
    */
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

  implicit val userJsonFormat = jsonFormat5(User)

  implicit val apiResponseJsonFormat = jsonFormat4(ApiResponse)

  /**
    * Below are the models for user service
    */
  trait Entity { def id: UUID }

  case class User(id: UUID, username: String, password: String, name: String, email: String) extends Entity
}