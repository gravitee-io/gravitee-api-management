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
import { generateReleaseHelmConfig } from '../pipeline-release-helm';

describe('Release helm charts tests', () => {
  it.each`
    isDryRun | expectedResult
    ${false} | ${'release-helm.yml'}
    ${true}  | ${'release-helm-dry-run.yml'}
  `('should build release helm chart with dry run $isDryRun', ({ isDryRun, expectedResult }) => {
    const result = generateReleaseHelmConfig({
      action: 'release_helm',
      sha1: '784ff35ca',
      changedFiles: [],
      buildNum: '1234',
      buildId: '1234',
      graviteeioVersion: '4.2.0',
      apimVersionPath: './src/pipelines/tests/resources/common/pom-snapshot.xml',
      baseBranch: 'master',
      branch: 'master',
      isDryRun,
    });

    const expected = fs.readFileSync(`./src/pipelines/tests/resources/release-helm/${expectedResult}`, 'utf-8');
    expect(result.stringify()).toStrictEqual(expected);
  });
});
