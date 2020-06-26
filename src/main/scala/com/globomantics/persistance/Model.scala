package com.globomantics.persistance

import java.time.Instant
import java.util.UUID

import akka.http.scaladsl.marshallers.sprayjson._
import com.globomantics.services._
import spray.json._

object Model extends SprayJsonSupport with DefaultJsonProtocol {

  /*  JsonFormatter for Models  */

  implicit object userRoleJsonFormat extends EnumerationFormat[UserRole.Value](UserRole)

  implicit object uuidJsonFormat extends JsonFormat[UUID] {
    def write(uuid: UUID) = JsString(uuid.toString)

    def read(value: JsValue): UUID = {
      value match {
        case JsString(uuid) => UUID.fromString(uuid)
        case _ => throw new DeserializationException("Expected hexadecimal UUID string")
      }
    }
  }

  implicit val instantJsonFormat: JsonFormat[Instant] = new JsonFormat[Instant] {
    override def write(obj: Instant): JsValue = JsString(obj.toString)

    override def read(json: JsValue): Instant = json match {
      case JsString(value) => Instant.parse(value)
      case _ => throw new RuntimeException("can not parse")
    }
  }

  class EnumerationFormat[A](enum: Enumeration) extends RootJsonFormat[A] {
    def write(obj: A): JsValue = JsString(obj.toString)

    def read(json: JsValue): A = json match {
      case JsString(str) => enum.withName(str).asInstanceOf[A]
      case x => throw new RuntimeException(s"unknown enumeration value: $x")
    }
  }

  implicit val errorJsonFormat = jsonFormat2(ErrorResponse)
  implicit val apiResponseJsonFormat = jsonFormat4(ApiResponse)

  implicit val locationJsonFormat = jsonFormat3(Location)
  implicit val addressJsonFormat = jsonFormat2(Address)

  implicit val userJsonFormat = jsonFormat6(User)

  implicit val timeslotJsonFormat = jsonFormat2(Timeslot)
  implicit val talkJsonFormat = jsonFormat5(Talk)
  implicit val trackJsonFormat = jsonFormat3(Track)
  implicit val scheduleJsonFormat = jsonFormat4(Schedule)
  implicit val conferenceJsonFormat = jsonFormat3(Conference)

  implicit val participantJsonFormat = jsonFormat3(Participant)
  implicit val contastRegJsonFormat = jsonFormat2(ContestRegistration)


  object UserRole extends Enumeration {

    type UserRole = Value

    val Admin = Value("Admin")
    val Speaker = Value("Speaker")
    val Attendee = Value("Attendee")
    val Organizer = Value("Organizer")
  }

  case class Location(pin: String, city: Option[String], country: Option[String])
  case class Address(addressLine: Option[String], location: Location)
  case class Timeslot(from: Instant, to: Instant)

  trait Entity { def id: UUID }

  case class Talk(id: UUID, title: String, timeslot: Timeslot, trackFK: UUID, speaker: UUID) extends Entity

  case class Track(id: UUID, title: String, scheduleFK: UUID) extends Entity

  case class Schedule(id: UUID, time: Timeslot, venue: Address, conferenceFK: UUID) extends Entity

  case class Conference(id: UUID, title: String, price: Option[Double]) extends Entity

  import UserRole._

  case class User(id: UUID, name: String, email: String, password: String, address: Address, role: UserRole) extends Entity

  case class Contest(id: UUID, title: String, durationInMins: Double) extends Entity
  case class Participant(id: UUID, username: String, email: String) extends Entity

  sealed trait ContestHttpRequest
  case class ContestRegistration(contestId: UUID, participant: Participant) extends ContestHttpRequest
}