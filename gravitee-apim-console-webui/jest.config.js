process.env.TZ = 'UTC';

module.exports = {
  preset: 'jest-preset-angular',
  setupFilesAfterEnv: [__dirname + '/src/setup-jest.ts'],
  globalSetup: 'jest-preset-angular/global-setup',
  transformIgnorePatterns: [
    '/node_modules/(?!(.*\\.mjs$)|(@gravitee/ui-components/.*?\\.js)|lit|@lit/reactive-element|(lit-element/.*?\\.js)|(lit-html/.*?\\.js)|(resize-observer-polyfill/.*?\\.js)|(date-fns/.*?\\.js)$)',
  ],
};
