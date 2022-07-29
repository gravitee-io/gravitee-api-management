# Gravitee.io APIM - performance

This folder contains a performance tools of Gravitee.io API Management.

They are based on k6 and can be run against a locally running APIM Rest API.

## Prerequisites
- 
- [nvm](https://github.com/nvm-sh/nvm)
- [k6](https://k6.io/docs/getting-started/installation)
- [NodeJS](https://nodejs.org/en/download/)
- [Yarn](https://yarnpkg.com/getting-started/install) (optional)

Use with `nvm use` or install with `nvm install` the version of Node.js declared in `.nvmrc`

## Installation

**Install dependencies**

```bash
$ yarn install
```

## Running the test

1. Start an APIM instance with `gravitee_services_sync_delay=1000` on Gateway.
2. Check the configuration on `.env` file.
3. Write your test at root of `src` folder, here `src/get-200-status.setup.ts`;
4. Call the runner:

```bash
$ ./scripts/test-runner.ts  -f src/get-200-status-nosetup.test.js
```

## Writing own tests

House rules for writing tests:
- The test code is located in `src` folder

