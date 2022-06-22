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

### Circle CI

During the `Test APIM e2e` build step, a lightweight version of the reports is published as a build artifact attached
to the step in circle CI. In order to reduce the time consumed uploading artifacts, this version only includes the 
indexes of the report (meaning that only package and class level coverage is available).

The `jacoco.exec` binary containing execution data is attached to the step as an artifact as well in order to be able
to download this file to the `gravitee-apim-e2e/jacoco` directory and run `npm run test:report:dev` without having
to run the test suites locally. This will generate a full report, including source files coverage and a XML report.
