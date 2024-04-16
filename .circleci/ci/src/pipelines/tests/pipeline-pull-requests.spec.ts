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
    branchName                      | changedFiles                           | expectedFileName
    ${'master'}                     | ${[]}                                  | ${'pull-requests-master.yml'}
    ${'4.1.x'}                      | ${[]}                                  | ${'pull-requests-4-1-x.yml'}
    ${'APIM-1234-run-e2e'}          | ${[]}                                  | ${'pull-requests-run-e2e.yml'}
    ${'mergify/bp/4.0.x/pr-1234'}   | ${['pom.xml']}                         | ${'pull-requests-mergify.yml'}
    ${'APIM-1234-my-custom-branch'} | ${['pom.xml']}                         | ${'pull-requests-custom-branch.yml'}
    ${'APIM-1234-my-custom-branch'} | ${['helm']}                            | ${'pull-requests-custom-branch-helm-only.yml'}
    ${'APIM-1234-my-custom-branch'} | ${['gravitee-apim-console-webui']}     | ${'pull-requests-custom-branch-console-only.yml'}
    ${'APIM-1234-my-custom-branch'} | ${['gravitee-apim-portal-webui']}      | ${'pull-requests-custom-branch-portal-only.yml'}
    ${'APIM-1234-my-custom-branch'} | ${['gravitee-apim-definition']}        | ${'pull-requests-custom-branch-backend-only.yml'}
    ${'APIM-1234-my-custom-branch'} | ${['gravitee-apim-repository']}        | ${'pull-requests-custom-branch-backend-only.yml'}
    ${'APIM-1234-my-custom-branch'} | ${['gravitee-apim-distribution']}      | ${'pull-requests-custom-branch-backend-distribution-only.yml'}
    ${'APIM-1234-my-custom-branch'} | ${['gravitee-apim-gateway']}           | ${'pull-requests-custom-branch-backend-gateway-only.yml'}
    ${'APIM-1234-my-custom-branch'} | ${['gravitee-apim-integration-tests']} | ${'pull-requests-custom-branch-backend-integration-tests-only.yml'}
    ${'APIM-1234-my-custom-branch'} | ${['gravitee-apim-plugin']}            | ${'pull-requests-custom-branch-backend-plugin-only.yml'}
    ${'APIM-1234-my-custom-branch'} | ${['gravitee-apim-rest-api']}          | ${'pull-requests-custom-branch-backend-rest-api-only.yml'}
  `(
    'should generate pull-requests config for branch $branchName with changedFiles $changedFiles',
    ({ branchName, changedFiles, expectedFileName }) => {
      const result = generatePullRequestsConfig({
        action: 'pull_requests',
        apimVersionPath: './src/pipelines/tests/resources/common/pom.xml',
        branch: branchName,
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
