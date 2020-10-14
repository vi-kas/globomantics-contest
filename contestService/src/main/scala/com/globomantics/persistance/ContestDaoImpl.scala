package com.globomantics.persistance


import java.util.UUID

import com.globomantics.persistance.Model.Contest
import slick.jdbc.JdbcProfile

import scala.concurrent.{ExecutionContext, Future}

class ContestDaoImpl extends ContestDao {

  val driver: JdbcProfile = slick.jdbc.PostgresProfile

  val db: driver.backend.DatabaseDef = driver.api.Database.forConfig("slick.contestdb")

  import driver.api._

  val table = TableQuery[ContestTable]

  def initSchema()(implicit ec: ExecutionContext): Future[Unit] =
    dropSchema()
      .map(_ => createSchema())

  def dropSchema()(implicit ec: ExecutionContext): Future[Unit] =
    db.run {
      table.schema.dropIfExists
    }

  def createSchema()(implicit ec: ExecutionContext): Future[Unit] =
    db.run {
      table.schema.create
    }

  override def insert(contest: Contest)(implicit ec: ExecutionContext): Future[Contest] =
    db.run {
      table returning table += contest
    }

  override def all(implicit ec: ExecutionContext): Future[Seq[Contest]] =
    db.run {
      table.to[Seq].result
    }

  override def byId(id: UUID)(implicit ec: ExecutionContext): Future[Option[Contest]] =
    db.run {
      table.filter(_.id === id).result.headOption
    }

  override def remove(id: UUID)(implicit ec: ExecutionContext): Future[Boolean] =
    db.run {
      table.filter(_.id === id).delete.map(_ > 0)
    }

  class ContestTable(tag: Tag) extends Table[Contest](tag, "contests_t"){

    val id = column[UUID]("id", O.PrimaryKey)

    val title = column[String]("title")
    val durationInMinutes = column[Double]("durationinminutes")

    def * =
      (id, title, durationInMinutes) <> (Contest.tupled, Contest.unapply)
  }

}
