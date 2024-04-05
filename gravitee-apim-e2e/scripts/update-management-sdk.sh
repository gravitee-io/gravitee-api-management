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

rm -rf lib/management-webclient-sdk
mkdir -p .tmp
sed s/'"uniqueItems":true'/'"uniqueItems":false'/g ../gravitee-apim-rest-api/gravitee-apim-rest-api-management/gravitee-apim-rest-api-management-rest/target/classes/console-openapi.json > .tmp/console-openapi.json

# magic sed to properly handle multipart/form-data endpoints
export var1='{"multipart/form-data":{'
export var2='{"*/*":{'
sed -i.bak s@$var1@$var2@g .tmp/console-openapi.json

yarn dlx @openapitools/openapi-generator-cli@2.13.1 generate \
  -i .tmp/console-openapi.json \
  -g typescript-fetch \
  -o lib/management-webclient-sdk/src/lib/ \
  -puseSingleRequestParameter=true \
  -pmodelPropertyNaming=original \
  -penumPropertyNaming=UPPERCASE \
  -psortModelPropertiesByRequiredFlag=false \
  -psortParamsByRequiredFlag=false \
  --import-mappings=DateTime=Date \
  --type-mappings=DateTime=Date,object=any \
  --reserved-words-mappings=configuration=configuration

find lib/management-webclient-sdk/src/lib/ -name "*.ts" -exec sed -i.bak "/* The version of the OpenAPI document/d" {} \+

# Magic `sed` to properly handle `text/plain` in TS SDK, until we have a proper way to handle it in the OpenAPI generator
export var1="overridedInit.body instanceof URLSearchParams ||"
export var2="overridedInit.body instanceof URLSearchParams || typeof overridedInit.body === 'string' ||"
find lib/management-webclient-sdk/src/lib/ -name "*.ts" -exec sed -i.bak "s/$var1/$var2/g" {} \+

# must delete .bak files
find lib/management-webclient-sdk/src/lib/ -name "*.ts.bak" -exec rm -f {} \+
rm -f lib/management-webclient-sdk/src/lib/index.ts
rm -f lib/management-webclient-sdk/src/lib/apis/index.ts
rm .tmp/console-openapi*