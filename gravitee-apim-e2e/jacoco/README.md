# APIM E2E Tests Coverage Report

Both APIM Gateway and APIM Management containers used during end to end tests run a jacoco agent that will publish an 
execution report to this folder in a `jacoco.exec` file that can be used to generate coverage reports.

### Prerequisites

  - `apache ant` must be installed to generate the reports
  - `mvn clean install` must be run both for APIM Gateway and APIM Rest API

## Generating reports

```shell
npm run test:api # tests must complete to generate execution data
npm run test:report:dev
```
