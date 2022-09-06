# APIM E2E Tests Coverage Report

Both APIM Gateway and APIM Management containers used during end to end tests run a jacoco agent that will publish an 
execution report to this folder in a `jacoco.exec` file that can be used to generate coverage reports.

### Prerequisites

  - `apache ant` must be installed to generate the reports
  - `mvn clean install` must be run both for APIM Gateway and APIM Rest API

## Generating reports

```shell

JACOCO_OPTS="-javaagent:/opt/jacoco/lib/org.jacoco.agent-0.8.8-runtime.jar=destfile=/opt/jacoco/jacoco.exec" \
  npm run test:api && npm run test:report:dev # tests must complete to generate execution data
```

> **_NOTE:_**  When `npm run test:api` is used in CircleCI, we do not want to use JaCoCo. That's why JACOCO_OPTS is not set by default. But you can set the JACOCO_OPTS env var to activate it locally 
