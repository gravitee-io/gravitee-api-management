import { defineConfig } from "cypress";

export default defineConfig({
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
        managementOrganizationApi: "/management/organizations/DEFAULT",
        managementApi: "/management/organizations/DEFAULT/environments/DEFAULT",
        managementUI: "http://nginx/console",
        gatewayServer: "http://nginx/gateway",
        portalApi: "/portal/environments/DEFAULT",
    },
    e2e: {
        projectId: "ui-test",
        specPattern: "./ui-test/integration/**/*.spec.ts",
        viewportWidth: 1920,
        viewportHeight: 1080,
        fixturesFolder: "./ui-test/fixtures",
        screenshotsFolder: "./ui-test/screenshots",
        supportFile: "./ui-test/support/e2e.ts",
        videosFolder: "./ui-test/videos",
        video: false,
        screenshotOnRunFailure: false,
        baseUrl: "http://nginx",
        setupNodeEvents(on, config) {
            // implement node event listeners here
        },
    },
});