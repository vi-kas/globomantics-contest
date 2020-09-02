package com.globomantics.services

import java.util.UUID
import java.util.concurrent.ForkJoinPool

import com.globomantics.persistence.Model.User
import com.typesafe.config.Config
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}

//import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

class UserServiceImpl(config: Config) extends UserService {

  private val logger: Logger = LoggerFactory.getLogger(getClass.getName)
  private var Users = Map[UUID, User]()

  implicit val threadPool: ForkJoinPool = new ForkJoinPool(2)
  implicit val ec: ExecutionContextExecutor = ExecutionContext.fromExecutorService(threadPool)

  override def all: Future[ServiceResponse[Seq[User]]] = Future {
    logger.info("[all]")

    Try(Users.values) match {
      case Failure(exception) => Left(ErrorResponse(exception.getMessage, 0))
      case Success(empty) if empty.isEmpty => Right(empty.toList)
      case Success(nonEmpty) => Right(nonEmpty.toList)
    }
  }

  override def byId(id: UUID): Future[ServiceResponse[User]] = Future {
    logger.info("[byId] - {}", id)

    Users.get(id) match {
      case None => Left(ErrorResponse(s"No User found with ID: $id", 0))
      case Some(user) => Right(user)
    }
  }

  override def create(user: User): Future[ServiceResponse[User]] = Future {

    Try(Users + (user.id -> user))
      .map(updated => Users = updated) match {
      case Failure(exception) => Left(ErrorResponse(exception.getMessage, 0))
      case Success(_) => Right(user)
    }
  }

  override def delete(id: UUID): Future[ServiceResponse[Boolean]] = Future {
    logger.info("[delete] - {}", id)

    Users.get(id) match {
      case None => Left(ErrorResponse(s"Couldn't find User with id: $id", 0))
      case Some(_) =>
        Try(Users.-(id))
          .map(updated => Users = updated) match {
          case Failure(exception) => Left(ErrorResponse(exception.getMessage, 0))
          case Success(_) => Right(true)
        }
    }
  }
}