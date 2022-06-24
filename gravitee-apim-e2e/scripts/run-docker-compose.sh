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

if [ -n "$1" ] && [ $1 == "clean" ]; then
  docker-compose -f ./docker/common/docker-compose-apis.yml -f ./docker/common/docker-compose-wiremock.yml -f ./docker/common/docker-compose-uis.yml --project-directory $PWD stop || true
  docker-compose -f ./docker/common/docker-compose-apis.yml -f ./docker/common/docker-compose-wiremock.yml -f ./docker/common/docker-compose-uis.yml --project-directory $PWD rm || true
  docker volume rm gravitee-apim-e2e_data-mongo || true
  docker volume rm gravitee-apim-e2e_data-jdbc || true
  docker volume rm gravitee-apim-e2e_data-elasticsearch || true
elif [ -n "$1" ] && [ $1 == "only-apim" ] && [ -n "$2" ] && [ $2 == "mongo" ]; then
  docker-compose -f ./docker/common/docker-compose-base.yml -f ./docker/common/docker-compose-mongo.yml -f ./docker/common/docker-compose-apis.yml -f ./docker/common/docker-compose-wiremock.yml -f ./docker/common/docker-compose-uis.yml --project-directory $PWD up
elif [ -n "$1" ] && [ $1 == "only-apim" ] && [ -n "$2" ] && [ $2 == "jdbc" ]; then
  docker-compose -f ./docker/common/docker-compose-base.yml -f ./docker/common/docker-compose-jdbc.yml -f ./docker/common/docker-compose-apis.yml -f ./docker/common/docker-compose-wiremock.yml -f ./docker/common/docker-compose-uis.yml --project-directory $PWD up
elif [ -n "$1" ] && [ $1 == "api-test" ] && [ -n "$2" ] && [ $2 == "mongo" ]; then
  docker-compose -f ./docker/common/docker-compose-base.yml -f ./docker/common/docker-compose-mongo.yml -f ./docker/common/docker-compose-apis.yml -f ./docker/common/docker-compose-wiremock.yml -f ./docker/api-tests/docker-compose-api-tests.yml --project-directory $PWD up --abort-on-container-exit --exit-code-from jest-e2e
elif [ -n "$1" ] && [ $1 == "api-test" ] && [ -n "$2" ] && [ $2 == "jdbc" ]; then
  docker-compose -f ./docker/common/docker-compose-base.yml -f ./docker/common/docker-compose-jdbc.yml -f ./docker/common/docker-compose-apis.yml -f ./docker/common/docker-compose-wiremock.yml -f ./docker/api-tests/docker-compose-api-tests.yml --project-directory $PWD up --abort-on-container-exit --exit-code-from jest-e2e
elif [ -n "$1" ] && [ $1 == "ui-test" ] && [ -n "$2" ] && [ $2 == "mongo" ]; then
  docker-compose -f ./docker/common/docker-compose-base.yml -f ./docker/common/docker-compose-mongo.yml -f ./docker/common/docker-compose-apis.yml -f ./docker/common/docker-compose-uis.yml -f ./docker/ui-tests/docker-compose-ui-tests.yml --project-directory $PWD up --no-build --abort-on-container-exit --exit-code-from cypress
elif [ -n "$1" ] && [ $1 == "ui-test" ] && [ -n "$2" ] && [ $2 == "jdbc" ]; then
  docker-compose -f ./docker/common/docker-compose-base.yml -f ./docker/common/docker-compose-jdbc.yml -f ./docker/common/docker-compose-apis.yml -f ./docker/common/docker-compose-wiremock.yml -f ./docker/common/docker-compose-uis.yml -f ./docker/ui-tests/docker-compose-ui-tests.yml --project-directory --project-directory $PWD up --no-build --abort-on-container-exit --exit-code-from cypress
else
  echo "Usage: $0 [clean|only-apim|api-test|ui-test] [mongo|jdbc]"
fi
