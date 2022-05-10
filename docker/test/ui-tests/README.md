# MongoDB

Here is a docker-compose to run APIM jest api e2e tests with MongoDB as database.

---
> For more information, please read :
> https://docs.gravitee.io/apim/3.x/apim_installguide_repositories_mongodb.html
---

## How to run ?

We assume that the following command line uses the current location of the user. To work this command line needs to be run
from gravitee-api-management root directory.

```bash
APIM_REGISTRY={APIM_REGISTRY} APIM_TAG={APIM_TAG} docker-compose -f ./docker/test/common/docker-compose-apis.yml -f ./docker/test/common/docker-compose-uis.yml -f ./docker/test/ui-tests/docker-compose-ui-tests.yml -p ui-tests --project-directory $PWD up --no-build --abort-on-container-exit --exit-code-from cypress
```
