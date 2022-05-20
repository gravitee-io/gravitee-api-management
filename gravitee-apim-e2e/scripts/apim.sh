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
  docker-compose -f ./docker/common/docker-compose-apis.yml -f ./docker/common/docker-compose-wiremock.yml -f ./docker/common/docker-compose-uis.yml --project-directory $PWD stop
  docker-compose -f ./docker/common/docker-compose-apis.yml -f ./docker/common/docker-compose-wiremock.yml -f ./docker/common/docker-compose-uis.yml --project-directory $PWD rm
  docker volume rm gravitee-apim-e2e_data-mongo
  docker volume rm gravitee-apim-e2e_data-elasticsearch
else
  docker-compose -f ./docker/common/docker-compose-apis.yml -f ./docker/common/docker-compose-wiremock.yml -f ./docker/common/docker-compose-uis.yml --project-directory $PWD up
fi



