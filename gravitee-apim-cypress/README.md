# Integration tests with Cypress

This folder contains the integration tests of Gravitee.io API Management.

They are based on Cypress and can be run against a locally running APIM Rest API.


## Getting Started

First you need to install the dependencies:

```shell
npm install
```

Then, the following NPM scripts are available:
 - `npm run test`: Opens the Cypress Test Runner to run tests against a locally running APIM Rest API  
 - `npm run test:ci`: Run integration tests with Cypress against a locally running APIM Rest API
 - `npm run test:dev`: Run integration tests with Cypress against a locally running APIM Rest API with verbose (**requests, responses and calls to cy.log() are logged**)
 - `npm run test:e2e`: Run end to end tests with Cypress against a locally running APIM, testing is made against UI
 - `npm run test:bulk`: Run Cypress to insert bulk data in APIM

## Structure
````
Cypress
|_ assertions
|_ e2e
|_ fakers
|_ fixtures
|_ model
|_ nrt
|_ plugins
|_ support
````

| Folder 	            | Description 	|
|--------	            |-------------	|
| assertions     	    | Utils classes to do common assertions for a particular object     	|
| e2e     	            | End to end testing folder (tests run against UI)     	|
| fakers     	        | Utils classes to generate fake data for particular object  	|
| fixtures     	        | Files containing static data to be used in tests through `cy.fixture(filePath)`  	|
| model       	        | Types of our objects            	|
| nrt       	        | Non regression testing folder            	|
| plugins       	    | Load and configure plugins for cypress            	|
| support       	    | Processed and loaded automatically before your test files.            	|

## Environment variables

You can override all environments variables contained in `cypress.json` file, see documentation [here](https://docs.cypress.io/guides/guides/environment-variables#Setting).

