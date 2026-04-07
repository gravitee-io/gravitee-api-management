module.exports = {
  testTimeout: 30000,
  workerIdleMemoryLimit: '512MB',
  preset: 'jest-preset-angular',
  roots: [__dirname + '/src'],
  setupFiles: ['jest-canvas-mock'],
  setupFilesAfterEnv: [__dirname + '/setup-jest.ts'],
  modulePathIgnorePatterns: ['<rootDir>/dist/'],
  transformIgnorePatterns: ['/node_modules/(?!(.*\\.mjs$)|lodash-es|marked-extended-tables|(@asciidoctor/.*?\\.js)$)'],
  collectCoverageFrom: [
    'src/**/*.{ts,html}',
    '!src/**/*.spec.{ts,tsx}',
    '!src/**/*.module.ts',
    '!src/main.ts',
    '!src/**/index.ts',
    '!src/environments/**',
    '!src/**/*.stories.ts',
    '!src/**/__mocks__/**',
    '!src/**/*.harness.ts',
  ],
  coverageDirectory: __dirname + '/coverage',
  moduleNameMapper: {
    '^@gravitee/gravitee-dashboard$': '<rootDir>/../gravitee-apim-webui-libs/gravitee-dashboard/src/public-api.ts',
    '^@gravitee/gravitee-markdown$': '<rootDir>/../gravitee-apim-webui-libs/gravitee-markdown/src/public-api.ts',
    '^chartjs-adapter-date-fns$': '<rootDir>/__mocks__/chartjs-adapter-date-fns.js',
  },
  reporters: [
    'default',
    [
      'jest-junit',
      {
        outputDirectory: __dirname + '/coverage',
        outputName: 'junit.xml',
        addFileAttribute: 'true',
      },
    ],
  ],
};
