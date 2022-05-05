# MongoDB

Here is a docker-compose to run APIM jest api e2e tests with MongoDB as database.

---
> For more information, please read :
> https://docs.gravitee.io/apim/3.x/apim_installguide_repositories_mongodb.html
---

## How to run ?

```bash
APIM_REGISTRY={APIM_REGISTRY} APIM_TAG={APIM_TAG} docker-compose -f ./docker/test/common/docker-compose-apis.yml -f ./docker/test/api-tests/docker-compose-api-tests.yml -p api-integration-test up --abort-on-container-exit --exit-code-from jest-e2e
```
