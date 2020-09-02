package com.globomantics.services


import java.util.UUID

import com.globomantics.persistence.Model.User

import scala.concurrent.Future

trait UserService {

  def all: Future[ServiceResponse[Seq[User]]]

  def byId(id: UUID): Future[ServiceResponse[User]]

  def create(user: User): Future[ServiceResponse[User]]

  def delete(id: UUID): Future[ServiceResponse[Boolean]]
}