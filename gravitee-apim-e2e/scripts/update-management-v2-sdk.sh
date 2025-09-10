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

rm -rf lib/management-v2-webclient-sdk
mkdir -p .tmp
sed s/'uniqueItems: true'/'uniqueItems: false'/g ../gravitee-apim-rest-api/gravitee-apim-rest-api-management-v2/gravitee-apim-rest-api-management-v2-rest/src/main/resources/openapi/openapi-environments.yaml > .tmp/openapi-environments.yaml
sed s/'uniqueItems: true'/'uniqueItems: false'/g ../gravitee-apim-rest-api/gravitee-apim-rest-api-management-v2/gravitee-apim-rest-api-management-v2-rest/src/main/resources/openapi/openapi-apis.yaml > .tmp/openapi-apis.yaml

yarn dlx @openapitools/openapi-generator-cli@2.13.1 generate \
  -i .tmp/openapi-apis.yaml \
  -g typescript-fetch \
  -o lib/management-v2-webclient-sdk/src/lib/ \
  -puseSingleRequestParameter=true \
  -pmodelPropertyNaming=original \
  -penumPropertyNaming=UPPERCASE \
  -psortModelPropertiesByRequiredFlag=false \
  -psortParamsByRequiredFlag=false \
  --import-mappings=DateTime=Date \
  --type-mappings=DateTime=Date,object=any \
  --reserved-words-mappings=configuration=configuration

# Create unique enum entry for descending sort params
sed -i.bak "s|NAME: '-name'|_NAME: '-name'|" lib/management-v2-webclient-sdk/src/lib/apis/APIsApi.ts
sed -i.bak "s|PATHS: '-paths'|_PATHS: '-paths'|" lib/management-v2-webclient-sdk/src/lib/apis/APIsApi.ts
sed -i.bak "s|KEY: '-key'|_KEY: '-key'|" lib/management-v2-webclient-sdk/src/lib/apis/APIsApi.ts
sed -i.bak "s|FORMAT: '-format'|_FORMAT: '-format'|" lib/management-v2-webclient-sdk/src/lib/apis/APIsApi.ts
sed -i.bak "s|VALUE: '-value'|_VALUE: '-value'|" lib/management-v2-webclient-sdk/src/lib/apis/APIsApi.ts
sed -i.bak "s|API_TYPE: '-api_type'|_API_TYPE: '-api_type'|" lib/management-v2-webclient-sdk/src/lib/apis/APIsApi.ts
sed -i.bak "s|STATUS: '-status'|_STATUS: '-status'|" lib/management-v2-webclient-sdk/src/lib/apis/APIsApi.ts
sed -i.bak "s|OWNER: '-owner'|_OWNER: '-owner'|" lib/management-v2-webclient-sdk/src/lib/apis/APIsApi.ts
sed -i.bak "s|VISIBILITY: '-visibility'|_VISIBILITY: '-visibility'|" lib/management-v2-webclient-sdk/src/lib/apis/APIsApi.ts

# Remove duplicate `HttpEndpointV2FromJSONTyped` import
sed -i.bak "s|     HttpEndpointV2FromJSONTyped,||" lib/management-v2-webclient-sdk/src/lib/models/BaseEndpointV2.ts

find lib/management-v2-webclient-sdk/src -name "*.ts" -exec sed -i.bak "/* The version of the OpenAPI document/d" {} \;
# must delete .bak files
find lib/management-v2-webclient-sdk/src -name "*.ts.bak" -exec rm -f {} \;