#!/bin/bash -x
set -e

echo "Create k8s cluster"
kind create cluster --name=ptt --config=kind-config.yaml

echo "Deploy and initialize PostgreSQL into the k8s cluster"
kubectl create configmap postgresql-init  --from-file ../local-dev/schema/
helm repo add bitnami https://charts.bitnami.com/bitnami
helm install postgresql --set initdbScriptsConfigMap=postgresql-init,postgresqlDatabase=ptt bitnami/postgresql

echo "Build docker images"
cd .. && sbt docker:publishLocal && cd k8s
docker image prune -f --filter label=snp-multi-stage=intermediate
image_tag=$(git rev-parse HEAD)-SNAPSHOT

# Workaround for docker from snap (see https://kind.sigs.k8s.io/docs/user/known-issues/#docker-installed-with-snap)
echo "Create temporary dictionary"
TMPDIR=$(mktemp -d -t ptt-docker-image-XXXXXXXXXX --tmpdir=$HOME)
export TMPDIR
trap "rm -f $TMPDIR/* && rmdir $TMPDIR" EXIT

echo "Load images into the k8s cluster"
kind --name=ptt load docker-image ptt-gateway:${image_tag}
kind --name=ptt load docker-image ptt-read-side:${image_tag}
kind --name=ptt load docker-image ptt-write-side:${image_tag}

echo "Deploy project time tracker into the k8s cluster"
helm install --set pttVersion=${image_tag} project-time-tracker helm-ptt-chart
