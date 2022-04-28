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
  -i ../gravitee-apim-rest-api/gravitee-apim-rest-api-portal/gravitee-apim-rest-api-portal-rest/src/main/resources/openapi.yaml \
  -g typescript-fetch \
  -o lib/portal-webclient-sdk/src/lib/ \
  -puseSingleRequestParameter=true \
  -pmodelPropertyNaming=original \
  -psortModelPropertiesByRequiredFlag=false \
  -psortParamsByRequiredFlag=false \
  --import-mappings=DateTime=Date \
  --type-mappings=DateTime=Date,object=any \
  --reserved-words-mappings=configuration=configuration

sed -i.bak "s|export \* from \'\.\/AnalyticsApi\';||" lib/portal-webclient-sdk/src/lib/apis/index.ts
sed -i.bak "1,30 s|DateHistoAnalytics \| GroupByAnalytics \| CountAnalytics|CountAnalytics, DateHistoAnalytics, GroupByAnalytics|" lib/portal-webclient-sdk/src/lib/apis/AnalyticsApi.ts
sed -i.bak "1,30 s|CountAnalytics, DateHistoAnalytics, GroupByAnalyticsFromJSON,|GroupByAnalyticsFromJSON, CountAnalyticsFromJSON,|" lib/portal-webclient-sdk/src/lib/apis/AnalyticsApi.ts
sed -i.bak "1,30 s|CountAnalytics, DateHistoAnalytics, GroupByAnalyticsToJSON,|GroupByAnalyticsToJSON,|" lib/portal-webclient-sdk/src/lib/apis/AnalyticsApi.ts
sed -i.bak "s|DateHistoAnalytics \| GroupByAnalytics \| CountAnalyticsFromJSON(|CountAnalyticsFromJSON(|" lib/portal-webclient-sdk/src/lib/apis/AnalyticsApi.ts
sed -i.bak "1,50 s|DateHistoAnalytics \| GroupByAnalytics \| CountAnalytics|CountAnalytics, DateHistoAnalytics, GroupByAnalytics|" lib/portal-webclient-sdk/src/lib/apis/ApplicationApi.ts
sed -i.bak "1,50 s|CountAnalytics, DateHistoAnalytics, GroupByAnalyticsFromJSON,|GroupByAnalyticsFromJSON, CountAnalyticsFromJSON,|" lib/portal-webclient-sdk/src/lib/apis/ApplicationApi.ts
sed -i.bak "1,50 s|CountAnalytics, DateHistoAnalytics, GroupByAnalyticsToJSON,|GroupByAnalyticsToJSON,|" lib/portal-webclient-sdk/src/lib/apis/ApplicationApi.ts
sed -i.bak "s|DateHistoAnalytics \| GroupByAnalytics \| CountAnalyticsFromJSON(|CountAnalyticsFromJSON(|" lib/portal-webclient-sdk/src/lib/apis/ApplicationApi.ts
#sed -i.bak "/export...from....AnalyticsApi.service';/d" lib/portal-webclient-sdk/src/lib/apis/ApiApi.ts
find lib/portal-webclient-sdk/src -name "*.ts" -exec sed -i.bak "/* The version of the OpenAPI document/d" {} \;
# must delete .bak files
find lib/portal-webclient-sdk/src -name "*.ts.bak" -exec rm -f {} \;
