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
import { Config, workflow } from '../../circleci-config';
import { E2ECypressJob, E2EGenerateSDKJob, E2ELintBuildJob, E2ETestJob } from '../../jobs';
import { CircleCIEnvironment } from '../../pipelines';
import { config } from '../../config';

/**
 * The end-to-end suites: SDK generation, the e2e project build, the matrix run and Cypress.
 *
 * These depend on the backend images but do not build them — see `backendImageJobs`. A caller
 * wanting e2e must contribute both groups, and must contribute the images first so the job
 * declaration order stays stable.
 */
export function e2eJobs(dynamicConfig: Config, environment: CircleCIEnvironment): workflow.WorkflowJob[] {
  const e2eGenerateSdkJob = E2EGenerateSDKJob.create(dynamicConfig, environment);
  dynamicConfig.addJob(e2eGenerateSdkJob);

  const e2eLintBuildJob = E2ELintBuildJob.create(dynamicConfig, environment);
  dynamicConfig.addJob(e2eLintBuildJob);

  const e2eTestJob = E2ETestJob.create(dynamicConfig, environment);
  dynamicConfig.addJob(e2eTestJob);

  const e2eCypressJob = E2ECypressJob.create(dynamicConfig, environment);
  dynamicConfig.addJob(e2eCypressJob);

  return [
    new workflow.WorkflowJob(e2eGenerateSdkJob, {
      context: config.jobContext,
      name: 'Generate e2e tests SDK',
      requires: ['Build backend'],
    }),
    new workflow.WorkflowJob(e2eLintBuildJob, {
      context: config.jobContext,
      name: 'Lint & Build APIM e2e',
      requires: ['Generate e2e tests SDK'],
    }),
    new workflow.WorkflowJob(e2eTestJob, {
      context: config.jobContext,
      name: 'E2E - << matrix.execution_mode >> - << matrix.database >>',
      requires: ['Lint & Build APIM e2e', 'Build APIM Management API docker image', 'Build APIM Gateway docker image'],
      matrix: {
        execution_mode: ['v3', 'v4-emulation-engine'],
        database: ['mongo', 'jdbc', 'bridge'],
      },
    }),
    new workflow.WorkflowJob(e2eCypressJob, {
      context: config.jobContext,
      name: 'Run Cypress UI tests',
      requires: [
        'Lint & Build APIM e2e',
        'Build APIM Management API docker image',
        'Build APIM Gateway docker image',
        'Build APIM Console docker image',
        'Build APIM Portal docker image',
      ],
    }),
  ];
}
