package com.globomantics.persistence

import java.util.UUID

import com.globomantics.persistence.Model._

import scala.concurrent.{ExecutionContext, Future}

trait UserDao {

  def insert(entity: User)(implicit ec: ExecutionContext): Future[User]

  def byId(id: UUID)(implicit ec: ExecutionContext): Future[Option[User]]

  def byUsername(username: String)(implicit ec: ExecutionContext): Future[Option[User]]

  def all(implicit ec: ExecutionContext): Future[Seq[User]]

  def update(id: UUID, entity: User)(implicit ec: ExecutionContext): Future[Int]

  def remove(id: UUID)(implicit ec: ExecutionContext): Future[Boolean]
}