# E2E UI tests

Here is a docker-compose to run APIM E2E UI tests using jest, with MongoDB as database.

## How to run ?

We assume that the following command line uses the current location of the user. To work this command line needs to be run
from gravitee-api-management/gravitee-apim-e2e directory.

```bash
APIM_REGISTRY={APIM_REGISTRY} APIM_TAG={APIM_TAG} docker-compose -f ./docker/common/docker-compose-apis.yml -f ./docker/common/docker-compose-uis.yml -f ./docker/ui-tests/docker-compose-ui-tests.yml -p ui-tests --project-directory $PWD up --no-build --abort-on-container-exit --exit-code-from cypress
```
