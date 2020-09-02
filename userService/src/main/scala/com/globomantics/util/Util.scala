package com.globomantics.util

import scala.concurrent.{ExecutionContext, Future}

object Util {
  import java.util.Base64

  def encrypt(pwd: String)(implicit ec: ExecutionContext): Future[String] =
    Future {
      Base64.getEncoder.encodeToString(pwd.getBytes)
    }

}
