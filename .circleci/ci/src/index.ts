import { changedFiles, isBlank } from './utils';
import { argv } from 'node:process';
import { buildCIPipeline, CircleCIEnvironment } from './pipelines';
import * as fs from 'fs';

const destFile = argv.slice(2).at(0) ?? './dynamicConfig.yml';

const CIRCLE_BRANCH: string | undefined = process.env.CIRCLE_BRANCH;
const CIRCLE_BUILD_NUM: string = process.env.CIRCLE_BUILD_NUM ?? '';
const CIRCLE_SHA1: string = process.env.CIRCLE_SHA1 ?? '';
const CIRCLE_TAG: string | undefined = process.env.CIRCLE_TAG;
const CI_ACTION: string | undefined = process.env.CI_ACTION;
const CI_DRY_RUN: string | undefined = process.env.CI_DRY_RUN;
const CI_GRAVITEEIO_VERSION: string = process.env.CI_GRAVITEEIO_VERSION ?? '';
const GIT_BASE_BRANCH: string = process.env.GIT_BASE_BRANCH ?? 'master';

if (isBlank(CIRCLE_SHA1)) {
  console.error('No CIRCLE_SHA1 defined');
  process.exit(1);
}

const changed = CIRCLE_TAG != null || CIRCLE_BRANCH === 'master' ? Promise.resolve([]) : changedFiles(`origin/${GIT_BASE_BRANCH}`);

changed
  .then((changes) => ({
    branch: CIRCLE_BRANCH ?? CIRCLE_TAG ?? 'unknown',
    buildNum: CIRCLE_BUILD_NUM, // TODO merge this line with the next one when everything is working on the CI
    buildId: CIRCLE_BUILD_NUM,
    sha1: CIRCLE_SHA1,
    action: CI_ACTION ?? 'pull_requests',
    isDryRun: CI_DRY_RUN !== 'false',
    graviteeioVersion: CI_GRAVITEEIO_VERSION,
    changedFiles: changes,
  }))
  .then((environment: CircleCIEnvironment) => buildCIPipeline(environment))
  .then((dynamicConfig) => {
    if (dynamicConfig !== null) {
      const yaml = dynamicConfig.stringify();

      fs.writeFile(destFile, yaml, (err) => {
        if (err) {
          console.error('💥', err);
          return;
        }
      });
    }
  })
  .catch((error) => {
    console.error(error);
    process.exit(1);
  });
