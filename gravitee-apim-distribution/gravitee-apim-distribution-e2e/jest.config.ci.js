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
    // @faker-js/faker dropped its CommonJS build in v10. These suites run as CommonJS, and
    // Jest's module registry cannot `require` an ES module, so the package is transpiled on
    // the way in. The pattern stays on the package itself: transpiling all of node_modules
    // would cost far more than it buys.
    '/node_modules/@faker-js/faker/.+\\.js$': [
      'ts-jest',
      { tsconfig: { allowJs: true, module: 'CommonJS', moduleResolution: 'Node10', target: 'ES2020' } },
    ],
  },

  // The default would skip node_modules entirely, faker included.
  transformIgnorePatterns: ['/node_modules/(?!(@faker-js)/)', '\\.pnp\\.[^\\/]+$'],

  setupFilesAfterEnv: ['<rootDir>/dist/api-test/jest.setup.js'],

  reporters: ['default', ['jest-junit', { outputDirectory: '.tmp', outputName: 'e2e-test-report.xml' }]],
};
