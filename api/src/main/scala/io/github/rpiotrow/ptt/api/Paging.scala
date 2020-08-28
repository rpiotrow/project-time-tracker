package io.github.rpiotrow.ptt.api

import cats.implicits._
import eu.timepit.refined.auto._
import io.github.rpiotrow.ptt.api.param._
import sttp.tapir._
import sttp.tapir.codec.refined._

private[api] object Paging {
  val defaultPageNumber: PageNumber = 0
  val defaultPageSize: PageSize     = 25

  val pageNumberInput: EndpointInput[PageNumber] =
    query[Option[PageNumber]]("pageNumber")
      .description(s"Page number ($defaultPageNumber if not provided)")
      .example(defaultPageNumber.some)
      .map(Mapping.from[Option[PageNumber], PageNumber] { pageNumberOption: Option[PageNumber] =>
        pageNumberOption.getOrElse(defaultPageNumber)
      } { pageNumber =>
        pageNumber.some
      })

  val pageSizeInput: EndpointInput[PageSize] =
    query[Option[PageSize]]("pageSize")
      .description(s"Page size ($defaultPageSize if not provided)")
      .example(defaultPageSize.some)
      .map(Mapping.from[Option[PageSize], PageSize] { pageSizeOption: Option[PageSize] =>
        pageSizeOption.getOrElse(defaultPageSize)
      } { pageSize =>
        pageSize.some
      })
}
