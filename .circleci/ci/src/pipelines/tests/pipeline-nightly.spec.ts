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
import * as fs from 'fs';
import { generateNightlyConfig } from '../pipeline-nightly';
import { generatePullRequestsConfig } from '../pipeline-pull-requests';

describe('Nightly', () => {
  it('should generate the nightly pipeline', () => {
    const result = generateNightlyConfig({
      action: 'nightly',
      baseBranch: 'master',
      branch: 'master',
      sha1: '784ff35ca',
      changedFiles: [],
      buildNum: '1234',
      buildId: '1234',
      graviteeioVersion: '4.2.0',
      isDryRun: false,
      apimVersionPath: './src/pipelines/tests/resources/common/pom-snapshot.xml',
    });

    const expected = fs.readFileSync(`./src/pipelines/tests/resources/nightly/nightly.yml`, 'utf-8');
    expect(result.stringify()).toStrictEqual(expected);
  });

  // The nightly spells out its own analyses, so nothing stops the two lists from drifting: a
  // seventh module added to the pull-request groups would never be analysed on the reference
  // branch, and the snapshot above would accept it.
  it('should analyse exactly the projects a pull request analyses', () => {
    const environment = {
      baseBranch: 'master',
      branch: 'master',
      sha1: '784ff35ca',
      changedFiles: [],
      buildNum: '1234',
      buildId: '1234',
      graviteeioVersion: '4.2.0',
      isDryRun: false,
      apimVersionPath: './src/pipelines/tests/resources/common/pom-snapshot.xml',
    };

    const sonarJobs = (yaml: string): string[] => [...yaml.matchAll(/name: (Sonar - [\w-]+)/g)].map((match) => match[1]).sort();

    const nightly = generateNightlyConfig({ ...environment, action: 'nightly' }).stringify();
    const pullRequest = generatePullRequestsConfig({
      ...environment,
      action: 'pull_requests',
      branch: 'APIM-1234-my-custom-branch',
      changedFiles: ['pom.xml'],
    }).stringify();

    expect(sonarJobs(nightly)).toEqual(sonarJobs(pullRequest));
    expect(sonarJobs(nightly)).toHaveLength(9);
  });
});
