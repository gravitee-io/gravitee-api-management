module.exports = {
  preset: 'jest-preset-angular',
  setupFilesAfterEnv: ['<rootDir>/setup-jest.ts'],
  transformIgnorePatterns: ['/node_modules/(?!(.*\\.mjs$)|(marked-extended-tables)|(@asciidoctor/.*?\\.js)$)'],
};
