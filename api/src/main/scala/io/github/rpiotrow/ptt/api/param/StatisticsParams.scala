package io.github.rpiotrow.ptt.api.param

import java.time.YearMonth

import io.github.rpiotrow.ptt.api.model.NonEmptyUserIdList

case class StatisticsParams(ids: NonEmptyUserIdList, from: YearMonth, to: YearMonth)
