# Gravitee.io APIM - performance

This repository provides a scaffolding project to start using TypeScript in your k6 scripts.

## Rationale

While JavaScript is great for a myriad of reasons, one area where it fall short is type safety and developer ergonomics. It's perfectly possible to write JavaScript code that will look OK and behave OK until a certain condition forces the executor into a faulty branch.

While it, of course, still is possible to shoot yourself in the foot with TypeScript as well, it's significantly harder. Without adding much overhead, TypeScript will:

- Improve the ability to safely refactor your code.
- Improve readability and maintainability.
- Allow you to drop a lot of the defensive code previously needed to make sure consumers are calling functions properly.


## Prerequisites

- [k6](https://k6.io/docs/getting-started/installation)
- [NodeJS](https://nodejs.org/en/download/)
- [Yarn](https://yarnpkg.com/getting-started/install) (optional)

## Installation

**Install dependencies**

Clone the generated repository on your local machine, move to the project root folder and install the dependencies defined in [`package.json`](./package.json)

```bash
$ yarn install
```

## Running the test

Start a local APIM instance and create an API on `/echo`.

To run a test written in TypeScript, we first have to transpile the TypeScript code into JavaScript and bundle the project

```bash
$ yarn webpack
```

This command creates the final test files to the `./dist` folder.

Once that is done, we can run our script the same way we usually do, for instance:

```bash
# Running a 30-second, 10-VU load test
$ k6 run --vus 10 --duration 30s dist/get-200-status-test.js
```

## Writing own tests

House rules for writing tests:
- The test code is located in `src` folder
- The entry points for the tests need to have "_test_" word in the name to distinguish them from auxiliary files. You can change the entry [here](./webpack.config.js#L8). 
- If static files are required then add them to `./assets` folder. Its content gets copied to the destination folder (`dist`) along with compiled scripts.

### Transpiling and Bundling

By default, k6 can only run ES5.1 JavaScript code. To use TypeScript, we have to set up a bundler that converts TypeScript to JavaScript code. 

This project uses `Babel` and `Webpack` to bundle the different files - using the configuration of the [`webpack.config.js`](./webpack.config.js) file.

If you want to learn more, check out [Bundling node modules in k6](https://k6.io/docs/using-k6/modules#bundling-node-modules).
