#!/bin/bash
set -e
VERSION="$1"
kubectl config use-context prod-fss
sed "s/IMAGE_VERSION/$VERSION/g" nais/naiserator-prod.yml | kubectl -n default apply -f-
kubectl rollout status deployment/fasit -n default

# Check deployment rollout status every 10 seconds (max 10 minutes) until complete.
ATTEMPTS=0
ROLLOUT_STATUS_CMD=""
until $ROLLOUT_STATUS_CMD || [ $ATTEMPTS -eq 60 ]; do
  $ROLLOUT_STATUS_CMD
  ATTEMPTS=$((attempts + 1))
  sleep 10
done

