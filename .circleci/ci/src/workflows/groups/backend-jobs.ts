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
import {
  BuildBackendJob,
  SetupJob,
  SonarCloudAnalysisJob,
  TestDefinitionJob,
  TestGammaJob,
  TestGammaUiJob,
  TestGatewayJob,
  TestIntegrationJob,
  TestPluginJob,
  TestReporterJob,
  TestRepositoryJob,
  TestRestApiJob,
  ValidateJob,
} from '../../jobs';
import { CircleCIEnvironment } from '../../pipelines';
import { config } from '../../config';
import {
  shouldBuildBackend,
  shouldTestDefinition,
  shouldTestGamma,
  shouldTestGateway,
  shouldTestIntegrationTests,
  shouldTestPlugin,
  shouldTestReporter,
  shouldTestRepository,
  shouldTestRestApi,
} from './changed-files';

/**
 * Setup, validation, the engine build and the per-module test suites.
 *
 * Returns both the jobs and the names a downstream gate should wait on: callers accumulate
 * the two lists in the order they contribute the groups, which is what keeps the emitted
 * job list and the `requires` list stable.
 */
export function backendJobs(
  dynamicConfig: Config,
  environment: CircleCIEnvironment,
  filterJobs: boolean,
): { jobs: workflow.WorkflowJob[]; requires: string[] } {
  const jobs: workflow.WorkflowJob[] = [];
  const requires: string[] = [];

  if (filterJobs && !shouldBuildBackend(environment.changedFiles)) {
    return { jobs, requires };
  }

  const setupJob = SetupJob.create(dynamicConfig);
  dynamicConfig.addJob(setupJob);

  const validateBackendJob = ValidateJob.create(dynamicConfig, environment);
  dynamicConfig.addJob(validateBackendJob);

  const buildBackendJob = BuildBackendJob.create(dynamicConfig, environment);
  dynamicConfig.addJob(buildBackendJob);

  jobs.push(
    new workflow.WorkflowJob(setupJob, { name: 'Setup', context: config.jobContext }),
    new workflow.WorkflowJob(validateBackendJob, {
      name: 'Validate backend',
      context: config.jobContext,
      requires: ['Setup'],
    }),
    new workflow.WorkflowJob(buildBackendJob, {
      name: 'Build backend',
      context: config.jobContext,
      requires: ['Validate backend'],
    }),
  );
  requires.push('Build backend');

  if (!filterJobs || shouldTestDefinition(environment.changedFiles)) {
    const testDefinitionJob = TestDefinitionJob.create(dynamicConfig, environment);
    dynamicConfig.addJob(testDefinitionJob);

    const sonarCloudAnalysisJob = SonarCloudAnalysisJob.create(dynamicConfig, environment);
    dynamicConfig.addJob(sonarCloudAnalysisJob);

    jobs.push(
      new workflow.WorkflowJob(testDefinitionJob, {
        name: 'Test definition',
        context: config.jobContext,
        requires: ['Build backend'],
      }),
      new workflow.WorkflowJob(sonarCloudAnalysisJob, {
        name: 'Sonar - gravitee-apim-definition',
        context: config.jobContext,
        requires: ['Test definition'],
        working_directory: 'gravitee-apim-definition',
        cache_type: 'backend',
      }),
    );
    requires.push('Test definition');
  }

  if (!filterJobs || shouldTestGateway(environment.changedFiles)) {
    const testGatewayJob = TestGatewayJob.create(dynamicConfig, environment);
    dynamicConfig.addJob(testGatewayJob);

    const sonarCloudAnalysisJob = SonarCloudAnalysisJob.create(dynamicConfig, environment);
    dynamicConfig.addJob(sonarCloudAnalysisJob);

    jobs.push(
      new workflow.WorkflowJob(testGatewayJob, {
        name: 'Test gateway',
        context: config.jobContext,
        requires: ['Build backend'],
      }),
      new workflow.WorkflowJob(sonarCloudAnalysisJob, {
        name: 'Sonar - gravitee-apim-gateway',
        context: config.jobContext,
        requires: ['Test gateway'],
        working_directory: 'gravitee-apim-gateway',
        cache_type: 'backend',
      }),
    );
    requires.push('Test gateway');
  }

  if (!filterJobs || shouldTestRestApi(environment.changedFiles)) {
    const testRestApiJob = TestRestApiJob.create(dynamicConfig, environment);
    dynamicConfig.addJob(testRestApiJob);

    const sonarCloudAnalysisJob = SonarCloudAnalysisJob.create(dynamicConfig, environment);
    dynamicConfig.addJob(sonarCloudAnalysisJob);

    jobs.push(
      new workflow.WorkflowJob(testRestApiJob, {
        name: 'Test rest-api',
        context: config.jobContext,
        requires: ['Build backend'],
      }),
      new workflow.WorkflowJob(sonarCloudAnalysisJob, {
        name: 'Sonar - gravitee-apim-rest-api',
        context: config.jobContext,
        requires: ['Test rest-api'],
        working_directory: 'gravitee-apim-rest-api',
        cache_type: 'backend',
      }),
    );
    requires.push('Test rest-api');
  }

  if (!filterJobs || shouldTestGamma(environment.changedFiles)) {
    const testGammaJob = TestGammaJob.create(dynamicConfig, environment);
    dynamicConfig.addJob(testGammaJob);

    const testGammaUiJob = TestGammaUiJob.create(dynamicConfig, environment);
    dynamicConfig.addJob(testGammaUiJob);

    jobs.push(
      new workflow.WorkflowJob(testGammaJob, {
        name: 'Test gamma',
        context: config.jobContext,
        requires: ['Build backend'],
      }),
      new workflow.WorkflowJob(testGammaUiJob, {
        name: 'Test gamma UI',
        context: config.jobContext,
        requires: ['Build backend'],
      }),
    );
    requires.push('Test gamma', 'Test gamma UI');
  }

  if (!filterJobs || shouldTestIntegrationTests(environment.changedFiles)) {
    const testIntegrationJob = TestIntegrationJob.create(dynamicConfig, environment);
    dynamicConfig.addJob(testIntegrationJob);

    jobs.push(
      new workflow.WorkflowJob(testIntegrationJob, {
        name: 'Integration tests',
        context: config.jobContext,
        requires: ['Build backend'],
      }),
    );
    requires.push('Integration tests');
  }

  if (!filterJobs || shouldTestPlugin(environment.changedFiles)) {
    const testPluginsJob = TestPluginJob.create(dynamicConfig, environment);
    dynamicConfig.addJob(testPluginsJob);

    const sonarCloudAnalysisJob = SonarCloudAnalysisJob.create(dynamicConfig, environment);
    dynamicConfig.addJob(sonarCloudAnalysisJob);

    jobs.push(
      new workflow.WorkflowJob(testPluginsJob, {
        name: 'Test plugins',
        context: config.jobContext,
        requires: ['Build backend'],
      }),
      new workflow.WorkflowJob(sonarCloudAnalysisJob, {
        name: 'Sonar - gravitee-apim-plugin',
        context: config.jobContext,
        requires: ['Test plugins'],
        working_directory: 'gravitee-apim-plugin',
        cache_type: 'backend',
      }),
    );
    requires.push('Test plugins');
  }

  if (!filterJobs || shouldTestReporter(environment.changedFiles)) {
    const testReporterJob = TestReporterJob.create(dynamicConfig, environment);
    dynamicConfig.addJob(testReporterJob);

    const sonarCloudAnalysisJob = SonarCloudAnalysisJob.create(dynamicConfig, environment);
    dynamicConfig.addJob(sonarCloudAnalysisJob);

    jobs.push(
      new workflow.WorkflowJob(testReporterJob, {
        name: 'Test reporters',
        context: config.jobContext,
        requires: ['Build backend'],
      }),
      new workflow.WorkflowJob(sonarCloudAnalysisJob, {
        name: 'Sonar - gravitee-apim-reporter',
        context: config.jobContext,
        requires: ['Test reporters'],
        working_directory: 'gravitee-apim-reporter',
        cache_type: 'backend',
      }),
    );
    requires.push('Test reporters');
  }

  if (!filterJobs || shouldTestRepository(environment.changedFiles)) {
    const testRepositoryJob = TestRepositoryJob.create(dynamicConfig, environment);
    dynamicConfig.addJob(testRepositoryJob);

    const sonarCloudAnalysisJob = SonarCloudAnalysisJob.create(dynamicConfig, environment);
    dynamicConfig.addJob(sonarCloudAnalysisJob);

    jobs.push(
      new workflow.WorkflowJob(testRepositoryJob, {
        name: 'Test repository',
        context: config.jobContext,
        requires: ['Build backend'],
      }),
      new workflow.WorkflowJob(sonarCloudAnalysisJob, {
        name: 'Sonar - gravitee-apim-repository',
        context: config.jobContext,
        requires: ['Test repository'],
        working_directory: 'gravitee-apim-repository',
        cache_type: 'backend',
      }),
    );
    requires.push('Test repository');
  }

  return { jobs, requires };
}
