module.exports = {
  preset: 'jest-preset-angular',
  setupFilesAfterEnv: ['<rootDir>/setup-jest.ts'],
  transformIgnorePatterns: ['/node_modules/(?!(.*\\.mjs$)|(marked-extended-tables)|(@asciidoctor/.*?\\.js)$)'],
  moduleNameMapper: {
    '^@gravitee/gravitee-markdown$': '<rootDir>/projects/gravitee-markdown/src/public-api.ts',
  },
  modulePathIgnorePatterns: ['<rootDir>/dist/'],
};
