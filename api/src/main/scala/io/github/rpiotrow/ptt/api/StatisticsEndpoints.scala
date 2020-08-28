package io.github.rpiotrow.ptt.api

import java.time.YearMonth

import io.circe.generic.auto._
import io.github.rpiotrow.ptt.api.model._
import io.github.rpiotrow.ptt.api.output._
import io.github.rpiotrow.ptt.api.param._
import sttp.tapir._
import sttp.tapir.integ.cats.codec._
import sttp.tapir.json.circe._

object StatisticsEndpoints {

  import Base._
  import CustomCodecs._

  private val statisticsInput: EndpointInput[StatisticsParams] =
    query[NonEmptyUserIdList]("ids")
      .description("Return statistics for given users")
      .example(NonEmptyUserIdList.example)
      .and(
        query[YearMonth]("from")
          .description("Return statistics starting from given year and month")
          .example(YearMonth.of(2020, 1))
      )
      .and(
        query[YearMonth]("to")
          .description("Return statistics until given year and month")
          .example(YearMonth.of(2020, 9))
      )
      .mapTo(StatisticsParams)
      .validate(Validator.custom(input => !input.to.isBefore(input.from), "`from` before or equal `to`"))

  val statisticsEndpoint = baseEndpoint
    .in("statistics")
    .get
    .in(statisticsInput)
    .out(jsonBody[StatisticsOutput].example(StatisticsOutput.example))

}
