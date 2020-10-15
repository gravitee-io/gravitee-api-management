#!/bin/bash

npx @openapitools/openapi-generator-cli@1.0.12-4.2.1 generate \
-i ../gravitee-management-rest-api/gravitee-rest-api-portal/gravitee-rest-api-portal-rest/src/main/resources/openapi.yaml \
-g typescript-angular \
-o projects/portal-webclient-sdk/src/lib/ \
-puseSingleRequestParameter=true \
-pmodelPropertyNaming=original
