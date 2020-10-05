#!/bin/bash -x

echo "Create k8s cluster"
kind create cluster --name=ptt --config=kind-config.yaml

echo "Deploy and initialize PostgreSQL into the k8s cluster"
kubectl create configmap postgresql-init  --from-file ../local-dev/schema/
helm install postgresql --set initdbScriptsConfigMap=postgresql-init,postgresqlDatabase=ptt bitnami/postgresql

echo "Build and load images into the k8s cluster"
cd .. && sbt docker:publishLocal && cd k8s
docker image prune -f --filter label=snp-multi-stage=intermediate
image_tag=$(git rev-parse HEAD)-SNAPSHOT
kind --name=ptt load docker-image ptt-gateway:${image_tag}
kind --name=ptt load docker-image ptt-read-side:${image_tag}
kind --name=ptt load docker-image ptt-write-side:${image_tag}

echo "Deploy project time tracker into the k8s cluster"
helm install --set pttVersion=${image_tag} project-time-tracker helm-ptt-chart
