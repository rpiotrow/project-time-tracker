# Deployment to local Kubernetes

## Prerequisites

 * [kind](https://kind.sigs.k8s.io/)
 * [helm](https://helm.sh/)
 * [kubectl](https://kubernetes.io/docs/tasks/tools/install-kubectl/)

## Run

To create local Kubernetes cluster with deployed project invoke `kind-start.sh` script.
It will start cluster, deploy Postgres helm chart, build and deploy ptt using helm chart.
Cluster is exposing gateway on port 30036. To access Swagger use below URL:
```
http://localhost:30036/docs
```

## End to end tests

To run [end to end tests](../e2e-tests/README.md) using ptt in cluster change `base-uri` in `application.conf`
of e2e-tests to:
```
http://localhost:30036
```

## Destroy cluster

Use `kind-stop.sh` script to stop and destroy cluster.
