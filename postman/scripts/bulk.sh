#!/usr/bin/env bash
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

set -e
POSTMAN_DIR=../postman
APP=100
API=100

# Read args
for i in "$@"
do
  case $i in
    -p=*|--postman-dir=*)
    POSTMAN_DIR="${i#*=}"
    shift
    ;;
    -e=*|--api=*)
    API="${i#*=}"
    shift
    ;;
    -u=*|--app=*)
    APP="${i#*=}"
  esac
done

echo "--------------------- INFOS ---------------------"
echo "POSTMAN_DIR           = ${POSTMAN_DIR}"
echo "Generate ${APP} applications and ${API} apis"
echo "-------------------------------------------------"
echo ""

newman run $POSTMAN_DIR/bulk/gio_configuration.json -e $POSTMAN_DIR/env/Gravitee.io-Localhost-Environment.json --bail
newman run $POSTMAN_DIR/bulk/gio_applications.json -e $POSTMAN_DIR/env/Gravitee.io-Localhost-Environment.json -n $APP --bail
newman run $POSTMAN_DIR/bulk/gio_apis.json -e $POSTMAN_DIR/env/Gravitee.io-Localhost-Environment.json --folder create.category --bail
newman run $POSTMAN_DIR/bulk/gio_apis.json -e $POSTMAN_DIR/env/Gravitee.io-Localhost-Environment.json -d $POSTMAN_DIR/bulk/categories.json --folder create.api -n $API --bail
