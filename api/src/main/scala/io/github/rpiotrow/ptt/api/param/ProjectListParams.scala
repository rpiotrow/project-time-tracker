package io.github.rpiotrow.ptt.api.param

import java.time.{OffsetDateTime}

import io.github.rpiotrow.ptt.api.model.ProjectId

case class ProjectListParams(
  ids: List[ProjectId],
  from: Option[OffsetDateTime],
  to: Option[OffsetDateTime],
  deleted: Option[Boolean],
  orderBy: Option[ProjectOrderingField],
  orderDirection: Option[OrderingDirection],
  pageNumber: PageNumber,
  pageSize: PageSize
)
