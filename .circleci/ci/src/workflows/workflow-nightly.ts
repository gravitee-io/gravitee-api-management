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
import {
  BuildBackendJob,
  BuildDockerWebUiImageJob,
  ConsoleWebuiBuildJob,
  DeployOnAzureJob,
  GammaWebuiBuildJob,
  PortalWebuiBuildJob,
  SetupJob,
  TestIntegrationJob,
} from '../jobs';
import { CircleCIEnvironment } from '../pipelines';
import { config } from '../config';
import { backendImageJobs } from './groups/backend-image-jobs';
import { e2eJobs } from './groups/e2e-jobs';

/**
 * The scheduled build, one per branch: master and every live support branch.
 *
 * It carries the two suites that run nowhere else. Pull requests cover their own scope — the
 * changed-files predicates are dependency-aware, so anything a change could affect is already
 * retested before merge — and a push to a branch only refreshes the dev environment. What no
 * one runs, then, is the real-plugin integration tests and the end-to-end suites. That is what
 * this workflow is for; it deliberately does not replay the per-module or frontend test suites.
 *
 * The job list is spelled out rather than composed from the pull-request groups. Those groups
 * bundle each project's build with its own lint, test and Sonar jobs, which is the right shape
 * for a pull request and the wrong one here. Threading flags through them to subtract jobs
 * would cost more legibility than these declarations do — and a workflow that runs unattended
 * is worth being able to read in one pass.
 */
export class NightlyWorkflow {
  static create(dynamicConfig: Config, environment: CircleCIEnvironment): Workflow {
    const setupJob = SetupJob.create(dynamicConfig);
    dynamicConfig.addJob(setupJob);

    const buildBackendJob = BuildBackendJob.create(dynamicConfig, environment);
    dynamicConfig.addJob(buildBackendJob);

    const testIntegrationJob = TestIntegrationJob.create(dynamicConfig, environment);
    dynamicConfig.addJob(testIntegrationJob);

    const consoleWebuiBuildJob = ConsoleWebuiBuildJob.create(dynamicConfig, environment);
    dynamicConfig.addJob(consoleWebuiBuildJob);

    const portalWebuiBuildJob = PortalWebuiBuildJob.create(dynamicConfig, environment);
    dynamicConfig.addJob(portalWebuiBuildJob);

    const gammaWebuiBuildJob = GammaWebuiBuildJob.create(dynamicConfig, environment);
    dynamicConfig.addJob(gammaWebuiBuildJob);

    const buildDockerWebUiImageJob = BuildDockerWebUiImageJob.create(dynamicConfig, environment, false);
    dynamicConfig.addJob(buildDockerWebUiImageJob);

    const deployOnAzureJob = DeployOnAzureJob.create(dynamicConfig, environment);
    dynamicConfig.addJob(deployOnAzureJob);

    return new Workflow('nightly', [
      new workflow.WorkflowJob(setupJob, { name: 'Setup', context: config.jobContext }),
      new workflow.WorkflowJob(buildBackendJob, {
        name: 'Build backend',
        context: config.jobContext,
        requires: ['Setup'],
      }),
      new workflow.WorkflowJob(testIntegrationJob, {
        name: 'Integration tests',
        context: config.jobContext,
        requires: ['Build backend'],
      }),

      // Frontend builds and their images. No lint, test or Sonar: pull requests own those.
      new workflow.WorkflowJob(consoleWebuiBuildJob, { name: 'Build APIM Console', context: config.jobContext }),
      new workflow.WorkflowJob(buildDockerWebUiImageJob, {
        context: config.jobContext,
        name: `Build APIM Console docker image`,
        requires: ['Build APIM Console'],
        'apim-project': config.components.console.project,
        'apim-project-workdir': config.components.console.workdir,
        'docker-context': '.',
        'docker-image-name': config.components.console.image,
      }),
      new workflow.WorkflowJob(portalWebuiBuildJob, { name: 'Build APIM Portal', context: config.jobContext }),
      new workflow.WorkflowJob(buildDockerWebUiImageJob, {
        context: config.jobContext,
        name: `Build APIM Portal docker image`,
        requires: ['Build APIM Portal'],
        'apim-project': config.components.portal.project,
        'apim-project-workdir': config.components.portal.workdir,
        'docker-context': '.',
        'docker-image-name': config.components.portal.image,
      }),
      new workflow.WorkflowJob(gammaWebuiBuildJob, { name: 'Build Gamma Console', context: config.jobContext }),
      new workflow.WorkflowJob(buildDockerWebUiImageJob, {
        context: config.jobContext,
        name: `Build Gamma Console docker image`,
        requires: ['Build Gamma Console'],
        'apim-project': config.components.gamma.project,
        'apim-project-workdir': config.components.gamma.workdir,
        'docker-context': '.',
        'docker-image-name': config.components.gamma.image,
      }),

      ...backendImageJobs(dynamicConfig, environment),
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
    ]);
  }
}
