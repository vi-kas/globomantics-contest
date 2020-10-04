package com.globomantics

import akka.actor
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import com.globomantics.actors.ContestManager
import com.globomantics.routes.ContestRoutes

import scala.util.{Failure, Success}

object BackendService {

  /**
    * @return Behavior of Backend Service, which in this case is to perform http server binding and start HTTP server.
    */
  def apply[T](): Behavior[T] =
    Behaviors.setup { context =>
      val system = context.system

      implicit val classicActorSystem: actor.ActorSystem = system.classicSystem
      import system.executionContext

      val contestManager: ActorRef[ContestManager.Command] = context.spawn(ContestManager(), "contest-manager")
      val contestRoutes: ContestRoutes = new ContestRoutes(contestManager)(system)

      val allRoutes: Route = contestRoutes.routes

      val port = 8081
      val host = "localhost"

      val futureBinding = Http().bindAndHandle(allRoutes, host, port)
      context.log.info("Starting Backend Service at http://{}:{}", host, port)

      futureBinding
        .onComplete {
          case Success(binding) =>
            system.log.info("Server started at http://{}:{}", binding.localAddress.getHostString, binding.localAddress.getPort)
          case Failure(exception) =>
            system.log.error("Failed to bind HTTP endpoint, terminating.", exception)
            system.terminate()
        }

      Behaviors.empty
    }

  /**
    * Entry-point to BackendService.
    * @param args => Arguments to application.
    */
  def main(args: Array[String]): Unit = {

    ActorSystem(BackendService(), "g-backend-service")

  }

}