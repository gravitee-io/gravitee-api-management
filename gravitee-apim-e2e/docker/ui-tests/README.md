# E2E UI tests

Here is a docker-compose to run APIM E2E UI tests using jest, with MongoDB as database.

## How to run ?

From the folder gravitee-apim-e2e folder, run UI E2E tests using this command ```npm run test:ui```

You can also only run the full APIM stack, in order to run tests from your IDE, with ```npm run serve:apim```

> **_NOTE:_**  You can set the used APIM version with the APIM_TAG parameter in `gravitee-apim-e2e/.env` file, and the associated APIM_REGISTRY.