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
import { BACKEND_SUITES } from '../../workflows/groups/analysed-projects';

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

  // Both consumers derive from `analysed-projects.ts`, so the equality below cannot drift on its
  // own. What earns its keep is the count: a project dropped from the shared list leaves the
  // nightly and every pull request at once, which is the failure this epic exists to prevent.
  //
  // The suites with no analysis need their own assertion: comparing Sonar jobs cannot see them,
  // which is how kafka-explorer and gamma silently stopped running at night once they were split
  // into jobs of their own.
  it('should run and analyse exactly what a pull request runs and analyses', () => {
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

    // A pull request runs the suites it touches; the nightly build runs every one of them.
    const unanalysedSuiteNames = BACKEND_SUITES.filter((suite) => !suite.sonar).map((suite) => suite.suiteName);
    expect(unanalysedSuiteNames).toEqual(
      expect.arrayContaining(['Test kafka-explorer', 'Test gamma', 'Test gamma UI', 'Integration tests']),
    );
    unanalysedSuiteNames.forEach((name) => expect(nightly).toContain(`name: ${name}`));
  });
});
