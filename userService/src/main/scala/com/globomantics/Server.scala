package com.globomantics

import akka.actor.ActorSystem
import akka.http.scaladsl.server.Route
import akka.stream.Materializer
import com.globomantics.persistence.UserDaoImpl
import com.globomantics.routes.UserRoutes
import com.globomantics.services.{UserServiceDBImpl, UserServiceImpl}
import com.softwaremill.macwire.wire
import com.typesafe.config.{Config, ConfigFactory}

object Server extends App {

  implicit val system: ActorSystem = ActorSystem()
  implicit val materializer: Materializer = Materializer(system)

  def config: Config = ConfigFactory.load()

  val userDaoImpl: UserDaoImpl = wire[UserDaoImpl]

//  val userService: UserServiceImpl = wire[UserServiceImpl]
  val userService: UserServiceDBImpl = wire[UserServiceDBImpl]

  val userRoutes: UserRoutes = wire[UserRoutes]
  val serviceRoutes: Route = userRoutes.routes

  HttpService.run(config, serviceRoutes)
}