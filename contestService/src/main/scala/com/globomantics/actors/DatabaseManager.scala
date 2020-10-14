package com.globomantics.actors

import java.net.ConnectException

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior, PostStop, PreRestart}
import com.globomantics.persistance.ContestDao
import com.globomantics.persistance.Model.Contest

import scala.concurrent.Future
import scala.util.{Failure, Success}

object DatabaseManager {

  implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global

  trait Command
  case class Create(contest: Contest, replyTo: ActorRef[Result]) extends Command

  trait Result
  case class DBContestCreated(success : Boolean, contest: Contest) extends Result

  case class AdaptedResult(result: Result, replyTo: ActorRef[Result]) extends Command

  def apply(contestDao: () => ContestDao): Behavior[Command] =
    Behaviors.setup { _ =>
      val dao = contestDao()
      db(dao)
    }

  def db(contestDao: ContestDao): Behavior[Command] =
    Behaviors
      .receive[Command] {(ctx, msg) =>

      msg match {
        case Create(contest, replyTo) =>
          ctx.log.info("DatabaseManager creating contest: {}", contest)
          val dbResultFuture: Future[Contest] = contestDao.insert(contest)

          ctx.pipeToSelf(dbResultFuture) {
            case Success(_) => AdaptedResult(DBContestCreated(true, contest), replyTo)
            case Failure(e) =>
              e match {
                case excep: java.sql.SQLTransientConnectionException =>
                  ctx.log.info(s"DatabaseManager create contest failed: ${excep.getCause}")
                  throw new ConnectException("Could not connect to database")

                case other =>
                  ctx.log.info(s"DatabaseManager create contest failed: ${other}")
                  AdaptedResult(DBContestCreated(false, contest), replyTo)
              }
          }
          Behaviors.same

        case AdaptedResult(res, replyTo) =>
          ctx.log.info("DatabaseManager WrappedResult: {}", res)
          replyTo ! res
          Behaviors.same
      }

    }.receiveSignal{
      case (ctx, PostStop) =>
        ctx.log.info("Stopping DatabaseManager")
        Behaviors.same

      case (ctx, PreRestart) =>
        ctx.log.info("Restarting DatabaseManager")
        Behaviors.same
    }

}
