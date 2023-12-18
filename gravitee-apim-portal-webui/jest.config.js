// jest.config.js (located at the project root)

// base config from jest-present-angular
// make sure to add in the required preset and
// and setup file entries
module.exports = {
  roots: [__dirname + '/src'],
  setupFilesAfterEnv: [__dirname + '/src/setup-jest.ts'],
  restoreMocks: true,
  transformIgnorePatterns: [
    '/node_modules/(?!(.*\\.mjs$)|(@gravitee/ui-components/.*?\\.js)|github-slugger|lit|@lit/reactive-element|(lit-element/.*?\\.js)|(resize-observer-polyfill/.*?\\.js)|(date-fns/.*?\\.js)$)',
  ],
  transform: {
    '\\.js$': 'babel-jest',
    '\\.mjs$': 'babel-jest',
  },
};
