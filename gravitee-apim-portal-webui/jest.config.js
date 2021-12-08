// jest.config.js (located at the project root)

// base config from jest-present-angular
const jestPreset = require('jest-preset-angular/jest-preset');
const { globals } = jestPreset;
const tsjest = globals['ts-jest'];

// set the correct path to the spect ts-config file
// the default for the jest-preset-angular package
// points to an incorrect path:
// <rootDir/src/tsconfig.spec.js
const tsjestOverrides = {
  ...tsjest,
  tsConfig: '<rootDir>/tsconfig.spec.json',
};

const globalOverrides = {
  ...globals,
  'ts-jest': { ...tsjestOverrides },
};

// make sure to add in the required preset and
// and setup file entries
module.exports = {
  ...jestPreset,
  globals: { ...globalOverrides },
  preset: 'jest-preset-angular',
  setupFilesAfterEnv: ['<rootDir>/src/setup-jest.ts'],
  transformIgnorePatterns: [
    '/node_modules/(?!(@gravitee/ui-components/.*?\\.js)|lit|@lit/reactive-element|(lit-element/.*?\\.js)|(lit-html/.*?\\.js)|(resize-observer-polyfill/.*?\\.js)|(date-fns/.*?\\.js)$)',
  ],
  transform: {
    '^.+\\.js$': 'babel-jest',
  },
  verbose: true,
};
