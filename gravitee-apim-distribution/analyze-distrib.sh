#!/usr/bin/env bash
#
# Copyright © 2015 The Gravitee team (http://gravitee.io)
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#


REST_API_DIST=gravitee-apim-rest-api/gravitee-apim-rest-api-standalone/gravitee-apim-rest-api-standalone-distribution/target/distribution
GATEWAY_DIST=gravitee-apim-gateway/gravitee-apim-gateway-standalone/gravitee-apim-gateway-standalone-distribution/target/distribution

unzip_plugins() {
  for pathToZip in $(find ./plugins -name "*.zip"); do
    path=$(dirname "$pathToZip")
    zipName=$(basename "$pathToZip")
    unzip -o -qq "$pathToZip" -d "$path/${zipName%.zip}"
  done
}

formatLine() {
  size=$1
  path=$(dirname "$2")
  jarName=$(basename "$2")

  regex="(.*)-([0-9]+\.[0-9]+(\.[0-9]+)?(-(alpha|beta|rc)\.[0-9]+)?(-SNAPSHOT)?)"

  if [[ $jarName =~ $regex ]]
  then
    artefactName="${BASH_REMATCH[1]}"
    artefactVersion="${BASH_REMATCH[2]}"

    echo "$path,$artefactName,'$artefactVersion,$size"
  fi
}

analyze_lib() {
  declare -a files;
  files=($(find . -name "*.jar" -exec du {} \;))
  for (( i = 0; i < ${#files[*]}; i=i+2 ))
  do
    formatLine ${files[$i]} ${files[$i+1]}
  done
}

clean_unzip_plugins() {
  for pathToZip in $(find ./plugins -name "*.zip"); do
    path=$(dirname "$pathToZip")
    zipName=$(basename "$pathToZip")
    rm -rf "$path/${zipName%.zip}"
  done
}
currentFolder=$(pwd)
echo ------------------------------------------------------------
echo Analyze RestAPI
pushd $REST_API_DIST || exit
unzip_plugins
analyze_lib > "$currentFolder/rest-api.logs"
clean_unzip_plugins
popd || exit
echo ------------------------------------------------------------
echo
echo ------------------------------------------------------------
echo Analyze Gateway
pushd $GATEWAY_DIST || exit
unzip_plugins
analyze_lib > "$currentFolder/gateway.logs"
clean_unzip_plugins
popd || exit
echo ------------------------------------------------------------