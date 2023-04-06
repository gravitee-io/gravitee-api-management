#!/bin/bash

npx @openapitools/openapi-generator-cli@1.0.18-4.3.1 generate \
-i ../gravitee-apim-rest-api/gravitee-apim-rest-api-portal/gravitee-apim-rest-api-portal-rest/src/main/resources/portal-openapi.yaml \
-g typescript-angular \
-o projects/portal-webclient-sdk/src/lib/ \
-puseSingleRequestParameter=true \
-pmodelPropertyNaming=original \
--import-mappings=DateTime=Date \
--type-mappings=DateTime=Date

# adding a .bak is a hack so the `sed` command works on both MacOS (BSD) and other UNIX (GNU) system.
# https://stackoverflow.com/questions/5694228/sed-in-place-flag-that-works-both-on-mac-bsd-and-linux
sed -i.bak "s|{ DateHistoAnalytics \| GroupByAnalytics \| CountAnalytics }|{ CountAnalytics, DateHistoAnalytics, GroupByAnalytics }|" projects/portal-webclient-sdk/src/lib/api/analytics.service.ts
sed -i.bak "s|{ DateHistoAnalytics \| GroupByAnalytics \| CountAnalytics }|{ CountAnalytics, DateHistoAnalytics, GroupByAnalytics }|" projects/portal-webclient-sdk/src/lib/api/application.service.ts
sed -i.bak "/export...from....analytics.service';/d" projects/portal-webclient-sdk/src/lib/api/api.ts
find projects/portal-webclient-sdk/src -name "*.ts" -exec sed -i.bak "/* The version of the OpenAPI document/d" {} \;
# must delete .bak files
find projects/portal-webclient-sdk/src -name "*.ts.bak" -exec rm -f {} \;
