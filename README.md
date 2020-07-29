# Project Time Tracker

API for tracking time of projects. The time is tracked in tasks. Statistics are gathered per user.

Data are stored in PostgreSQL database. Read operations are separated from writes (CQRS pattern).
There are separate sub-projects and separate schemas in SQL databases.

## Sub-projects

 * [api](api/README.md) - REST API defined with tAPIr
 * [gateway](gateway/README.md) - gateway exposing API with authorization and Open API documentation with Swagger
 * [read-side](read-side/README.md) - implementation of the query (read) side of the API
 * [write-side](write-side/README.md) - implementation of command (write) side of the API
 * [e2e-tests](e2e-tests/README.md) - end to end tests

## Running tests

To run all possible tests in all sub-projects invoke (from the main directory):
```
$ sbt checks
```
To run only unit tests:
```
$ sbt test
```
To run only integration tests (much slower than unit tests, using testcontainers):
```
$ sbt it:test
```

## Run

### Local database:
```
$ docker-compose -f local-dev/docker-compose.yml up -d
```

### Applications

TBD

## Libraries used

 * [http4s](https://http4s.org/) as http server in API implementation
 * [akka-http](https://doc.akka.io/docs/akka-http/current/index.html) as http server in gateway
 * [circe](https://circe.github.io/circe/) for JSON serialization
 * [ZIO](https://zio.dev/) as functional library
 * [cats](https://typelevel.org/cats/) as functional library
 * [tapir](https://tapir.softwaremill.com/) to describe endpoint (API)
 * [doobie](https://tpolecat.github.io/doobie/) to access SQL database
 * [quill](https://getquill.io/) to write SQL queries in Scala
 * [zio-config](https://zio.github.io/zio-config/) to parse configuration file into case class
 * [logback](http://logback.qos.ch/) for logging
 * [testcontainers](https://github.com/testcontainers/testcontainers-scala) to run database in a container in integration tests
 * [scalatest](https://www.scalatest.org/) as test framework
 * [enumeratum](https://github.com/lloydmeta/enumeratum) for enums
 * [refined](https://github.com/fthomas/refined) for type constraints in API
 * [diffx](https://github.com/softwaremill/diffx) for pretty diffs for case classes in tests
 * [scalamock](https://scalamock.org/) as mock library
