import { defineConfig } from 'cypress';

export default defineConfig({
  env: {
    managementApi: 'http://localhost:8083',
    wiremockUrl: 'http://wiremock:8080',
    gatewayServer: 'http://localhost:8082',
  },
  e2e: {
    baseUrl: 'http://localhost:4000',
    watchForFileChanges: false,
    projectId: 'ui-test',
    specPattern: 'ui-test/integration/**/*.spec.ts',
    fixturesFolder: 'ui-test/fixtures',
    screenshotsFolder: 'ui-test/screenshots',
    supportFile: 'ui-test/support/e2e.ts',
    videosFolder: 'ui-test/videos',
    video: false,
    screenshotOnRunFailure: false,
    setupNodeEvents(on, config) {},
  },
});
