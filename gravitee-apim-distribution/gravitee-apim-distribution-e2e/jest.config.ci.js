// For a detailed explanation regarding each configuration property, visit:
// https://jestjs.io/docs/en/configuration.html

module.exports = {
  // A map from regular expressions to module names or to arrays of module names that allow to stub out resources with a single module
  moduleNameMapper: {
    '@api-test-resources/(.*)': '<rootDir>/api-test/resources/$1',
    '@gravitee/fixtures/(.*)': '<rootDir>/dist/lib/fixtures/$1',
    '@gravitee/management-webclient-sdk/(.*)': '<rootDir>/dist/lib/management-webclient-sdk/$1',
    '@gravitee/management-v2-webclient-sdk/(.*)': '<rootDir>/dist/lib/management-v2-webclient-sdk/$1',
    '@gravitee/portal-webclient-sdk/(.*)': '<rootDir>/dist/lib/portal-webclient-sdk/$1',
    '@gravitee/utils/(.*)': '<rootDir>/dist/lib/utils/$1',
    '@lib/jest-utils': '<rootDir>/dist/lib/jest-utils',
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

  setupFilesAfterEnv: ['<rootDir>/dist/api-test/jest.setup.js'],

  reporters: ['default', ['jest-junit', { outputDirectory: '.tmp', outputName: 'e2e-test-report.xml' }]],
};
