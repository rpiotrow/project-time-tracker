package io.github.rpiotrow.ptt.e2e.utils

import io.github.rpiotrow.ptt.api.error.ApiError
import org.scalatest.matchers.should

private[e2e] trait ApiResults { self: should.Matchers =>

  protected implicit class ApiResultOps[A](result: Either[ApiError, A]) {
    def success(): A        = result.fold(apiError => fail(apiError.toString), identity)
    def failure(): ApiError = result.fold(identity, a => fail(a.toString))
  }

}
