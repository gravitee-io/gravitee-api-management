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
import { Config, Job, workflow, Workflow } from '../circleci-config';
import {
  AikidoScanDockerImagesJob,
  DeployOnAzureJob,
  SonarCloudAnalysisJob,
  TestDefinitionJob,
  TestGatewayJob,
  TestIntegrationJob,
  TestPluginJob,
  TestReporterJob,
  TestRepositoryJob,
  TestRestApiJob,
  WebuiLintTestJob,
} from '../jobs';
import { CircleCIEnvironment } from '../pipelines';
import { config } from '../config';
import { devEnvironmentJobs } from './groups/dev-environment-jobs';
import { e2eJobs } from './groups/e2e-jobs';
import { chainguardFipsImageJobs } from './groups/chainguard-fips-image-jobs';

/**
 * The scheduled build, one per branch: the default branch and every live support branch.
 *
 * Two chains hang off a single `Build backend`, for two unrelated purposes.
 *
 * The first refreshes the development environment: the web UIs and the docker images the
 * cluster runs, then the end-to-end suites and the deployment.
 *
 * The second gives SonarCloud an analysis of the reference branch. A push to a branch only
 * refreshes the environment, so nothing has analysed the default branch since that became
 * true — and without it a pull request has no new-code baseline and the server-side cache
 * finds nothing to reuse. The suites are replayed here for the coverage reports they leave
 * behind, not to retest what pull requests already covered.
 *
 * The jobs are spelled out rather than borrowed from the pull-request groups: those carry a
 * changed-files filter and a build-and-publish scope this workflow has no use for.
 */
export class NightlyWorkflow {
  static create(dynamicConfig: Config, environment: CircleCIEnvironment): Workflow {
    const testIntegrationJob = TestIntegrationJob.create(dynamicConfig, environment);
    dynamicConfig.addJob(testIntegrationJob);

    const deployOnAzureJob = DeployOnAzureJob.create(dynamicConfig, environment);
    dynamicConfig.addJob(deployOnAzureJob);

    // The analysis chain. One parameterized analysis job, reused with a different working
    // directory; one test job per module, each waiting on the shared `Build backend`.
    const sonarAnalysisJob = SonarCloudAnalysisJob.create(dynamicConfig, environment);
    dynamicConfig.addJob(sonarAnalysisJob);

    const backendSuites: { job: Job; name: string; project: string }[] = [
      { job: TestDefinitionJob.create(dynamicConfig, environment), name: 'Test definition', project: 'gravitee-apim-definition' },
      { job: TestGatewayJob.create(dynamicConfig, environment), name: 'Test gateway', project: 'gravitee-apim-gateway' },
      { job: TestRestApiJob.create(dynamicConfig, environment), name: 'Test rest-api', project: 'gravitee-apim-rest-api' },
      { job: TestPluginJob.create(dynamicConfig, environment), name: 'Test plugins', project: 'gravitee-apim-plugin' },
      { job: TestReporterJob.create(dynamicConfig, environment), name: 'Test reporters', project: 'gravitee-apim-reporter' },
      { job: TestRepositoryJob.create(dynamicConfig, environment), name: 'Test repository', project: 'gravitee-apim-repository' },
    ];
    backendSuites.forEach(({ job }) => dynamicConfig.addJob(job));

    const consoleLintTestJob = WebuiLintTestJob.createNx(dynamicConfig, environment);
    dynamicConfig.addJob(consoleLintTestJob);

    const portalNextLintTestJob = WebuiLintTestJob.createNx(dynamicConfig, environment);
    dynamicConfig.addJob(portalNextLintTestJob);

    const portalLintTestJob = WebuiLintTestJob.create(dynamicConfig, environment);
    dynamicConfig.addJob(portalLintTestJob);

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

      // The analysis chain: the suites that produce the coverage reports, each followed by the
      // analysis that reads them. Nothing downstream waits on these.
      ...backendSuites.flatMap(({ job, name, project }) => [
        new workflow.WorkflowJob(job, {
          name,
          context: config.jobContext,
          requires: ['Build backend'],
        }),
        new workflow.WorkflowJob(sonarAnalysisJob, {
          name: `Sonar - ${project}`,
          context: config.jobContext,
          requires: [name],
          working_directory: project,
          cache_type: 'backend',
        }),
      ]),

      new workflow.WorkflowJob(consoleLintTestJob, {
        name: 'Lint & test APIM Console',
        context: config.jobContext,
        'apim-ui-project-workdir': config.components.console.workdir,
        'nx-project': 'console',
        resource_class: 'xlarge',
        'max-workers': '7',
      }),
      new workflow.WorkflowJob(sonarAnalysisJob, {
        name: 'Sonar - gravitee-apim-console-webui',
        context: config.jobContext,
        requires: ['Lint & test APIM Console'],
        working_directory: config.components.console.project,
        cache_type: 'frontend',
      }),

      new workflow.WorkflowJob(portalNextLintTestJob, {
        name: 'Lint & test APIM Portal Next',
        context: config.jobContext,
        'apim-ui-project-workdir': config.components.portal.next.project,
        'nx-project': 'portal-next',
        'max-workers': '2',
      }),
      new workflow.WorkflowJob(sonarAnalysisJob, {
        name: 'Sonar - gravitee-apim-portal-webui-next',
        context: config.jobContext,
        requires: ['Lint & test APIM Portal Next'],
        working_directory: config.components.portal.next.project,
        cache_type: 'frontend',
      }),

      new workflow.WorkflowJob(portalLintTestJob, {
        name: 'Lint & test APIM Portal',
        context: config.jobContext,
        'apim-ui-project': config.components.portal.project,
        'apim-ui-project-workdir': config.components.portal.workdir,
        resource_class: 'large',
      }),
      new workflow.WorkflowJob(sonarAnalysisJob, {
        name: 'Sonar - gravitee-apim-portal-webui',
        context: config.jobContext,
        requires: ['Lint & test APIM Portal'],
        working_directory: config.components.portal.workdir,
        cache_type: 'frontend',
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
