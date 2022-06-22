// For a detailed explanation regarding each configuration property, visit:
// https://jestjs.io/docs/en/configuration.html

module.exports = {
  // An array of regexp pattern strings used to skip coverage collection
  coveragePathIgnorePatterns: ['<rootDir>/node_modules/', '<rootDir>/dist/'],

  // A map from regular expressions to module names or to arrays of module names that allow to stub out resources with a single module
  moduleNameMapper: {
    '@client-conf/(.*)': '<rootDir>/lib/configuration',
    '@management-fakers/(.*)': '<rootDir>/lib/fixtures/management/$1',
    '@management-apis/(.*)': '<rootDir>/lib/management-webclient-sdk/src/lib/apis/$1',
    '@management-models/(.*)': '<rootDir>/lib/management-webclient-sdk/src/lib/models/$1',
    '@portal-apis/(.*)': '<rootDir>/lib/portal-webclient-sdk/src/lib/apis/$1',
    '@portal-models/(.*)': '<rootDir>/lib/portal-webclient-sdk/src/lib/models/$1',
    '@api-test-resources/(.*)': '<rootDir>/api-test/resources/$1',
    '@lib/gateway': '<rootDir>/lib/gateway',
    '@lib/management': '<rootDir>/lib/management',
  },

  // The test environment that will be used for testing
  testEnvironment: 'node',

  // The glob patterns Jest uses to detect test files
  testMatch: ['<rootDir>/bulk/**/?(*.)+(bulk).[tj]s?(x)'],

  // A map from regular expressions to paths to transformers
  transform: {
    '\\.(js|jsx|ts|tsx)$': '@sucrase/jest-plugin',
    '^.+\\.xml$': '<rootDir>/lib/jest-raw-loader.js',
  },

  testTimeout: 300000,

  // Indicates whether each individual test should be reported during the run
  verbose: true,
};
