package com.globomantics.persistance

import java.util.UUID

import com.globomantics.persistance.Model.Contest

import scala.concurrent.{ExecutionContext, Future}

trait ContestDao {

  def insert(entity: Contest)(implicit ec: ExecutionContext): Future[Contest]

  def byId(id: UUID)(implicit ec: ExecutionContext): Future[Option[Contest]]

  def all(implicit ec: ExecutionContext): Future[Seq[Contest]]

  def remove(id: UUID)(implicit ec: ExecutionContext): Future[Boolean]
}