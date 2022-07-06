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

npx @openapitools/openapi-generator-cli@1.0.18-4.3.1 generate \
  -i ../gravitee-apim-rest-api/gravitee-apim-rest-api-management/gravitee-apim-rest-api-management-rest/target/classes/openapi.json \
  -g typescript-fetch \
  -o lib/management-webclient-sdk/src/lib/ \
  -puseSingleRequestParameter=true \
  -pmodelPropertyNaming=original \
  -psortModelPropertiesByRequiredFlag=false \
  -psortParamsByRequiredFlag=false \
  --import-mappings=DateTime=Date \
  --type-mappings=DateTime=Date,object=any \
  --reserved-words-mappings=configuration=configuration


find lib/management-webclient-sdk/src/lib/ -name "*.ts" -exec sed -i.bak "/* The version of the OpenAPI document/d" {} \;
# Magic `sed` to properly handle `text/plain` in TS SDK, until we have a proper way to handle it in the OpenAPI generator
export var1="const body = ((typeof FormData !== \"undefined\" \&\& context.body instanceof FormData) || context.body instanceof URLSearchParams || isBlob(context.body))"
export var2="const body = ((typeof FormData !== \"undefined\" \&\& context.body instanceof FormData) || context.body instanceof URLSearchParams || isBlob(context.body)) || typeof context.body === 'string'"
find lib/management-webclient-sdk/src/lib/ -name "*.ts" -exec sed -i.bak "s/$var1/$var2/g" {} \;

# must delete .bak files
find lib/management-webclient-sdk/src/lib/ -name "*.ts.bak" -exec rm -f {} \;
rm -f lib/management-webclient-sdk/src/lib/index.ts
rm -f lib/management-webclient-sdk/src/lib/apis/index.ts
