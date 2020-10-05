# End to end tests

## Description

End to end tests require running application (check [run locally](../README.md#run)
or [local Kubernetes cluster](../k8s/README.md)).

Base uri and key to create valid JWT tokens are taken from configuration file.
Defaults are for local Kubernetes cluster.

To run all end to end tests invoke below sbt command (in the root directory of the project):
```
$ sbt e2e:test
```

## Implementation notes

API is invoked using [sttp](https://sttp.softwaremill.com).
