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
import { Config } from '../circleci-config';
import { ReleaseWorkflow } from '../workflows';
import { CircleCIEnvironment } from './circleci-environment';
import { isHotfixBranch, isSupportBranch } from '../utils';
import { initDynamicConfig } from './config-factory';

const HOTFIX_QUALIFIER = /-hotfix\.\d+$/;

/**
 * The version and the branch reach this pipeline as two independent inputs, and nothing downstream
 * brings them together: the published version comes from the poms of whichever branch runs, while
 * the tag comes from the version. A mismatch therefore tags one version and publishes another,
 * without failing. A hotfix is where the two diverge most easily, since `hotfix/4.12.17` and
 * `4.12.17-hotfix.1` name the same release twice.
 */
function assertVersionMatchesBranch(version: string, branch: string): void {
  if (isHotfixBranch(branch)) {
    const releasedVersion = branch.substring('hotfix/'.length);
    if (!version.startsWith(`${releasedVersion}-`)) {
      throw new Error(`${branch} releases ${releasedVersion} with a qualifier, not ${version}`);
    }
  } else if (HOTFIX_QUALIFIER.test(version)) {
    throw new Error(`${version} is only released from hotfix/${version.replace(HOTFIX_QUALIFIER, '')}, not from ${branch}`);
  }
}

export function generateReleaseConfig(environment: CircleCIEnvironment): Config {
  if (!isSupportBranch(environment.branch) && !isHotfixBranch(environment.branch)) {
    throw new Error('Release is only supported on a support branch (X.Y.x) or a hotfix branch (hotfix/X.Y.Z)');
  }

  assertVersionMatchesBranch(environment.graviteeioVersion, environment.branch);

  const dynamicConfig = initDynamicConfig();
  const workflow = ReleaseWorkflow.create(dynamicConfig, environment);
  dynamicConfig.addWorkflow(workflow);
  return dynamicConfig;
}
