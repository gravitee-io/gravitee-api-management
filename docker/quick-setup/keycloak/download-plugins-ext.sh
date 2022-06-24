#!/bin/bash
#
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
#


PLUGINS_URL="https://download.gravitee.io/graviteeio-apim/plugins/"
PLUGIN_PATH="resources/gravitee-resource-oauth2-provider-keycloak/"
PLUGIN_FILENAME="gravitee-resource-oauth2-provider-keycloak-${1:-1.9.2}.zip"

echo "Downlaod $PLUGIN_FILENAME"

wget -P .plugins "$PLUGINS_URL$PLUGIN_PATH$PLUGIN_FILENAME"
