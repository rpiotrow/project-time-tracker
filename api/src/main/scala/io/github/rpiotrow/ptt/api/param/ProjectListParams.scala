package io.github.rpiotrow.ptt.api.param

import java.time.LocalDateTime

import io.github.rpiotrow.ptt.api.model.ProjectId

case class ProjectListParams(
  ids: List[ProjectId],
  from: Option[LocalDateTime],
  to: Option[LocalDateTime],
  deleted: Option[Boolean],
  orderBy: Option[ProjectOrderingField],
  orderDirection: Option[OrderingDirection],
  pageNumber: PageNumber,
  pageSize: PageSize
)
