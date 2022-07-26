#!/bin/bash
# Copyright (C) 2015 The Gravitee team (http://gravitee.io)
# 
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
# 
#         http://www.apache.org/licenses/LICENSE-2.0
# 
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.


clean() {
#  For each docker-compose service stop & remove containers & volumes
  docker-compose -f ./docker/common/docker-compose-base.yml -f ./docker/common/docker-compose-mongo.yml -f ./docker/common/docker-compose-jdbc.yml -f ./docker/common/docker-compose-apis.yml -f ./docker/common/docker-compose-wiremock.yml -f ./docker/common/docker-compose-uis.yml -f ./docker/ui-tests/docker-compose-ui-tests.yml -f ./docker/api-tests/docker-compose-api-tests.yml --project-directory $PWD rm --force --stop -v 2>/dev/null
}

if [ -n "$1" ] && [ "$1" = "clean" ]; then
  clean
  exit 0
fi

if [ -n "$1" ] && [ -n "$2" ]; then
  clean
  mode="$1"
  databaseType="$2"

  jdbcDriver="postgresql-42.4.0.jar"
  jdbcDest="./docker/common/tmp/jdbc-driver/$jdbcDriver"

  if [ "$databaseType" = "jdbc" ] && [ ! -f "$jdbcDest" ]; then
    curl https://jdbc.postgresql.org/download/$jdbcDriver --create-dirs -o $jdbcDest
  fi

  if [ "$mode" = "only-apim" ]; then
    DB_PROVIDER=$databaseType docker-compose -f ./docker/common/docker-compose-base.yml -f ./docker/common/docker-compose-$databaseType.yml -f ./docker/common/docker-compose-apis.yml -f ./docker/common/docker-compose-wiremock.yml -f ./docker/common/docker-compose-uis.yml --project-directory $PWD up $3
  elif [ "$mode" = "api-test" ]; then
    DB_PROVIDER=$databaseType docker-compose -f ./docker/common/docker-compose-base.yml -f ./docker/common/docker-compose-$databaseType.yml -f ./docker/common/docker-compose-apis.yml -f ./docker/common/docker-compose-wiremock.yml -f ./docker/api-tests/docker-compose-api-tests.yml --project-directory $PWD up --abort-on-container-exit --exit-code-from jest-e2e
  elif [ "$mode" = "ui-test" ]; then
    DB_PROVIDER=$databaseType docker-compose -f ./docker/common/docker-compose-base.yml -f ./docker/common/docker-compose-$databaseType.yml -f ./docker/common/docker-compose-apis.yml -f ./docker/common/docker-compose-uis.yml -f ./docker/ui-tests/docker-compose-ui-tests.yml --project-directory $PWD up --no-build --abort-on-container-exit --exit-code-from cypress
  fi
else
  echo "Usage: $0 [clean|only-apim|api-test|ui-test] [mongo|jdbc]"
fi
