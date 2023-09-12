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
import { Config, workflow, Workflow } from '@circleci/circleci-config-sdk';
import {
  BuildBackendImagesJob,
  BuildBackendJob,
  DangerJsJob,
  DeployOnAzureJob,
  PerfLintBuildJob,
  SetupJob,
  SonarCloudAnalysisJob,
} from '../jobs';

import { CircleCIEnvironment } from '../pipelines';
import { isE2EBranch, isSupportBranchOrMaster } from '../utils';
import { config } from '../config';
import { SnykApimChartsJob, TestApimChartsJob } from '../jobs/helm';
import { E2ECypressJob, E2EGenerateSDKJob, E2ELintBuildJob, E2ETestJob } from '../jobs/e2e';
import { ChromaticConsoleJob, StorybookConsoleJob, WebuiBuildJob, WebuiLintTestJob } from '../jobs/frontend';
import {
  CommunityBuildBackendJob,
  PublishJob,
  TestBackendJob,
  TestIntegrationJob,
  TestPluginJob,
  TestRepositoryJob,
  ValidateJob,
} from '../jobs/backend';

export class PullRequestsWorkflow {
  static create(dynamicConfig: Config, environment: CircleCIEnvironment) {
    const isSupportOrMaster = isSupportBranchOrMaster(environment.branch);
    const isRunE2E = isE2EBranch(environment.branch);

    const jobs = this.getCommonJobs(dynamicConfig, environment);

    if (isSupportOrMaster || isRunE2E) {
      this.addE2EJobs(dynamicConfig, environment, jobs);
    }

    if (isSupportOrMaster) {
      this.addMasterAndSupportJobs(dynamicConfig, environment, jobs);
    }

    return new Workflow('pull_requests', jobs);
  }

  private static getCommonJobs(dynamicConfig: Config, environment: CircleCIEnvironment) {
    const apimChartsTestJob = TestApimChartsJob.create(dynamicConfig);
    dynamicConfig.addJob(apimChartsTestJob);

    const setupJob = SetupJob.create(dynamicConfig);
    dynamicConfig.addJob(setupJob);

    const validateBackendJob = ValidateJob.create(dynamicConfig);
    dynamicConfig.addJob(validateBackendJob);

    const dangerJSJob = DangerJsJob.create(dynamicConfig);
    dynamicConfig.addJob(dangerJSJob);

    const buildBackendJob = BuildBackendJob.create(dynamicConfig, environment);
    dynamicConfig.addJob(buildBackendJob);

    const testBackendJob = TestBackendJob.create(dynamicConfig);
    dynamicConfig.addJob(testBackendJob);

    const testIntegrationJob = TestIntegrationJob.create(dynamicConfig);
    dynamicConfig.addJob(testIntegrationJob);

    const testPluginsJob = TestPluginJob.create(dynamicConfig);
    dynamicConfig.addJob(testPluginsJob);

    const testRepositoryJob = TestRepositoryJob.create(dynamicConfig);
    dynamicConfig.addJob(testRepositoryJob);

    const webuiLintTestJob = WebuiLintTestJob.create(dynamicConfig);
    dynamicConfig.addJob(webuiLintTestJob);

    const webuiBuildJob = WebuiBuildJob.create(dynamicConfig, environment);
    dynamicConfig.addJob(webuiBuildJob);

    const e2eGenerateSdkJob = E2EGenerateSDKJob.create(dynamicConfig);
    dynamicConfig.addJob(e2eGenerateSdkJob);

    const e2eLintBuildJob = E2ELintBuildJob.create(dynamicConfig);
    dynamicConfig.addJob(e2eLintBuildJob);

    const perfLintBuildJob = PerfLintBuildJob.create(dynamicConfig);
    dynamicConfig.addJob(perfLintBuildJob);

    const storybookConsoleJob = StorybookConsoleJob.create(dynamicConfig);
    dynamicConfig.addJob(storybookConsoleJob);

    const chromaticConsoleJob = ChromaticConsoleJob.create(dynamicConfig);
    dynamicConfig.addJob(chromaticConsoleJob);

    const sonarCloudAnalysisJob = SonarCloudAnalysisJob.create(dynamicConfig, environment);
    dynamicConfig.addJob(sonarCloudAnalysisJob);

    return [
      new workflow.WorkflowJob(apimChartsTestJob, { name: 'Helm Chart - Lint & Test' }),
      new workflow.WorkflowJob(setupJob, { name: 'Setup', context: config.jobContext }),
      new workflow.WorkflowJob(validateBackendJob, {
        name: 'Validate backend',
        context: config.jobContext,
        requires: ['Setup'],
      }),
      new workflow.WorkflowJob(dangerJSJob, {
        name: 'Run Danger JS',
        context: config.jobContext,
        requires: ['Validate backend'],
      }),
      new workflow.WorkflowJob(buildBackendJob, {
        name: 'Build backend',
        context: config.jobContext,
        requires: ['Validate backend'],
      }),
      new workflow.WorkflowJob(testBackendJob, {
        name: 'Test backend',
        context: config.jobContext,
        requires: ['Build backend'],
      }),
      new workflow.WorkflowJob(testIntegrationJob, {
        name: 'Integration tests',
        context: config.jobContext,
        requires: ['Build backend'],
      }),
      new workflow.WorkflowJob(testPluginsJob, {
        name: 'Test plugins',
        context: config.jobContext,
        requires: ['Build backend'],
      }),
      new workflow.WorkflowJob(testRepositoryJob, {
        name: 'Test repository',
        context: config.jobContext,
        requires: ['Build backend'],
      }),
      new workflow.WorkflowJob(webuiLintTestJob, {
        name: 'Lint & test APIM Console',
        context: config.jobContext,
        requires: ['Setup'],
        'apim-ui-project': 'gravitee-apim-console-webui',
        resource_class: 'large',
      }),
      new workflow.WorkflowJob(webuiBuildJob, {
        name: 'Build APIM Console and publish image',
        context: config.jobContext,
        requires: ['Setup'],
        'apim-ui-project': 'gravitee-apim-console-webui',
        'docker-image-name': config.dockerImages.console,
      }),
      new workflow.WorkflowJob(webuiLintTestJob, {
        name: 'Lint & test APIM Portal',
        context: config.jobContext,
        requires: ['Setup'],
        'apim-ui-project': 'gravitee-apim-portal-webui',
        resource_class: 'large',
      }),
      new workflow.WorkflowJob(webuiBuildJob, {
        name: 'Build APIM Portal and publish image',
        context: config.jobContext,
        requires: ['Setup'],
        'apim-ui-project': 'gravitee-apim-portal-webui',
        'docker-image-name': config.dockerImages.portal,
      }),
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
      new workflow.WorkflowJob(perfLintBuildJob, {
        context: config.jobContext,
        name: 'Lint & Build APIM perf',
        requires: ['Generate e2e tests SDK'],
      }),

      new workflow.WorkflowJob(storybookConsoleJob, {
        name: 'Build Console Storybook',
        context: config.jobContext,
        requires: ['Setup'],
      }),
      new workflow.WorkflowJob(chromaticConsoleJob, {
        name: 'Deploy console in chromatic',
        context: config.jobContext,
        requires: ['Build Console Storybook'],
      }),
      new workflow.WorkflowJob(sonarCloudAnalysisJob, {
        name: 'Sonar - << matrix.working_directory >>',
        context: config.jobContext,
        requires: ['Test backend'],
        matrix: { working_directory: ['gravitee-apim-rest-api', 'gravitee-apim-gateway', 'gravitee-apim-definition'] },
      }),
      new workflow.WorkflowJob(sonarCloudAnalysisJob, {
        name: 'Sonar - gravitee-apim-plugin',
        context: config.jobContext,
        requires: ['Test plugins'],
        working_directory: 'gravitee-apim-plugin',
      }),
      new workflow.WorkflowJob(sonarCloudAnalysisJob, {
        name: 'Sonar - gravitee-apim-repository',
        context: config.jobContext,
        requires: ['Test repository'],
        working_directory: 'gravitee-apim-repository',
      }),
      new workflow.WorkflowJob(sonarCloudAnalysisJob, {
        name: 'Sonar - gravitee-apim-console-webui',
        context: config.jobContext,
        requires: ['Lint & test APIM Console'],
        working_directory: 'gravitee-apim-console-webui',
      }),
      new workflow.WorkflowJob(sonarCloudAnalysisJob, {
        name: 'Sonar - gravitee-apim-portal-webui',
        context: config.jobContext,
        requires: ['Lint & test APIM Portal'],
        working_directory: 'gravitee-apim-portal-webui',
      }),
    ];
  }

  private static addE2EJobs(dynamicConfig: Config, environment: CircleCIEnvironment, jobs: workflow.WorkflowJob[]) {
    const communityBuildJob = CommunityBuildBackendJob.create(dynamicConfig);
    dynamicConfig.addJob(communityBuildJob);

    const buildImagesJob = BuildBackendImagesJob.create(dynamicConfig, environment);
    dynamicConfig.addJob(buildImagesJob);

    const e2eTestJob = E2ETestJob.create(dynamicConfig, environment);
    dynamicConfig.addJob(e2eTestJob);

    const e2eCypressJob = E2ECypressJob.create(dynamicConfig, environment);
    dynamicConfig.addJob(e2eCypressJob);

    jobs.push(
      new workflow.WorkflowJob(communityBuildJob, { name: 'Check build as Community user', context: config.jobContext }),
      new workflow.WorkflowJob(buildImagesJob, {
        name: 'Build and push rest api and gateway images',
        context: config.jobContext,
        requires: ['Build backend'],
      }),
      new workflow.WorkflowJob(e2eTestJob, {
        context: config.jobContext,
        name: 'E2E - << matrix.execution_mode >> - << matrix.database >>',
        requires: ['Lint & Build APIM e2e', 'Build and push rest api and gateway images'],
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
          'Build and push rest api and gateway images',
          'Build APIM Console and publish image',
          'Build APIM Portal and publish image',
        ],
      }),
    );
  }

  private static addMasterAndSupportJobs(dynamicConfig: Config, environment: CircleCIEnvironment, jobs: workflow.WorkflowJob[]) {
    const snykApimChartsJob = SnykApimChartsJob.create(dynamicConfig, environment);
    dynamicConfig.addJob(snykApimChartsJob);

    const publishOnArtifactoryJob = PublishJob.create(dynamicConfig, 'artifactory');
    dynamicConfig.addJob(publishOnArtifactoryJob);

    const publishOnNexusJob = PublishJob.create(dynamicConfig, 'nexus');
    dynamicConfig.addJob(publishOnNexusJob);

    const deployOnAzureJob = DeployOnAzureJob.create(dynamicConfig, environment);
    dynamicConfig.addJob(deployOnAzureJob);

    jobs.push(
      new workflow.WorkflowJob(snykApimChartsJob, { name: 'Scan snyk Helm chart', context: config.jobContext, requires: ['Setup'] }),
      new workflow.WorkflowJob(publishOnArtifactoryJob, {
        name: 'Publish on artifactory',
        context: config.jobContext,
        requires: ['Test backend', 'Test plugins', 'Test repository'],
      }),
      new workflow.WorkflowJob(publishOnNexusJob, {
        name: 'Publish on nexus',
        context: config.jobContext,
        requires: ['Test backend', 'Test plugins', 'Test repository'],
      }),
      new workflow.WorkflowJob(deployOnAzureJob, {
        name: 'Deploy on Azure cluster',
        context: config.jobContext,
        requires: [
          'Test backend',
          'Test plugins',
          'Test repository',
          'Build and push rest api and gateway images',
          'Build APIM Console and publish image',
          'Build APIM Portal and publish image',
        ],
      }),
    );
  }
}
