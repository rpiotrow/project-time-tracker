package io.github.rpiotrow.projecttimetracker.api.param

import java.time.YearMonth

import io.github.rpiotrow.projecttimetracker.api.model.NonEmptyUserIdList

case class StatisticsParams(ids: NonEmptyUserIdList, from: YearMonth, to: YearMonth)
