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
POSTMAN_ENV=localhost
POSTMAN_HEALTH_URL=localhost:8083/management/swagger.json

echoerr() { echo "\033[0;31m $@ \033[0m"; }
echosuccess() { echo "\033[0;32m $@ \033[0m"; }

# Read args
for i in "$@"
do
  case $i in
    -p=*|--postman-dir=*)
    POSTMAN_DIR="${i#*=}"
    shift
    ;;
    -e=*|--postman-env=*)
    POSTMAN_ENV="${i#*=}"
    shift
    ;;
    -u=*|--postman-health-url=*)
    POSTMAN_HEALTH_URL="${i#*=}"
  esac
done

echo "--------------------- INFOS ---------------------"
echo "POSTMAN_DIR           = ${POSTMAN_DIR}"
echo "POSTMAN_ENV           = ${POSTMAN_ENV}"
echo "POSTMAN_HEALTH_URL    = ${POSTMAN_HEALTH_URL}"
echo "-------------------------------------------------"
echo ""

# Check if POSTMAN_ENV value is a valid environment
if [ $POSTMAN_ENV != "demo" ] && [ $POSTMAN_ENV != "nightly" ] && [ $POSTMAN_ENV != "localhost" ]; then
  echoerr "$POSTMAN_ENV is not a valid environment ❌";
  echoerr "You should try with nightly, demo or localhost (default)";
  exit;
fi

echo "Tests will be run on $POSTMAN_ENV"

STATUS=0
RETRY_LIMIT=10
RETRY_COUNT=1
INTERVAL=5

echo "Check availability of environment every $INTERVAL seconds for $RETRY_LIMIT iterations"
while [ $STATUS -ne 200 ] && [ $RETRY_COUNT -lt $RETRY_LIMIT ]
do
  echo "Try to $POSTMAN_HEALTH_URL, retry number: $RETRY_COUNT"
  STATUS=$(curl -s -o /dev/null -w "%{http_code}" --location $POSTMAN_HEALTH_URL );
  RETRY_COUNT=$((RETRY_COUNT + 1));
  echo "Request returned status $STATUS";
  if [ $STATUS -eq 200 ]; then
    INTERVAL=0
  fi
  sleep $INTERVAL;
done ;

if [ $STATUS -ne 200 ]; then
  echoerr "$POSTMAN_ENV is not available ❌";
  exit;
else
  echosuccess "$POSTMAN_ENV is available ✅"
  echosuccess "Proceed Newman testing 🚀";
fi

set -e;

# Run all collections
for f in $POSTMAN_DIR/test/*;do if [[ -f $f ]]; then newman run $f -e $POSTMAN_DIR/env/Gravitee.io-$POSTMAN_ENV-Environment.json --bail; fi; done;