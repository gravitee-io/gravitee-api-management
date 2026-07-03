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

/*
 * Spike / go-no-go for the in-house circleci-config model.
 *
 * By mocking `@circleci/circleci-config-sdk` with our internal module, the real
 * pull_requests pipeline generator runs against our model. We then assert that
 * the produced configuration is *semantically* equal (YAML parsed, key order
 * ignored) to the golden fixtures that were generated with the original SDK.
 *
 * The deep (`dist/...`) SDK imports used by the pipeline code are type-only and
 * are erased at runtime, so mocking the package root is enough.
 */
// eslint-disable-next-line @typescript-eslint/no-require-imports -- jest.mock factories are hoisted and cannot use ESM imports
jest.mock('@circleci/circleci-config-sdk', () => require('../index'));

import * as fs from 'fs';
import { parse } from 'yaml';
import { generatePullRequestsConfig } from '../../pipelines/pipeline-pull-requests';
import { CircleCIEnvironment } from '../../pipelines';

interface Case {
  baseBranch: string;
  branch: string;
  changedFiles: string[];
  expectedFileName: string;
}

const cases: Case[] = [
  { baseBranch: 'master', branch: 'master', changedFiles: ['pom.xml'], expectedFileName: 'pull-requests-master.yml' },
  { baseBranch: '4.1.x', branch: '4.1.x', changedFiles: ['pom.xml'], expectedFileName: 'pull-requests-4-1-x.yml' },
  { baseBranch: '4.0.x', branch: 'mergify/bp/4.0.x/pr-1234', changedFiles: ['pom.xml'], expectedFileName: 'pull-requests-mergify.yml' },
  { baseBranch: 'master', branch: 'APIM-1234-run-e2e', changedFiles: ['pom.xml'], expectedFileName: 'pull-requests-run-e2e.yml' },
  {
    baseBranch: 'master',
    branch: 'APIM-1234-my-custom-branch',
    changedFiles: ['pom.xml'],
    expectedFileName: 'pull-requests-custom-branch.yml',
  },
  {
    baseBranch: 'master',
    branch: 'APIM-1234-my-custom-branch',
    changedFiles: ['helm'],
    expectedFileName: 'pull-requests-custom-branch-helm-only.yml',
  },
  {
    baseBranch: 'master',
    branch: 'APIM-1234-my-custom-branch',
    changedFiles: ['gravitee-apim-console-webui'],
    expectedFileName: 'pull-requests-custom-branch-console-only.yml',
  },
  {
    baseBranch: 'master',
    branch: 'APIM-1234-my-custom-branch',
    changedFiles: ['gravitee-gamma'],
    expectedFileName: 'pull-requests-custom-branch-gamma-console-only.yml',
  },
  {
    baseBranch: 'master',
    branch: 'APIM-1234-my-custom-branch',
    changedFiles: ['gravitee-apim-portal-webui'],
    expectedFileName: 'pull-requests-custom-branch-portal-only.yml',
  },
  {
    baseBranch: 'master',
    branch: 'APIM-1234-my-custom-branch',
    changedFiles: ['gravitee-apim-portal-webui-next'],
    expectedFileName: 'pull-requests-custom-branch-portal-next-only.yml',
  },
  {
    baseBranch: 'master',
    branch: 'APIM-1234-my-custom-branch',
    changedFiles: ['gravitee-apim-definition'],
    expectedFileName: 'pull-requests-custom-branch-backend-only.yml',
  },
  {
    baseBranch: 'master',
    branch: 'APIM-1234-my-custom-branch',
    changedFiles: ['gravitee-apim-distribution'],
    expectedFileName: 'pull-requests-custom-branch-backend-distribution-only.yml',
  },
  {
    baseBranch: 'master',
    branch: 'APIM-1234-my-custom-branch',
    changedFiles: ['gravitee-apim-gateway'],
    expectedFileName: 'pull-requests-custom-branch-backend-gateway-only.yml',
  },
  {
    baseBranch: 'master',
    branch: 'APIM-1234-my-custom-branch',
    changedFiles: ['gravitee-apim-integration-tests'],
    expectedFileName: 'pull-requests-custom-branch-backend-integration-tests-only.yml',
  },
  {
    baseBranch: 'master',
    branch: 'APIM-1234-my-custom-branch',
    changedFiles: ['gravitee-apim-plugin'],
    expectedFileName: 'pull-requests-custom-branch-backend-plugin-only.yml',
  },
  {
    baseBranch: 'master',
    branch: 'APIM-1234-my-custom-branch',
    changedFiles: ['gravitee-apim-reporter'],
    expectedFileName: 'pull-requests-custom-branch-backend-reporter-only.yml',
  },
  {
    baseBranch: 'master',
    branch: 'APIM-1234-my-custom-branch',
    changedFiles: ['gravitee-apim-repository'],
    expectedFileName: 'pull-requests-custom-branch-backend-only.yml',
  },
  {
    baseBranch: 'master',
    branch: 'APIM-1234-my-custom-branch',
    changedFiles: ['gravitee-apim-rest-api'],
    expectedFileName: 'pull-requests-custom-branch-backend-rest-api-only.yml',
  },
];

describe('circleci-config model — semantic equivalence on pull_requests', () => {
  it.each(cases)(
    'matches the SDK golden fixture for $expectedFileName ($changedFiles)',
    ({ baseBranch, branch, changedFiles, expectedFileName }) => {
      const environment: CircleCIEnvironment = {
        action: 'pull_requests',
        apimVersionPath: './src/pipelines/tests/resources/common/pom.xml',
        baseBranch,
        branch,
        sha1: '784ff35ca',
        changedFiles,
        buildNum: '1234',
        buildId: '1234',
        graviteeioVersion: '',
        isDryRun: false,
      };

      const generated = parse(generatePullRequestsConfig(environment).stringify());
      const expected = parse(fs.readFileSync(`./src/pipelines/tests/resources/pull-requests/${expectedFileName}`, 'utf-8'));

      expect(generated).toStrictEqual(expected);
    },
  );
});
