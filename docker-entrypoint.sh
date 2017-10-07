#!/bin/bash

set -ex
export JAVA_OPTS="-Xms50m -Xmx100m"

if [ "$1" = "start" ]
then
  exec lein run
fi

exec "$@"
