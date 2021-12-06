#!/usr/bin/env zx

const outputPath = "/tmp/parameters.json";

const isOnMasterOrSupportBranch = process.env.CIRCLE_BRANCH === "master" || process.env.CIRCLE_BRANCH.match(/\d+\.\d+\.x/);
if (isOnMasterOrSupportBranch) {
    await fs.outputJson(outputPath, {});
}

let circleCiPR = process.env.CIRCLE_PULL_REQUEST;

if (!circleCiPR) {
    if (process.env.CIRCLECI) {
        $`circleci step halt`;
    }

    process.exit(0);
}

const cleanedPRId = circleCiPR.substring(circleCiPR.lastIndexOf("/") + 1);

const url = `https://api.github.com/repos/gravitee-io/gravitee-api-management/pulls/${cleanedPRId}`;

const response = await fetch(url);
if (!response.ok) {
    // TODO: log error?
    console.log(await response.text());
}

const body = await response.json();
const targetBranch = body.base.ref;

console.log(`Target branch is ${targetBranch}`);

const modifiedFilesAsString = await $`git diff --name-only origin/${targetBranch}`;
const modifiedFiles = modifiedFilesAsString.stdout.split("\n");

// Portal
const shouldBuildPortal = modifiedFiles.some((file) => file.includes("gravitee-apim-portal"));

// Console
const shouldBuildConsole = modifiedFiles.some((file) => file.includes("gravitee-apim-console"));

// All maven projects
const mavenProjectsIdentifiers = [
    "gravitee-apim-gateway",
    "gravitee-apim-repository",
    "gravitee-apim-rest-api",
    "gravitee-apim-distribution",
];
const shouldBuildMavenProjects = modifiedFiles.some((file) => mavenProjectsIdentifiers.some((identifier) => file.includes(identifier)));

const baseDepsIdentifiers = [/^.circleci/, /^pom.xml$/, /^.gitignore$/, /^.prettierrc$/];
const shouldBuildAll = modifiedFiles.some((file) => baseDepsIdentifiers.some((identifier) => identifier.test(file)));

const parametersTemplate = {
    build_maven: shouldBuildMavenProjects || shouldBuildAll,
    build_portal: shouldBuildPortal || shouldBuildAll,
    build_console: shouldBuildConsole || shouldBuildAll,
};

console.log(chalk.blue(`Build will be executed with the following parameters:`));
console.dir(parametersTemplate);
await fs.outputJson(outputPath, parametersTemplate);
