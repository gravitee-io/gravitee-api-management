// For a detailed explanation regarding each configuration property, visit:
// https://jestjs.io/docs/en/configuration.html

module.exports = {
  // An array of regexp pattern strings used to skip coverage collection
  coveragePathIgnorePatterns: ['<rootDir>/node_modules/', '<rootDir>/dist/'],

  // A map from regular expressions to module names or to arrays of module names that allow to stub out resources with a single module
  moduleNameMapper: {
    '@api-test-resources/(.*)': '<rootDir>/api-test/resources/$1',
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

  transformIgnorePatterns: ['/node_modules/(?!(@gravitee)/)', '\\.pnp\\.[^\\/]+$'],

  testTimeout: 300000,

  // Indicates whether each individual test should be reported during the run
  verbose: true,
};
