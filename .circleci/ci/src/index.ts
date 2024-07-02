/*
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import { changedFiles, isBlank, isSupportBranchOrMaster } from './utils';
import { argv } from 'node:process';
import { buildCIPipeline, CircleCIEnvironment } from './pipelines';
import * as fs from 'fs';

const destFile = argv.slice(2).at(0) ?? './dynamicConfig.yml';

const CIRCLE_BRANCH: string | undefined = process.env.CIRCLE_BRANCH ?? '';
const CIRCLE_BUILD_NUM: string = process.env.CIRCLE_BUILD_NUM ?? '';
const CIRCLE_SHA1: string = process.env.CIRCLE_SHA1 ?? '';
const CI_ACTION: string | undefined = process.env.CI_ACTION;
const CI_DRY_RUN: string | undefined = process.env.CI_DRY_RUN;
const CI_GRAVITEEIO_VERSION: string = process.env.CI_GRAVITEEIO_VERSION ?? '';
const CI_DOCKER_TAG_AS_LATEST: string | undefined = process.env.CI_DOCKER_TAG_AS_LATEST;
const GIT_BASE_BRANCH: string = process.env.GIT_BASE_BRANCH ?? 'master';
const APIM_VERSION_PATH: string | undefined = process.env.APIM_VERSION_PATH;

if (isBlank(CIRCLE_SHA1)) {
  console.error('No CIRCLE_SHA1 defined');
  process.exit(1);
}

/**
 * The pipeline generation is available according to different conditions:
 *     - if the branch is supported ( CIRCLE_BRANCH is master or a support branch )
 *     - if we are working on a branch with changes committed on the base branch
 */
const changed = isSupportBranchOrMaster(CIRCLE_BRANCH) ? Promise.resolve([]) : changedFiles(GIT_BASE_BRANCH);

changed
  .then(
    (changes) =>
      ({
        baseBranch: GIT_BASE_BRANCH,
        branch: CIRCLE_BRANCH,
        buildNum: CIRCLE_BUILD_NUM, // TODO merge this line with the next one when everything is working on the CI
        buildId: CIRCLE_BUILD_NUM,
        sha1: CIRCLE_SHA1,
        action: CI_ACTION ?? 'pull_requests',
        isDryRun: CI_DRY_RUN !== 'false',
        graviteeioVersion: CI_GRAVITEEIO_VERSION,
        changedFiles: changes,
        apimVersionPath: APIM_VERSION_PATH ?? '/home/circleci/project/pom.xml',
        dockerTagAsLatest: CI_DOCKER_TAG_AS_LATEST === 'true',
      }) as CircleCIEnvironment,
  )
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
