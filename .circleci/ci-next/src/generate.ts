/**
 * CLI entry point — generates the dynamic CircleCI config YAML.
 *
 * Usage: tsx src/generate.ts [output-file]
 *
 * Reads CircleCI environment variables and writes the generated
 * pipeline configuration to the given file (defaults to ./dynamicConfig.yml).
 */
import { argv } from 'node:process';
import * as fs from 'node:fs';
import { isSupportBranchOrMaster } from './config/branch-utils.js';
import { changedFiles } from './utils/git.js';
import { generatePullRequestsConfig } from './workflows/pull-requests.js';
import type { CircleCIEnvironment } from './config/environment.js';

const destFile = argv.slice(2).at(0) ?? './dynamicConfig.yml';

const CIRCLE_BRANCH = process.env.CIRCLE_BRANCH ?? '';
const CIRCLE_BUILD_NUM = process.env.CIRCLE_BUILD_NUM ?? '';
const CIRCLE_SHA1 = process.env.CIRCLE_SHA1 ?? '';
const CI_ACTION = process.env.CI_ACTION ?? 'pull_requests';
const CI_DRY_RUN = process.env.CI_DRY_RUN;
const CI_GRAVITEEIO_VERSION = process.env.CI_GRAVITEEIO_VERSION ?? '';
const CI_DOCKER_TAG_AS_LATEST = process.env.CI_DOCKER_TAG_AS_LATEST;
const GIT_BASE_BRANCH = process.env.GIT_BASE_BRANCH ?? 'master';
const GIT_COMMON_COMMIT_HASH = process.env.GIT_COMMON_COMMIT_HASH ?? '';
const APIM_VERSION_PATH = process.env.APIM_VERSION_PATH ?? '/home/circleci/project/pom.xml';

if (!CIRCLE_SHA1) {
  console.error('No CIRCLE_SHA1 defined');
  process.exit(1);
}

const changed = isSupportBranchOrMaster(CIRCLE_BRANCH)
  ? Promise.resolve([])
  : changedFiles(GIT_COMMON_COMMIT_HASH || GIT_BASE_BRANCH);

changed
  .then((changes): CircleCIEnvironment => ({
    baseBranch: GIT_BASE_BRANCH,
    branch: CIRCLE_BRANCH,
    buildNum: CIRCLE_BUILD_NUM,
    buildId: CIRCLE_BUILD_NUM,
    sha1: CIRCLE_SHA1,
    action: CI_ACTION,
    isDryRun: CI_DRY_RUN !== 'false',
    graviteeioVersion: CI_GRAVITEEIO_VERSION,
    changedFiles: changes,
    apimVersionPath: APIM_VERSION_PATH,
    dockerTagAsLatest: CI_DOCKER_TAG_AS_LATEST === 'true',
  }))
  .then((environment) => {
    // For now only pull_requests is supported in ci-next
    if (environment.action !== 'pull_requests') {
      console.error(`Action "${environment.action}" is not yet supported by ci-next`);
      process.exit(1);
    }
    return generatePullRequestsConfig(environment);
  })
  .then((cfg) => {
    const yaml = cfg.toYAML();
    fs.writeFileSync(destFile, yaml);
    console.log(`Config written to ${destFile}`);
  })
  .catch((error) => {
    console.error(error);
    process.exit(1);
  });
