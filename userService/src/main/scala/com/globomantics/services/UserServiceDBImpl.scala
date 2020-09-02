package com.globomantics.services

import java.lang.Exception
import java.util.UUID

import com.globomantics.persistence.Model.User
import com.globomantics.persistence.UserDao
import com.globomantics.util.Util
import com.typesafe.config.Config
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class UserServiceDBImpl(config: Config, userDao: UserDao) extends UserService {

  private val logger: Logger = LoggerFactory.getLogger(getClass.getName)

  override def all: Future[ServiceResponse[Seq[User]]] = {
    logger.info("[all]")

    userDao
      .all
      .map(users => Right(users))
      .recover {
        case e: Exception => Left(ErrorResponse(e.getMessage, 0))
      }
  }

  override def create(user: User): Future[ServiceResponse[User]] = {
    logger.info("[create] - {}", user.id)

    //val createdUser: Future[User] = userDao.insert(user)

    encryptPassword(user)
      .zip(isUsernameUnique(user))
      .flatMap { encUserAndIsUnique: (User, Boolean) =>

        val (pwdEncryptedUser, isUnameUnique) = encUserAndIsUnique
        if(isUnameUnique)
          userDao.insert(pwdEncryptedUser)
        else throw new IllegalArgumentException(s"Username ${pwdEncryptedUser.username} already exists.")
      }
      .map { created =>
        Right(created)
      }
      .recover {
        case e: Exception => Left(ErrorResponse(e.getMessage, 0))
      }

//    encryptPassword(user)
//      .zip(isUsernameUnique(user))
//      .flatMap { encUserAndIsUnique: (User, Boolean) =>
//
//        val (pwdEncryptedUser, isUnameUnique) = encUserAndIsUnique
//        if(isUnameUnique)
//          userDao.insert(pwdEncryptedUser)
//        else throw new IllegalArgumentException(s"Username ${pwdEncryptedUser.username} already exists.")
//      }
//      .map { created =>
//        Right(created)
//      }

    //    encryptPassword(user)
    //      .flatMap { pwdEncryptedUser =>
    //        userDao.insert(pwdEncryptedUser)
    //      }
    //      .map { created =>
    //        Right(created)
    //      }

    //    createdUser
    //      .foreach { user =>
    //        logger.info("[create] - User Created with ID {}.", user.id)
    //      }
    //
    //    createdUser
    //      .foreach { user =>
    //        logger.info("[create] - Sending an email for successful reg to {}.", user.email)
    //        //sendEmail
    //      }

    //    createdUser
    //      .andThen {
    //        case scala.util.Success(user) => logger.info("[create] - User Created with ID {}.", user.id)
    //      }
    //      .andThen {
    //        case scala.util.Success(user) => logger.info("[create] - Sending an email for successful reg to {}.", user.email)
    //        //sendEmail
    //      }

    //    createdUser
    //      .map{user =>
    //        Right(user)
    //      }
  }

  def encryptPassword(user: User): Future[User] =
    Util
      .encrypt(user.password)
      .map(encrypted => user.copy(password = encrypted))

  def isUsernameUnique(user: User): Future[Boolean] =
    userDao
      .byUsername(user.username)
      .map(_.isEmpty)

  //  override def create(user: User): Future[ServiceResponse[User]] = {
  //    logger.info("[create] - {}", user.id)
  //
  //    userDao
  //      .insert(user)
  //      .map(Right(_))
  //      .recover {
  //        case e: Exception =>
  //          logger.info("[create] - exception occurred: {}", e.getMessage)
  //          e.printStackTrace()
  //          Left(ErrorResponse(e.getMessage, 0))
  //      }
  //  }

  override def byId(id: UUID): Future[ServiceResponse[User]] = {
    logger.info("[byId] - {}", id)

    userDao
      .byId(id)
      .map {
        case None => Left(ErrorResponse(s"Could not read User with Id: $id", 0))
        case Some(user) => Right(user)
      }
      .recover {
        case e: Exception =>
          logger.info("[create] - exception occurred: {}", e.getMessage)
          Left(ErrorResponse(e.getMessage, 0))
      }
  }

  override def delete(id: UUID): Future[ServiceResponse[Boolean]] = {
    logger.info("[delete] - {}", id)

    userDao
      .remove(id)
      .map {
        case true => Right(true)
        case false => Left(ErrorResponse("Could not delete User with Given ID", 0))
      }
      .recover {
        case e: Exception =>
          logger.info("[create] - exception occurred: {}", e.getMessage)
          Left(ErrorResponse(e.getMessage, 0))
      }
  }
}