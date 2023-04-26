import { defineConfig } from 'cypress';

export default defineConfig({
  e2e: {
    baseUrl: 'http://localhost:8083',
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
