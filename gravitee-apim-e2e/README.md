# Gravitee APIM e2e

This folder contains the integration tests of Gravitee.io API Management.

They are based on Cypress and Jest and can be run against a locally running APIM Rest API.


## Getting Started

First you need to install the dependencies:

```shell
npm install
```

Then, the following NPM scripts are available:
 - `npm run test`: Opens the Cypress Test Runner to run tests against a locally running APIM Rest API
 - `npm run test:api:management`: Run end to end tests of management rest-api against a locally running APIM.

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
| api-test/management     	            | Api management e2e tests                                                           |
| api-test/portal     	                | Api portal e2e tests (soon)                                                        |
| api-test/gateway     	               | Api gateway e2e tests (soon)                                                       |
| ui-test/assertions     	             | Utils classes to do common assertions for a particular object     	                |
| ui-test/fakers     	                 | Utils classes to generate fake data for particular object  	                       |
| ui-test/fixtures     	               | Files containing static data to be used in tests through `cy.fixture(filePath)`  	 |
| ui-test/model       	                | Types of our objects            	                                                  |
| ui-test/plugins       	              | Load and configure plugins for cypress            	                                |
| ui-test/support       	              | Processed and loaded automatically before your test files.            	            |
| lib/management-webclient-sdk       	 | Generated client for management API                                                |

## About management API client 

This project use project https://github.com/OpenAPITools/openapi-generator[openapi-generator] to generate a client sdk with openapi file.
If you want to generate the client, start the management rest-api locally and run `npm run update:sdk`

## Environment variables

You can override all environments variables contained in `cypress.json` file, see documentation [here](https://docs.cypress.io/guides/guides/environment-variables#Setting).

