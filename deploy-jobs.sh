#!/bin/bash

# Note: Make sure that the desired jobs are marked true in application.yaml

# Exit script when a command fails
set -e

./mvnw package -Dmaven.test.skip=true

docker build --platform linux/amd64 -t tanafaso .

random_tag=$(printf "randomtag%08d" $((RANDOM%100000000)))

docker tag tanafaso europe-west1-docker.pkg.dev/tanafaso/tanafaso-jobs/tanafaso-jobs:$random_tag

docker push europe-west1-docker.pkg.dev/tanafaso/tanafaso-jobs/tanafaso-jobs:$random_tag

sed -i '' "s|image: europe-west1-docker.pkg.dev/tanafaso/tanafaso-jobs/tanafaso-jobs:.*|image: europe-west1-docker.pkg.dev/tanafaso/tanafaso-jobs/tanafaso-jobs:$random_tag|" tanafaso-cloud-run-jobs.yaml

gcloud run jobs replace tanafaso-cloud-run-jobs.yaml

echo "Deployed with tag: $random_tag"

