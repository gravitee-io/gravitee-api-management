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
import { generateFullReleaseConfig } from '../pipeline-full-release';

describe('Full release tests', () => {
  it.each`
    baseBranch | branch     | isDryRun | dockerTagAsLatest | graviteeioVersion  | apimVersionPath                                           | expectedResult
    ${'4.2.x'} | ${'4.2.x'} | ${true}  | ${false}          | ${'4.2.0'}         | ${'./src/pipelines/tests/resources/common/pom.xml'}       | ${'release-4-2-0-dry-run.yml'}
    ${'4.2.x'} | ${'4.2.x'} | ${false} | ${false}          | ${'4.2.0'}         | ${'./src/pipelines/tests/resources/common/pom.xml'}       | ${'release-4-2-0-no-dry-run.yml'}
    ${'4.2.x'} | ${'4.2.x'} | ${false} | ${true}           | ${'4.2.0'}         | ${'./src/pipelines/tests/resources/common/pom.xml'}       | ${'release-4-2-0-latest.yml'}
    ${'4.2.x'} | ${'4.2.x'} | ${false} | ${false}          | ${'4.2.0-alpha.1'} | ${'./src/pipelines/tests/resources/common/pom-alpha.xml'} | ${'release-4-2-0-alpha.yml'}
  `(
    'should build full release config on $branch with dry run $isDryRun, is latest $dockerTagAsLatest and version graviteeioVersion',
    ({ baseBranch, branch, isDryRun, dockerTagAsLatest, graviteeioVersion, apimVersionPath, expectedResult }) => {
      const result = generateFullReleaseConfig({
        action: 'release',
        sha1: '784ff35ca',
        changedFiles: [],
        buildNum: '1234',
        buildId: '1234',
        apimVersionPath,
        graviteeioVersion,
        baseBranch,
        branch,
        isDryRun,
        dockerTagAsLatest,
      });

      const expected = fs.readFileSync(`./src/pipelines/tests/resources/full-release/${expectedResult}`, 'utf-8');
      expect(result.stringify()).toStrictEqual(expected);
    },
  );

  it('should throw error when branch is not a support branch', () => {
    expect.assertions(1);

    try {
      generateFullReleaseConfig({
        action: 'release',
        sha1: '784ff35ca',
        changedFiles: [],
        buildNum: '1234',
        buildId: '1234',
        graviteeioVersion: '4.1.0',
        branch: 'apim-1234-dev',
        baseBranch: 'master',
        isDryRun: false,
        apimVersionPath: './src/pipelines/tests/resources/common/pom.xml',
      });
    } catch (e) {
      expect(e).toStrictEqual(new Error('Full release is only supported on support branches'));
    }
  });
});
