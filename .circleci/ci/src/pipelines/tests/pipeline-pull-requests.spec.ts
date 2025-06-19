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
import { generatePullRequestsConfig } from '../pipeline-pull-requests';

describe('Pull requests workflow tests', () => {
  it.each`
    baseBranch  | branch                          | changedFiles                           | expectedFileName
    ${'master'} | ${'master'}                     | ${['pom.xml']}                         | ${'pull-requests-master.yml'}
  `(
    'should generate pull-requests config for branch $branchName with changedFiles $changedFiles',
    ({ baseBranch, branch, changedFiles, expectedFileName }) => {
      const result = generatePullRequestsConfig({
        action: 'pull_requests',
        apimVersionPath: './src/pipelines/tests/resources/common/pom.xml',
        baseBranch,
        branch,
        sha1: '784ff35ca',
        changedFiles: changedFiles,
        buildNum: '1234',
        buildId: '1234',
        graviteeioVersion: '',
        isDryRun: false,
      });

      const expected = fs.readFileSync(`./src/pipelines/tests/resources/pull-requests/${expectedFileName}`, 'utf-8');
      expect(result.stringify()).toStrictEqual(expected);
    },
  );
});
