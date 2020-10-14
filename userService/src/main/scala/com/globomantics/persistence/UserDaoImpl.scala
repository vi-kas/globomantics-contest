package com.globomantics.persistence

import java.util.UUID

import com.globomantics.persistence.Model.User
import slick.jdbc.JdbcProfile

import scala.concurrent.ExecutionContext

//import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class UserDaoImpl extends UserDao {

  val driver: JdbcProfile = slick.jdbc.PostgresProfile

  val db: driver.backend.DatabaseDef =
    driver.api.Database.forConfig("slick.userdb")

  import driver.api._

  val table = TableQuery[UserTable]

  def initSchema()(implicit ec: ExecutionContext): Future[Unit] =
    dropSchema()
      .map(_ => createSchema())

  def dropSchema()(implicit ec: ExecutionContext): Future[Unit] = db.run {
    table.schema.dropIfExists
  }

  def createSchema()(implicit ec: ExecutionContext): Future[Unit] = db.run {
    table.schema.create
  }

  override def insert(user: User)(implicit ec: ExecutionContext): Future[User] = db.run {
    table returning table += user
  }

  override def all(implicit ec: ExecutionContext): Future[Seq[User]] = db.run {
    table.to[Seq].result
  }

  override def byId(id: UUID)(implicit ec: ExecutionContext): Future[Option[User]] = db.run {
    table.filter(_.id === id).result.headOption
  }

  override def byUsername(username: String)(implicit ec: ExecutionContext): Future[Option[User]] = db.run {
    table.filter(_.username === username).result.headOption
  }

  override def update(id: UUID, user: User)(implicit ec: ExecutionContext): Future[Int] = db.run {
    table insertOrUpdate user
  }

  override def remove(id: UUID)(implicit ec: ExecutionContext): Future[Boolean] = db.run {
    table.filter(_.id === id).delete.map(_ > 0)
  }

  def byEmail(email: String)(implicit ec: ExecutionContext): Future[Option[User]] = db.run {
    table.filter(_.email === email).result.headOption
  }

  class UserTable(tag: Tag) extends Table[User](tag, "users_t"){

    val id = column[UUID]("id", O.PrimaryKey)

    val username = column[String]("username")
    val password = column[String]("password")
    val name = column[String]("name")
    val email = column[String]("email")

    def * =
      (id, username, password, name, email) <> (User.tupled, User.unapply)
  }
}