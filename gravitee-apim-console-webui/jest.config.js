process.env.TZ = 'UTC';

module.exports = {
  preset: 'jest-preset-angular',
  setupFilesAfterEnv: [__dirname + '/src/setup-jest.ts'],
  transformIgnorePatterns: [
    '/node_modules/(?!(.*\\.mjs$)|(@gravitee/ui-components/.*?\\.js)|lit|@lit/reactive-element|(lit-element/.*?\\.js)|(lit-html/.*?\\.js)|(resize-observer-polyfill/.*?\\.js)|(date-fns/.*?\\.js)$)',
  ],
  moduleNameMapper: {
    '^html-loader!.*\\.html$': '<rootDir>/src/__mocks__/htmlLoaderMock.js',
  },
};
