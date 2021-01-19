#!/bin/bash

npx @openapitools/openapi-generator-cli@1.0.18-4.3.1 generate \
-i ../gravitee-management-rest-api/gravitee-rest-api-portal/gravitee-rest-api-portal-rest/src/main/resources/openapi.yaml \
-g typescript-angular \
-o projects/portal-webclient-sdk/src/lib/ \
-puseSingleRequestParameter=true \
-pmodelPropertyNaming=original

# adding a .bak is a hack so the `sed` command works on both MacOS (BSD) and other UNIX (GNU) system.
# https://stackoverflow.com/questions/5694228/sed-in-place-flag-that-works-both-on-mac-bsd-and-linux
sed -i.bak "s|import.*dateHistoAnalyticsGroupByAnalyticsCountAnalytics';|import { CountAnalytics, DateHistoAnalytics, GroupByAnalytics } from '../model/models';|" projects/portal-webclient-sdk/src/lib/api/analytics.service.ts
sed -i.bak "s|import.*../model/dateHistoAnalyticsGroupByAnalyticsCountAnalytics';|import { CountAnalytics, DateHistoAnalytics, GroupByAnalytics } from '../model/models';|" projects/portal-webclient-sdk/src/lib/api/application.service.ts
sed -i.bak "/export...from....analytics.service';/d" projects/portal-webclient-sdk/src/lib/api/api.ts
# must delete .bak files
rm projects/portal-webclient-sdk/src/lib/api/analytics.service.ts.bak
rm projects/portal-webclient-sdk/src/lib/api/application.service.ts.bak
rm projects/portal-webclient-sdk/src/lib/api/api.ts.bak
