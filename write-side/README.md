# Write side (commands)

## Introduction

Service handling all write operations of the API. Full documentation of the API
is exposed by [gateway](../gateway/README.md) service, and the API is defined
using [tAPIr](https://tapir.softwaremill.com/) library in [api](../api/README.md) module.

## Operations

All operations require presence of `X-Authorization` header with value of the id of a user performing operations.
When using API via the gateway this header is set by based on JWT token.

### Create a new project

To create a new  project user needs to give only project identifier which can be arbitrary text. This value needs
to be unique in the system (including deleted projects). In other words: identifier once used cannot be applied
to create new project.

### Update project

Only owner of the project (user who created it) can change project identifier. Change must be done to value that
is not already used in the system for other project (including deleted ones) to ensure project identifier uniqueness. 

### Delete project

Only owner of the project (user who created it) can delete it. This is soft delete, although deleted project cannot
be undeleted. All the tasks that are registered for deleted project are also marked as deleted (with the same deletion
time as the deleted project).

Project can be deleted only once, and it cannot be undeleted. Subsequent invocation of delete will return client error.

### Register a new task in project

Every user can register a task for a project. Task specify amount of time spend on the project. User can register
multiple tasks for a single project, but time span cannot overlap for any pair of these tasks.

### Update task

Only owner of the task can update it. Updated time span cannot overlap with any other user task from this project.  
Update is done by deleting (soft delete) existing task and creating new one with updated values. This means that the
previous version of the task will be present in project tasks (as deleted).

### Delete task

Only owner of the task can delete it. This is soft delete and deleted task will be still visible in tasks list
of the project, although deleted task duration will not be counted to project duration and to statistics.

Task can be deleted only once, and it cannot be undeleted. Subsequent invocation of delete will return client error.

## Implementation notes

### Data

Data is stored in PostgreSQL database. Read model has a separate schema which is optimized for reads. Each operation
is done in a transaction. The transaction includes update of the read model to ensure data consistency.

Data redundancy is done by purpose to make it possible to switch into different implementation
(e.g. based on akka event sourcing with events stored in Cassandra database).

When update of the read model detects inconsistency it is reported as warn to logs, but transaction
is not aborted (unless there is some error or failure in operation on the write side).

### Effects

Repositories return values in `ConnectionIO` from Doobie, while services and routes use IO monad from cats.

Usage of ZIO was problematic, since using it together with `ConnectionIO` in services was quite cumbersome.
