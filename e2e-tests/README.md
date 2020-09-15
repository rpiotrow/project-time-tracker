# End to end tests

## Description

End to end tests require running application (check [run locally](../README.md#run)).

Base uri and key to create valid JWT tokens are taken from configuration file.

To run all end to end tests invoke below sbt command:
```
$ sbt e2e:test
```

## Implementation notes

API is invoked using [sttp](https://sttp.softwaremill.com).
