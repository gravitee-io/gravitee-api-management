# Gravitee APIM e2e

This folder contains the integration tests of Gravitee.io API Management.

They are based on Cypress and Jest and can be run against a locally running APIM Rest API.


## Getting Started

First you need to install the dependencies & build test:

```shell
npm install
npm run build
```

Then, the following NPM scripts are available:
 - `npm run test:ui`: Run the APIM stack and the cypress UI tests in docker ([more details](./docker/ui-tests/README.md))
 - `npm run test:ui:dev`: Run the cypress UI tests against a locally running APIM
 - `npm run test:api:mongo`: Run the APIM stack with mongo and the jest API tests in docker ([more details](./docker/api-tests/README.md))
 - `npm run test:api:jdbc`: Run the APIM stack with jdbc and the jest API tests in docker ([more details](./docker/api-tests/README.md))
 - `npm run test:api:dev`: Run the jest API tests against a locally running APIM
 - `npm run apim:serve`: Run the APIM stack in docker with last apim release by default in order to run tests locally from your IDE.
    > We have to adapt wiremock according to the gateway location. With this command the gateway is executed in docker, so we have to change the variable to `WIREMOCK_BASE_URL=http://wiremock:8080` in .env
 - `npm run apim:serve -- wiremock`: To run only wiremock on docker  
   To run with latest master use this env-var `APIM_REGISTRY=graviteeio.azurecr.io APIM_TAG=master-latest npm run apim:serve`
 - `npm run apim:clean`: Stop and remove the containers & volumes.
 - `npm run bulk`: Run jest bulk scripts to create data to local environment
   - You can pass some options like `npm run bulk -- --apis=50 --applications=5`, available options:
       - apis (number): The number of APis to create
       - applications (number): The number of applications to create
       - skipStart (boolean): to not start created APIs
       - skipDeploy (boolean): to not deploy created APIs
       - skipSubscriptions (boolean): to not create subscriptions between APIs and applications
       - skipRatings (boolean): to not rate the APIs
 - `npm run update:sdk:management`: Generate the client for Management REST API
 - `npm run update:sdk:management:v2`: Generate the client for Management REST API V2
 - `npm run update:sdk:portal`: Generate the client for Portal REST API

## Structure

````
|api-test
|bulk
|ui-test
|_ assertions
|_ fakers
|_ fixtures
|_ model
|_ plugins
|_ support
|lib
|types
|scripts
````

| Folder 	                                | Description 	                                                                      |
|-----------------------------------------|------------------------------------------------------------------------------------|
| api-test/*     	                        | Management API related e2e tests                                                   |
| ui-test/assertions     	                | Utils classes to do common assertions for a particular object     	                |
| ui-test/fakers     	                    | Utils classes to generate fake data for particular object  	                       |
| ui-test/fixtures     	                  | Files containing static data to be used in tests through `cy.fixture(filePath)`  	 |
| ui-test/model       	                   | Types of our objects            	                                                  |
| ui-test/plugins       	                 | Load and configure plugins for cypress            	                                |
| ui-test/support       	                 | Processed and loaded automatically before your test files.            	            |
| lib/management-webclient-sdk       	    | Generated client for management API                                                |
| lib/management-v2-webclient-sdk       	 | Generated client for management API V2                                             |
| lib/portal-webclient-sdk       	        | Generated client for portal API                                                    |

## About management API clients

This project use project https://github.com/OpenAPITools/openapi-generator[openapi-generator] to generate a client sdk with openapi file.
If you want to generate the client for management API, start the management rest-api locally and run `npm run update:sdk:management`
If you want to generate the client for management API V2, start the management rest-api locally and run `npm run update:sdk:management:v2`
If you want to generate the client for portal API, just run `npm run update:sdk:portal`

## Environment variables

You can override all environments variables contained in `cypress.json` file, see documentation [here](https://docs.cypress.io/guides/guides/environment-variables#Setting).

