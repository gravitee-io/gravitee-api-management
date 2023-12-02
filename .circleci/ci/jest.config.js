module.exports = {
    testEnvironment: "node",
    transform: {
        "^.+\\.ts$": "@sucrase/jest-plugin",
    },
    testMatch: ["<rootDir>/**/*.spec.ts"],
    verbose: true,
};