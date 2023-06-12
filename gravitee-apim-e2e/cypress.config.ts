import { defineConfig } from 'cypress';

export default defineConfig({
  env: {
    managementApi: 'http://localhost:8083',
  },
  e2e: {
    baseUrl: 'http://localhost:3000',
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
