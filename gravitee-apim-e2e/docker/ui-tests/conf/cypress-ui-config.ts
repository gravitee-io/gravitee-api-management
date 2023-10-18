import { defineConfig } from "cypress";
import { unlinkSync } from "fs";

export default defineConfig({
    projectId: "7tvhwd",
    env: {
        failOnStatusCode: false,
        api_publisher_user_login: "api1",
        api_publisher_user_password: "api1",
        application_user_login: "application1",
        application_user_password: "application1",
        low_permission_user_login: "user",
        low_permission_user_password: "password",
        admin_user_login: "admin",
        admin_user_password: "admin",
        defaultOrg: "/management/organizations/DEFAULT",
        defaultOrgEnv: "/management/organizations/DEFAULT/environments/DEFAULT",
        managementApi: 'http://nginx',
        gatewayServer: "http://nginx/gateway",
        portalApi: "/portal/environments/DEFAULT",
        wiremockUrl: 'http://wiremock:8080',
    },
    e2e: {
        specPattern: "./ui-test/integration/**/*.spec.ts",
        viewportWidth: 1920,
        viewportHeight: 1080,
        fixturesFolder: "./ui-test/fixtures",
        screenshotsFolder: "./ui-test/screenshots",
        supportFile: "./ui-test/support/e2e.ts",
        videosFolder: "./ui-test/videos",
        video: true,
        videoCompression: 10,
        screenshotOnRunFailure: true,
        baseUrl: "http://nginx/console",
        setupNodeEvents(on, config) {
            // Delete videos for successful tests
            on('after:spec', (spec, results) => {
                if (results && results.video) {
                    // Do we have failures for any retry attempts?
                    const failures = results.tests && results.tests.some((test) =>
                      test.attempts.some((attempt) => attempt.state === 'failed')
                    )
                    if (!failures) {
                        // delete the video if the spec passed and no tests retried
                        unlinkSync(results.video)
                    }
                }
            })
        },
    },
});