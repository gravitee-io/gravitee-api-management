// Root Jest configuration for workspace
// This is used for IntelliJ
module.exports = {
  testMatch: ['<rootDir>/**/src/**/*.spec.ts'],
  projects: [
    '<rootDir>/gravitee-apim-console-webui/jest.config.js',
    '<rootDir>/gravitee-apim-portal-webui-next/jest.config.js',
    '<rootDir>/gravitee-apim-webui-libs/gravitee-dashboard/jest.config.js',
    '<rootDir>/gravitee-apim-webui-libs/gravitee-markdown/jest.config.js',
  ],
};