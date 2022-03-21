// jest.config.js (located at the project root)

// base config from jest-present-angular
// make sure to add in the required preset and
// and setup file entries
module.exports = {
  preset: 'jest-preset-angular',
  roots: [__dirname + '/src'],
  setupFilesAfterEnv: [__dirname + '/src/setup-jest.ts'],
  restoreMocks: true,
  transformIgnorePatterns: [
    '/node_modules/(?!(@gravitee/ui-components/.*?\\.js)|lit|@lit/reactive-element|(lit-element/.*?\\.js)|(lit-html/.*?\\.js)|(resize-observer-polyfill/.*?\\.js)|(date-fns/.*?\\.js)$)',
  ],
};
