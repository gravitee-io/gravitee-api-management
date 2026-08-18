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
import { Config, workflow, Workflow } from '../circleci-config';
import { AikidoScanDockerImagesJob, DeployOnAzureJob, TestIntegrationJob } from '../jobs';
import { CircleCIEnvironment } from '../pipelines';
import { config } from '../config';
import { devEnvironmentJobs } from './groups/dev-environment-jobs';
import { e2eJobs } from './groups/e2e-jobs';
import { chainguardFipsImageJobs } from './groups/chainguard-fips-image-jobs';

/**
 * The scheduled build, one per branch: the default branch and every live support branch.
 *
 * It is the dev-environment refresh plus the two suites that run nowhere else. Pull requests
 * cover their own scope — the changed-files predicates are dependency-aware, so anything a
 * change could affect is retested before merge — and a push to a branch only refreshes the
 * environment. What no one runs, then, is the real-plugin integration tests and the end-to-end
 * suites. It deliberately does not replay the per-module or frontend test suites.
 */
export class NightlyWorkflow {
  static create(dynamicConfig: Config, environment: CircleCIEnvironment): Workflow {
    const testIntegrationJob = TestIntegrationJob.create(dynamicConfig, environment);
    dynamicConfig.addJob(testIntegrationJob);

    const deployOnAzureJob = DeployOnAzureJob.create(dynamicConfig, environment);
    dynamicConfig.addJob(deployOnAzureJob);

    return new Workflow('nightly', [
      ...devEnvironmentJobs(dynamicConfig, environment),

      // Only the FIPS variants. The chainguard images are built — and scanned — by every branch
      // push, since the environment runs them.
      ...chainguardFipsImageJobs(dynamicConfig, environment),

      new workflow.WorkflowJob(testIntegrationJob, {
        name: 'Integration tests',
        context: config.jobContext,
        requires: ['Build backend'],
      }),
      ...e2eJobs(dynamicConfig, environment),

      // Last, and only once everything the environment will run has been exercised.
      //
      // Cypress is required rather than the E2E matrix: fanning in on a matrix job means
      // depending on its CircleCI alias, not on the templated name, and this repository has no
      // precedent for it. Cypress already waits on the same four images, so the deployment is
      // gated on a green run either way — but it does not wait for the E2E matrix itself. Worth
      // settling on the first real run.
      new workflow.WorkflowJob(deployOnAzureJob, {
        name: 'Deploy on Azure cluster',
        context: config.jobContext,
        requires: ['Integration tests', 'Run Cypress UI tests'],
      }),

      // What this workflow builds: the standard images and the FIPS variants. FIPS is scanned
      // nowhere else between two releases; the chainguard variants are scanned on the branch
      // push that builds them.
      ...AikidoScanDockerImagesJob.workflowJobs(dynamicConfig, environment, false, '', ['chainguard-fips']),
    ]);
  }
}
