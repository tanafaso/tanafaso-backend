#!/bin/bash

# Exit script when a command fails
set -e

./mvnw package -Dmaven.test.skip=true

docker build --platform linux/amd64 -t tanafaso .

docker tag tanafaso europe-west1-docker.pkg.dev/tanafaso/tanafaso/tanafaso

docker push europe-west1-docker.pkg.dev/tanafaso/tanafaso/tanafaso:latest

