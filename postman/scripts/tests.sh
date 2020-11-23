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

POSTMAN_DIR=../postman
API_CONTAINER_NAME=gio_apim_management_api

echoerr() { echo "\033[0;31m $@ \033[0m"; }
echosuccess() { echo "\033[0;32m $@ \033[0m"; }

set -e;

# Read args
for i in "$@"
do
  case $i in
    -p=*|--postman-dir=*)
    POSTMAN_DIR="${i#*=}"
    shift
    ;;
    -c=*|--container-name=*)
    API_CONTAINER_NAME="${i#*=}"
  esac
done

# Get container status and health, without quotes
CONTAINER_STATUS=$(docker inspect ${API_CONTAINER_NAME} --format='{{json .State.Status}}' | sed -e 's/^"//' -e 's/"$//')
CONTAINER_HEALTH=$(docker inspect ${API_CONTAINER_NAME} --format='{{json .State.Health.Status}}' | sed -e 's/^"//' -e 's/"$//')

echo "--------------------- INFOS ---------------------"
echo "POSTMAN_DIR           = ${POSTMAN_DIR}"
echo "API_CONTAINER_NAME    = ${API_CONTAINER_NAME}"
echo "CONTAINER_STATUS      = ${CONTAINER_STATUS}"
echo "CONTAINER_HEALTH      = ${CONTAINER_HEALTH}"
echo "-------------------------------------------------"
echo ""

# Verify container is ready before running newman tests
if [ $CONTAINER_STATUS = "running" ]; then
  if [ $CONTAINER_HEALTH = "healthy" ]; then
    echosuccess "Your container is 'healthy' ✅";
    echosuccess "Proceed Newman testing 🚀";
  else
    echo "Your container is not 'healthy', please retry in few seconds 🚧";
    exit;
  fi
elif [ $CONTAINER_STATUS = "starting" ]; then
  echo "Container is ${CONTAINER_STATUS} 🚧";
  echo "You should wait for your container to be 'running' and 'healthy'";
  echo "Please retry";
  exit;
else
  echoerr "Container is ${CONTAINER_STATUS} ❌";
  echoerr "Start your container and retry";
  exit;
fi

# Run all collections
for f in $POSTMAN_DIR/test/*;do if [[ -f $f ]]; then newman run $f -e $POSTMAN_DIR/env/Gravitee.io-Localhost-Environment.json --bail; fi; done;