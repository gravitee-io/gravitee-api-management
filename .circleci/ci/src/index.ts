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
import {
  changedFiles,
  coreVersionFromTag,
  diffRef,
  distributionVersionFromTag,
  isBlank,
  isLatestRelease,
  isSupportBranchOrMaster,
  remoteTags,
} from './utils';
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
const GIT_BASE_BRANCH: string = process.env.GIT_BASE_BRANCH ?? 'master';
const GIT_COMMON_COMMIT_HASH: string = process.env.GIT_COMMON_COMMIT_HASH ?? '';
const APIM_VERSION_PATH: string | undefined = process.env.APIM_VERSION_PATH;
const CIRCLE_TAG: string = process.env.CIRCLE_TAG ?? '';

if (isBlank(CIRCLE_SHA1)) {
  console.error('No CIRCLE_SHA1 defined');
  process.exit(1);
}

/**
 * A core release tag carries everything the lane needs: which pipeline to build, and the version to
 * publish. Nothing is passed as a pipeline parameter on that path, so the artefacts come from the
 * tagged tree rather than from a branch head that has moved on since.
 */
const coreReleaseVersion = coreVersionFromTag(CIRCLE_TAG);

/**
 * The distribution releases under the bare version. Same principle as above: the tag carries the
 * version, so nothing is passed as a pipeline parameter and the artefacts come from the tagged tree.
 */
const distributionReleaseVersion = distributionVersionFromTag(CIRCLE_TAG);

const releasedByTag = coreReleaseVersion ?? distributionReleaseVersion;

/** Which lane the pipeline is generating for. A tag decides it; off a tag, the parameter does. */
const action =
  coreReleaseVersion !== undefined
    ? 'core_release'
    : distributionReleaseVersion !== undefined
      ? 'distribution_release'
      : (CI_ACTION ?? 'pull_requests');

/**
 * Which images take the `latest` tag. It used to be a pipeline parameter, answered from memory; a
 * tag-triggered pipeline receives none, so it is read from what has already been released instead.
 *
 * Only this lane ever asks. The `latest` tag is pushed on the release path alone — every other
 * workflow builds images with `isProd` false — so anything that is not a distribution tag answers
 * no without looking.
 */
const dockerTagAsLatest =
  distributionReleaseVersion === undefined
    ? Promise.resolve(false)
    : remoteTags().then((tags) => isLatestRelease(distributionReleaseVersion, tags));

/**
 * The pipeline generation is available according to different conditions:
 *     - if the branch is supported ( CIRCLE_BRANCH is master or a support branch )
 *     - if we are working on a branch with changes committed on the base branch
 */
const changed =
  releasedByTag !== undefined || isSupportBranchOrMaster(CIRCLE_BRANCH)
    ? Promise.resolve([])
    : changedFiles(diffRef(GIT_COMMON_COMMIT_HASH, GIT_BASE_BRANCH));

Promise.all([changed, dockerTagAsLatest])
  .then(
    ([changes, tagAsLatest]) =>
      ({
        baseBranch: GIT_BASE_BRANCH,
        branch: CIRCLE_BRANCH,
        buildNum: CIRCLE_BUILD_NUM, // TODO merge this line with the next one when everything is working on the CI
        buildId: CIRCLE_BUILD_NUM,
        sha1: CIRCLE_SHA1,
        action,
        tag: CIRCLE_TAG === '' ? undefined : CIRCLE_TAG,
        // A tag is never a rehearsal. `dry_run` defaults to true and no parameter reaches this path,
        // so without this a pushed tag would publish nothing and go green. Rehearsals keep the API
        // trigger, which is where the flag belongs.
        isDryRun: releasedByTag !== undefined ? false : CI_DRY_RUN !== 'false',
        graviteeioVersion: releasedByTag ?? CI_GRAVITEEIO_VERSION,
        changedFiles: changes,
        apimVersionPath: APIM_VERSION_PATH ?? '/home/circleci/project/pom.xml',
        dockerTagAsLatest: tagAsLatest,
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
