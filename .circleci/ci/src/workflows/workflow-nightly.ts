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
import { AikidoScanDockerImagesJob, DeployOnAzureJob, NxFormatCheckJob, SonarCloudAnalysisJob, TestIntegrationJob } from '../jobs';
import { CircleCIEnvironment } from '../pipelines';
import { config } from '../config';
import { analysisJobFor, BACKEND_ANALYSED_PROJECTS, FRONTEND_ANALYSED_PROJECTS } from './groups/analysed-projects';
import { devEnvironmentJobs } from './groups/dev-environment-jobs';
import { e2eJobs } from './groups/e2e-jobs';
import { chainguardFipsImageJobs } from './groups/chainguard-fips-image-jobs';

/**
 * The scheduled build, one per branch: the default branch and every live support branch.
 *
 * Two chains hang off a single `Build backend`: the development environment refresh, and the
 * analysis of the reference branch. A push to a branch only refreshes the environment, so
 * nothing else analyses the default branch — and without that, a pull request has no new-code
 * baseline and the server-side cache finds nothing to reuse.
 *
 * The analysed projects come from the list the pull-request groups use, taken unfiltered here:
 * a module is analysed on both or on neither.
 */
export class NightlyWorkflow {
  static create(dynamicConfig: Config, environment: CircleCIEnvironment): Workflow {
    const testIntegrationJob = TestIntegrationJob.create(dynamicConfig, environment);
    dynamicConfig.addJob(testIntegrationJob);

    const deployOnAzureJob = DeployOnAzureJob.create(dynamicConfig, environment);
    dynamicConfig.addJob(deployOnAzureJob);

    const sonarAnalysisJob = SonarCloudAnalysisJob.create(dynamicConfig, environment);
    dynamicConfig.addJob(sonarAnalysisJob);

    const formatCheckJob = NxFormatCheckJob.create(dynamicConfig, environment, 'all');
    dynamicConfig.addJob(formatCheckJob);

    const analysisChain = [
      ...BACKEND_ANALYSED_PROJECTS.map((project) => ({ project, requires: ['Build backend'] })),
      ...FRONTEND_ANALYSED_PROJECTS.map((project) => ({ project, requires: undefined })),
    ].flatMap(({ project, requires }) => {
      const suiteJob = project.createSuite(dynamicConfig, environment);
      dynamicConfig.addJob(suiteJob);
      return [
        new workflow.WorkflowJob(suiteJob, {
          name: project.suiteName,
          context: config.jobContext,
          ...(requires ? { requires } : {}),
          ...(project.suiteParameters ?? {}),
        }),
        analysisJobFor(project, sonarAnalysisJob),
      ];
    });

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

      // The nx lint & test suites below do not check formatting — this one does. 'all' rather than
      // the pull-request scope: on the reference branch there is no base to compare against.
      new workflow.WorkflowJob(formatCheckJob, {
        name: 'Check prettier formatting for nx projects',
        context: config.jobContext,
      }),

      // The suites replayed for the coverage reports they leave behind, each followed by the
      // analysis that reads them. Nothing downstream waits on these.
      ...analysisChain,
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
