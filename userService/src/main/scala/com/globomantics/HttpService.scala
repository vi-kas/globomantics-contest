package com.globomantics

import akka.actor.ActorSystem
import akka.event.{Logging, LoggingAdapter}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import akka.stream.Materializer
import com.typesafe.config.Config

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.{Failure, Success}

class HttpService private (config: Config, routes: Route)
                          (implicit system: ActorSystem, materializer: Materializer) {

  implicit def executor: ExecutionContextExecutor = system.dispatcher
  val logger: LoggingAdapter = Logging(system, getClass)

  val bindingFuture: Future[Http.ServerBinding] =
    Http()
      .bindAndHandle(
        routes,
        config.getString("http.interface"),
        config.getInt("http.port")
      )

  bindingFuture.onComplete{
    case Success(bound) =>
      logger.info(s"Server Started: ${bound.localAddress.getHostString}")
    case Failure(e) =>
      logger.error(s"Server could not start: ${e.getMessage}")
      system.terminate()
  }
}

object HttpService {

  def run(config: Config, serviceRoutes: Route)(implicit system: ActorSystem, mat: Materializer) =
    new HttpService(config, serviceRoutes)
}