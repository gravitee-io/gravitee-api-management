module.exports = {
  preset: 'jest-preset-angular',
  setupFiles: ['jest-canvas-mock'],
  setupFilesAfterEnv: ['<rootDir>/setup-jest.ts'],
  modulePathIgnorePatterns: ['<rootDir>/dist/'],
  transformIgnorePatterns: ['/node_modules/(?!(.*\\.mjs$)|lodash-es|marked-extended-tables|(@asciidoctor/.*?\\.js)$)'],
  moduleNameMapper: {
    '^@gravitee/gravitee-dashboard$': '<rootDir>/projects/gravitee-dashboard/src/public-api.ts',
    '^chartjs-adapter-date-fns$': '<rootDir>/__mocks__/chartjs-adapter-date-fns.js',
  },
};
