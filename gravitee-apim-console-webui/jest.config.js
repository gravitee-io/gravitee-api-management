process.env.TZ = 'UTC';

module.exports = {
  testTimeout: 30000,
  preset: 'jest-preset-angular',
  roots: [__dirname + '/src'],
  setupFilesAfterEnv: [__dirname + '/src/setup-jest.ts'],
  collectCoverageFrom: [
    'src/**/*.{ts,html}',
    '!src/**/*.spec.{ts,tsx}',
    '!src/**/*.module.ts',
    '!src/index.ts',
    '!src/**/index.ts',
    '!src/environments/**',
    '!src/**/*.stories.ts',
    '!src/**/__mocks__/**',
    '!src/**/*.harness.ts',
  ],
  coverageDirectory: __dirname + '/coverage',
  transformIgnorePatterns: [
    '/node_modules/(?!(.*\\.mjs$)|(@gravitee/ui-components/.*?\\.js)|lodash-es||lit|@lit/reactive-element|(lit-element/.*?\\.js)|(lit-html/.*?\\.js)|(resize-observer-polyfill/.*?\\.js)|(date-fns/.*?\\.js)$)',
  ],
  moduleNameMapper: {
    '^html-loader!.*\\.html$': '<rootDir>/src/__mocks__/htmlLoaderMock.js',
    '^@gravitee/gravitee-dashboard$': '<rootDir>/../gravitee-apim-webui-libs/gravitee-dashboard/src/public-api.ts',
    '^@gravitee/gravitee-markdown$': '<rootDir>/../gravitee-apim-webui-libs/gravitee-markdown/src/public-api.ts',
    '^@gravitee/gravitee-kafka-explorer$': '<rootDir>/../gravitee-apim-webui-libs/gravitee-kafka-explorer/src/public-api.ts',
  },
  reporters: [
    'default',
    [
      'jest-junit',
      {
        outputDirectory: __dirname + '/coverage',
        outputName: 'junit.xml',
      },
    ],
  ],
};
