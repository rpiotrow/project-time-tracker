# Read side (queries)

## Introduction

Service handling all read operations of the API. Full documentation of the API
is exposed by [gateway](../gateway/README.md) service, and the API is defined
using [tAPIr](https://tapir.softwaremill.com/) library in [api](../api/README.md) module.

## Operations

Detailed parameters of endpoints can be found in API documentation.

### List of projects

Any user can fetch list of projects filtered and with simple paging.

### Details of the project

Any user can fetch details of the project based on project identifier.

### Statistics

Any user can fetch statistics for given users and given time range.

## Implementation notes

### Data

Data is stored in PostgreSQL database. Hikari pool is configured as read-only, also database user has only read
permissions to the read model schema tables.

### Effects

This service use [ZIO](https://zio.dev/) with [zio-iterop-cats]() to be able
to use [doobie](https://tpolecat.github.io/doobie/), [quill](https://getquill.io/) and [http4s](https://http4s.org/).
