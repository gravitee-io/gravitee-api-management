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
import { generateReleaseConfig } from '../pipeline-release';

describe('Release tests', () => {
  it.each`
    baseBranch | branch     | isDryRun | apimVersionPath                                              | graviteeioVersion  | expectedResult
    ${'4.2.x'} | ${'4.2.x'} | ${true}  | ${'./src/pipelines/tests/resources/common/pom-snapshot.xml'} | ${'4.2.0'}         | ${'release-4-2-0-dry-run.yml'}
    ${'4.2.x'} | ${'4.2.x'} | ${false} | ${'./src/pipelines/tests/resources/common/pom-snapshot.xml'} | ${'4.2.0'}         | ${'release-4-2-0.yml'}
    ${'4.2.x'} | ${'4.2.x'} | ${false} | ${'./src/pipelines/tests/resources/common/pom-alpha.xml'}    | ${'4.2.0-alpha.1'} | ${'release-4-2-0-alpha.yml'}
  `(
    'should build release config on $branch with dry run $isDryRun and graviteeio version $graviteeioVersion',
    ({ baseBranch, branch, isDryRun, apimVersionPath, graviteeioVersion, expectedResult }) => {
      const result = generateReleaseConfig({
        action: 'release',
        sha1: '784ff35ca',
        changedFiles: [],
        buildNum: '1234',
        buildId: '1234',
        graviteeioVersion,
        baseBranch,
        branch,
        apimVersionPath,
        isDryRun,
      });

      const expected = fs.readFileSync(`./src/pipelines/tests/resources/release/${expectedResult}`, 'utf-8');
      expect(result.stringify()).toStrictEqual(expected);
    },
  );

  it('should throw error when branch is neither a support nor a hotfix branch', () => {
    expect.assertions(1);

    try {
      generateReleaseConfig({
        action: 'release',
        sha1: '784ff35ca',
        changedFiles: [],
        buildNum: '1234',
        buildId: '1234',
        graviteeioVersion: '4.1.0',
        baseBranch: 'master',
        branch: 'apim-1234-dev',
        apimVersionPath: '/some/path',
        isDryRun: false,
      });
    } catch (e) {
      expect(e).toStrictEqual(new Error('Release is only supported on a support branch (X.Y.x) or a hotfix branch (hotfix/X.Y.Z)'));
    }
  });

  it('should build release config on a hotfix branch cut from a released tag', () => {
    const result = generateReleaseConfig({
      action: 'release',
      sha1: '784ff35ca',
      changedFiles: [],
      buildNum: '1234',
      buildId: '1234',
      graviteeioVersion: '4.2.0-hotfix.1',
      baseBranch: '4.2.x',
      branch: 'hotfix/4.2.0',
      apimVersionPath: './src/pipelines/tests/resources/common/pom-snapshot.xml',
      isDryRun: false,
    });

    const expected = fs.readFileSync('./src/pipelines/tests/resources/release/release-4-2-0-hotfix.yml', 'utf-8');
    expect(result.stringify()).toStrictEqual(expected);
  });

  it.each`
    graviteeioVersion   | branch            | expectedError
    ${'4.2.0'}          | ${'hotfix/4.2.0'} | ${'hotfix/4.2.0 releases 4.2.0 with a qualifier, not 4.2.0'}
    ${'4.2.1-hotfix.1'} | ${'hotfix/4.2.0'} | ${'hotfix/4.2.0 releases 4.2.0 with a qualifier, not 4.2.1-hotfix.1'}
    ${'4.2.0-hotfix.1'} | ${'4.2.x'}        | ${'4.2.0-hotfix.1 is only released from hotfix/4.2.0, not from 4.2.x'}
  `('should throw for version $graviteeioVersion on branch $branch', ({ graviteeioVersion, branch, expectedError }) => {
    expect(() =>
      generateReleaseConfig({
        action: 'release',
        sha1: '784ff35ca',
        changedFiles: [],
        buildNum: '1234',
        buildId: '1234',
        graviteeioVersion,
        baseBranch: '4.2.x',
        branch,
        apimVersionPath: './src/pipelines/tests/resources/common/pom-snapshot.xml',
        isDryRun: false,
      }),
    ).toThrow(expectedError);
  });
});
