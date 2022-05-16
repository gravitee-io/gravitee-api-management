# Gravitee APIM e2e

This folder contains the integration tests of Gravitee.io API Management.

They are based on Cypress and Jest and can be run against a locally running APIM Rest API.


## Getting Started

First you need to install the dependencies:

```shell
npm install
```

Then, the following NPM scripts are available:
 - `npm run test:ui`: Run the APIM stack and the cypress UI tests in docker ([more details](./docker/ui-tests/README.md))
 - `npm run test:ui:dev`: Run the cypress UI tests against a locally running APIM
 - `npm run test:api`: Run the APIM stack and the jest API tests in docker ([more details](./docker/api-tests/README.md))
 - `npm run test:api:dev`: Run the jest API tests against a locally running APIM
 - `npm run serve:apim`: Run the APIM stack in docker in order to run tests locally from your IDE

## Structure
````
|api-test
|ui-test
|_ assertions
|_ fakers
|_ fixtures
|_ model
|_ plugins
|_ support
|lib
|scripts
````

| Folder 	                             | Description 	                                                                      |
|--------------------------------------|------------------------------------------------------------------------------------|
| api-test/apis     	                  | Api management API related e2e tests                                               |
| api-test/applications     	          | Api management applications related e2e tests                                      |
| api-test/portal     	                | Api portal e2e tests                                                               |
| api-test/gateway     	               | Gateway e2e tests                                                                  |
| ui-test/assertions     	             | Utils classes to do common assertions for a particular object     	                |
| ui-test/fakers     	                 | Utils classes to generate fake data for particular object  	                       |
| ui-test/fixtures     	               | Files containing static data to be used in tests through `cy.fixture(filePath)`  	 |
| ui-test/model       	                | Types of our objects            	                                                  |
| ui-test/plugins       	              | Load and configure plugins for cypress            	                                |
| ui-test/support       	              | Processed and loaded automatically before your test files.            	            |
| lib/management-webclient-sdk       	 | Generated client for management API                                                |
| lib/portal-webclient-sdk       	     | Generated client for portal API                                                    |

## About management API client 

This project use project https://github.com/OpenAPITools/openapi-generator[openapi-generator] to generate a client sdk with openapi file.
If you want to generate the client for management API, start the management rest-api locally and run `npm run update:sdk:management`
If you want to generate the client for portal API, just run `npm run update:sdk:portal`

## Environment variables

You can override all environments variables contained in `cypress.json` file, see documentation [here](https://docs.cypress.io/guides/guides/environment-variables#Setting).

