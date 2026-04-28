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

rm -rf lib/portal-webclient-sdk
yarn dlx @openapitools/openapi-generator-cli@1.0.18-4.3.1 generate \
  -i ../gravitee-apim-rest-api/gravitee-apim-rest-api-portal/gravitee-apim-rest-api-portal-rest/src/main/resources/portal-openapi.yaml \
  -g typescript-fetch \
  -o lib/portal-webclient-sdk/src/lib/ \
  -puseSingleRequestParameter=true \
  -pmodelPropertyNaming=original \
  -psortModelPropertiesByRequiredFlag=false \
  -psortParamsByRequiredFlag=false \
  --import-mappings=DateTime=Date \
  --type-mappings=DateTime=Date,object=any \
  --reserved-words-mappings=configuration=configuration

sed -i.bak "s|export \* from './AnalyticsApi';||" lib/portal-webclient-sdk/src/lib/apis/index.ts
sed -i.bak "1,50 s|DateHistoAnalytics \| GroupByAnalytics \| CountAnalytics|CountAnalytics, DateHistoAnalytics, GroupByAnalytics|" lib/portal-webclient-sdk/src/lib/apis/AnalyticsApi.ts
sed -i.bak "1,50 s|CountAnalytics, DateHistoAnalytics, GroupByAnalyticsFromJSON,|GroupByAnalyticsFromJSON, CountAnalyticsFromJSON,|" lib/portal-webclient-sdk/src/lib/apis/AnalyticsApi.ts
sed -i.bak "1,50 s|CountAnalytics, DateHistoAnalytics, GroupByAnalyticsToJSON,|GroupByAnalyticsToJSON,|" lib/portal-webclient-sdk/src/lib/apis/AnalyticsApi.ts
sed -i.bak "s|DateHistoAnalytics \| GroupByAnalytics \| CountAnalyticsFromJSON(|CountAnalyticsFromJSON(|" lib/portal-webclient-sdk/src/lib/apis/AnalyticsApi.ts
sed -i.bak "1,100 s|DateHistoAnalytics \| GroupByAnalytics \| CountAnalytics|CountAnalytics, DateHistoAnalytics, GroupByAnalytics|" lib/portal-webclient-sdk/src/lib/apis/ApplicationApi.ts
sed -i.bak "1,100 s|CountAnalytics, DateHistoAnalytics, GroupByAnalyticsFromJSON,|GroupByAnalyticsFromJSON, CountAnalyticsFromJSON,|" lib/portal-webclient-sdk/src/lib/apis/ApplicationApi.ts
sed -i.bak "1,100 s|CountAnalytics, DateHistoAnalytics, GroupByAnalyticsToJSON,|GroupByAnalyticsToJSON,|" lib/portal-webclient-sdk/src/lib/apis/ApplicationApi.ts
sed -i.bak "s|DateHistoAnalytics \| GroupByAnalytics \| CountAnalyticsFromJSON(|CountAnalyticsFromJSON(|" lib/portal-webclient-sdk/src/lib/apis/ApplicationApi.ts

# Fix broken generated analytics models from OpenAPI Generator 4.3.1
sed -i.bak "s|return { ...numberFromJSONTyped(json, true), ...stringFromJSONTyped(json, true) };|return json;|" lib/portal-webclient-sdk/src/lib/models/AnalyticsInterval.ts
sed -i.bak "s|return { ...numberToJSON(value), ...stringToJSON(value) };|return value;|" lib/portal-webclient-sdk/src/lib/models/AnalyticsInterval.ts
sed -i.bak "s|return {...AnalyticsStringFilterFromJSONTyped(json, true), operator: 'EQ'};|return {...AnalyticsStringFilterFromJSONTyped(json, true), operator: 'EQ'} as AnalyticsFilter;|" lib/portal-webclient-sdk/src/lib/models/AnalyticsFilter.ts
sed -i.bak "s|return {...AnalyticsArrayFilterFromJSONTyped(json, true), operator: 'IN'};|return {...AnalyticsArrayFilterFromJSONTyped(json, true), operator: 'IN'} as AnalyticsFilter;|" lib/portal-webclient-sdk/src/lib/models/AnalyticsFilter.ts
sed -i.bak "s|return {...AnalyticsNumberFilterFromJSONTyped(json, true), operator: 'LTE'};|return {...AnalyticsNumberFilterFromJSONTyped(json, true), operator: 'LTE'} as AnalyticsFilter;|" lib/portal-webclient-sdk/src/lib/models/AnalyticsFilter.ts
sed -i.bak "s|return {...AnalyticsNumberFilterFromJSONTyped(json, true), operator: 'GTE'};|return {...AnalyticsNumberFilterFromJSONTyped(json, true), operator: 'GTE'} as AnalyticsFilter;|" lib/portal-webclient-sdk/src/lib/models/AnalyticsFilter.ts
sed -i.bak "s|return {...AnalyticsFacetBucketLeafFromJSONTyped(json, true), type: 'LEAF'};|return {...AnalyticsFacetBucketLeafFromJSONTyped(json, true), type: 'LEAF'} as AnalyticsFacetBucket;|" lib/portal-webclient-sdk/src/lib/models/AnalyticsFacetBucket.ts
sed -i.bak "s|return {...AnalyticsFacetBucketGroupFromJSONTyped(json, true), type: 'GROUP'};|return {...AnalyticsFacetBucketGroupFromJSONTyped(json, true), type: 'GROUP'} as AnalyticsFacetBucket;|" lib/portal-webclient-sdk/src/lib/models/AnalyticsFacetBucket.ts
sed -i.bak "s|return {...AnalyticsTimeSeriesBucketLeafFromJSONTyped(json, true), type: 'LEAF'};|return {...AnalyticsTimeSeriesBucketLeafFromJSONTyped(json, true), type: 'LEAF'} as AnalyticsTimeSeriesBucket;|" lib/portal-webclient-sdk/src/lib/models/AnalyticsTimeSeriesBucket.ts
sed -i.bak "s|return {...AnalyticsTimeSeriesBucketGroupFromJSONTyped(json, true), type: 'GROUP'};|return {...AnalyticsTimeSeriesBucketGroupFromJSONTyped(json, true), type: 'GROUP'} as AnalyticsTimeSeriesBucket;|" lib/portal-webclient-sdk/src/lib/models/AnalyticsTimeSeriesBucket.ts
#sed -i.bak "/export...from....AnalyticsApi.service';/d" lib/portal-webclient-sdk/src/lib/apis/ApiApi.ts
find lib/portal-webclient-sdk/src -name "*.ts" -exec sed -i.bak "/* The version of the OpenAPI document/d" {} \;
# must delete .bak files
find lib/portal-webclient-sdk/src -name "*.ts.bak" -exec rm -f {} \;
