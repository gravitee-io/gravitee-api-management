// For a detailed explanation regarding each configuration property, visit:
// https://jestjs.io/docs/en/configuration.html

module.exports = {
  // A map from regular expressions to module names or to arrays of module names that allow to stub out resources with a single module
  moduleNameMapper: {
    '@client-conf/(.*)': '<rootDir>/dist/lib/configuration',
    '@management-fakers/(.*)': '<rootDir>/dist/lib/fixtures/management/$1',
    '@management-apis/(.*)': '<rootDir>/dist/lib/management-webclient-sdk/src/lib/apis/$1',
    '@management-models/(.*)': '<rootDir>/dist/lib/management-webclient-sdk/src/lib/models/$1',
    '@portal-apis/(.*)': '<rootDir>/dist/lib/portal-webclient-sdk/src/lib/apis/$1',
    '@portal-models/(.*)': '<rootDir>/dist/lib/portal-webclient-sdk/src/lib/models/$1',
    '@portal-fakers/(.*)': '<rootDir>/dist/lib/fixtures/portal/$1',
    '@api-test-resources/(.*)': '<rootDir>/api-test/resources/$1',
    '@lib/jest-utils': '<rootDir>/dist/lib/jest-utils',
    '@lib/gateway': '<rootDir>/dist/lib/gateway',
  },

  // The test environment that will be used for testing
  testEnvironment: 'node',

  // The glob patterns Jest uses to detect test files
  testMatch: ['<rootDir>/dist/api-test/**/?(*.)+(spec|test).[tj]s?(x)'],

  testTimeout: 30000,

  // A map from regular expressions to paths to transformers
  transform: {
    '^.+\\.xml$': '<rootDir>/lib/jest-raw-loader.js',
  },
};
