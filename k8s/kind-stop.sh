#!/bin/bash -x

echo "Delete k8s cluster"
kind delete cluster --name=ptt
