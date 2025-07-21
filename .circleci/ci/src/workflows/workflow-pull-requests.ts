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
import { commands, Config, Job, reusable, workflow, Workflow } from '@circleci/circleci-config-sdk';

import { CircleCIEnvironment } from '../pipelines';
import { isE2EBranch, isSupportBranchOrMaster } from '../utils';
import { config } from '../config';
import { BaseExecutor } from '../executors';
import {
  BuildBackendJob,
  BuildDockerBackendImageJob,
  BuildDockerWebUiImageJob,
  ChromaticConsoleJob,
  CommunityBuildBackendJob,
  ConsoleWebuiBuildJob,
  DangerJsJob,
  DeployOnAzureJob,
  E2ECypressJob,
  E2EGenerateSDKJob,
  E2ELintBuildJob,
  E2ETestJob,
  PerfLintBuildJob,
  PortalWebuiBuildJob,
  PublishJob,
  ReleaseHelmJob,
  SetupJob,
  SonarCloudAnalysisJob,
  StorybookConsoleJob,
  TestApimChartsJob,
  TestDefinitionJob,
  TestGatewayJob,
  TestIntegrationJob,
  TestPluginJob,
  TestRepositoryJob,
  TestRestApiJob,
  TriggerSaasDockerImagesJob,
  ValidateJob,
  WebuiLintTestJob,
} from '../jobs';
import { orbs } from '../orbs';

export class PullRequestsWorkflow {
  static create(dynamicConfig: Config, environment: CircleCIEnvironment): Workflow {
    let jobs: workflow.WorkflowJob[] = [];
    const shouldBuildDockerImages: boolean = isSupportBranchOrMaster(environment.branch) || isE2EBranch(environment.branch);
    // Needed to publish helm chart in internal repository
    environment.isDryRun = true;
    if (isSupportBranchOrMaster(environment.branch)) {
      jobs.push(
        ...this.getCommonJobs(dynamicConfig, environment, false, false, shouldBuildDockerImages),
        ...this.getE2EJobs(dynamicConfig, environment),
        ...this.getMasterAndSupportJobs(dynamicConfig, environment),
      );
    } else if (isE2EBranch(environment.branch)) {
      jobs.push(
        ...this.getCommonJobs(dynamicConfig, environment, false, true, shouldBuildDockerImages),
        ...this.getE2EJobs(dynamicConfig, environment),
      );
    } else {
      jobs = this.getCommonJobs(dynamicConfig, environment, true, true, shouldBuildDockerImages);
    }
    return new Workflow('pull_requests', jobs);
  }

  private static getCommonJobs(
    dynamicConfig: Config,
    environment: CircleCIEnvironment,
    filterJobs: boolean,
    addValidationJob: boolean,
    shouldBuildDockerImages: boolean,
  ): workflow.WorkflowJob[] {
    dynamicConfig.importOrb(orbs.keeper).importOrb(orbs.aquasec);

    const dangerJSJob = DangerJsJob.create(dynamicConfig);
    dynamicConfig.addJob(dangerJSJob);

    const jobs: workflow.WorkflowJob[] = [
      new workflow.WorkflowJob(orbs.aquasec.jobs.fs_scan, {
        context: config.jobContext,
        preSteps: [
          new reusable.ReusedCommand(orbs.keeper.commands['env-export'], {
            'secret-url': config.secrets.aquaKey,
            'var-name': 'AQUA_KEY',
          }),
          new reusable.ReusedCommand(orbs.keeper.commands['env-export'], {
            'secret-url': config.secrets.aquaSecret,
            'var-name': 'AQUA_SECRET',
          }),
          new reusable.ReusedCommand(orbs.keeper.commands['env-export'], {
            'secret-url': config.secrets.githubApiToken,
            'var-name': 'GITHUB_TOKEN',
          }),
        ],
      }),
      new workflow.WorkflowJob(dangerJSJob, {
        name: 'Run Danger JS',
        context: config.jobContext,
      }),
    ];
    const requires: string[] = [];

    if (!filterJobs || shouldBuildHelm(environment.changedFiles)) {
      const apimChartsTestJob = TestApimChartsJob.create(dynamicConfig, environment);
      dynamicConfig.addJob(apimChartsTestJob);
      jobs.push(
        new workflow.WorkflowJob(apimChartsTestJob, {
          name: 'Helm Chart - Lint & Test',
          context: config.jobContext,
        }),
      );

      requires.push('Helm Chart - Lint & Test');
    }

    if (!filterJobs || shouldBuildBackend(environment.changedFiles)) {
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
    }

    if (!filterJobs || shouldBuildConsole(environment.changedFiles)) {
      const webuiLintTestJob = WebuiLintTestJob.create(dynamicConfig, environment);
      dynamicConfig.addJob(webuiLintTestJob);

      const consoleWebuiBuildJob = ConsoleWebuiBuildJob.create(dynamicConfig, environment);
      dynamicConfig.addJob(consoleWebuiBuildJob);

      const storybookConsoleJob = StorybookConsoleJob.create(dynamicConfig, environment);
      dynamicConfig.addJob(storybookConsoleJob);

      const chromaticConsoleJob = ChromaticConsoleJob.create(dynamicConfig, environment);
      dynamicConfig.addJob(chromaticConsoleJob);

      const sonarCloudAnalysisJob = SonarCloudAnalysisJob.create(dynamicConfig, environment);
      dynamicConfig.addJob(sonarCloudAnalysisJob);

      jobs.push(
        new workflow.WorkflowJob(webuiLintTestJob, {
          name: 'Lint & test APIM Console',
          context: config.jobContext,
          'apim-ui-project': config.components.console.project,
          resource_class: 'xlarge',
        }),
        new workflow.WorkflowJob(consoleWebuiBuildJob, {
          name: 'Build APIM Console',
          context: config.jobContext,
        }),
      );
      requires.push('Lint & test APIM Console', 'Build APIM Console');

      if (shouldBuildDockerImages) {
        const buildDockerWebUiImageJob = BuildDockerWebUiImageJob.create(dynamicConfig, environment, false);
        dynamicConfig.addJob(buildDockerWebUiImageJob);

        jobs.push(
          new workflow.WorkflowJob(buildDockerWebUiImageJob, {
            context: config.jobContext,
            name: `Build APIM Console docker image`,
            requires: ['Build APIM Console'],
            'apim-project': config.components.console.project,
            'docker-context': '.',
            'docker-image-name': config.components.console.image,
          }),
        );
        requires.push('Build APIM Console docker image');
      }

      jobs.push(
        new workflow.WorkflowJob(storybookConsoleJob, {
          name: 'Build Console Storybook',
          context: config.jobContext,
        }),
        new workflow.WorkflowJob(chromaticConsoleJob, {
          name: 'Deploy console in chromatic',
          context: config.jobContext,
          requires: ['Build Console Storybook'],
        }),
        new workflow.WorkflowJob(sonarCloudAnalysisJob, {
          name: 'Sonar - gravitee-apim-console-webui',
          context: config.jobContext,
          requires: ['Lint & test APIM Console'],
          working_directory: config.components.console.project,
          cache_type: 'frontend',
        }),
      );
    }

    if (!filterJobs || shouldBuildPortal(environment.changedFiles)) {
      const webuiLintTestJob = WebuiLintTestJob.create(dynamicConfig, environment);
      dynamicConfig.addJob(webuiLintTestJob);

      const portalWebuiBuildJob = PortalWebuiBuildJob.create(dynamicConfig, environment);
      dynamicConfig.addJob(portalWebuiBuildJob);

      const sonarCloudAnalysisJob = SonarCloudAnalysisJob.create(dynamicConfig, environment);
      dynamicConfig.addJob(sonarCloudAnalysisJob);

      jobs.push(
        new workflow.WorkflowJob(webuiLintTestJob, {
          name: 'Lint & test APIM Portal Next',
          context: config.jobContext,
          'apim-ui-project': config.components.portal.next.project,
        }),
        new workflow.WorkflowJob(webuiLintTestJob, {
          name: 'Lint & test APIM Portal',
          context: config.jobContext,
          'apim-ui-project': config.components.portal.project,
          resource_class: 'large',
        }),
        new workflow.WorkflowJob(portalWebuiBuildJob, {
          name: 'Build APIM Portal',
          context: config.jobContext,
        }),
      );
      requires.push('Lint & test APIM Portal', 'Lint & test APIM Portal Next', 'Build APIM Portal');

      if (shouldBuildDockerImages) {
        const buildDockerWebUiImageJob = BuildDockerWebUiImageJob.create(dynamicConfig, environment, false);
        dynamicConfig.addJob(buildDockerWebUiImageJob);

        jobs.push(
          new workflow.WorkflowJob(buildDockerWebUiImageJob, {
            context: config.jobContext,
            name: `Build APIM Portal docker image`,
            requires: ['Build APIM Portal'],
            'apim-project': config.components.portal.project,
            'docker-context': '.',
            'docker-image-name': config.components.portal.image,
          }),
        );
        requires.push('Build APIM Portal docker image');
      }

      jobs.push(
        new workflow.WorkflowJob(sonarCloudAnalysisJob, {
          name: 'Sonar - gravitee-apim-portal-webui',
          context: config.jobContext,
          requires: ['Lint & test APIM Portal'],
          working_directory: config.components.portal.project,
          cache_type: 'frontend',
        }),
        new workflow.WorkflowJob(sonarCloudAnalysisJob, {
          name: 'Sonar - gravitee-apim-portal-webui-next',
          context: config.jobContext,
          requires: ['Lint & test APIM Portal Next'],
          working_directory: config.components.portal.next.project,
          cache_type: 'frontend',
        }),
      );
    }

    // Force validation workflow in case only distribution pom.xml has changed
    if (environment.changedFiles.some((file) => file.includes('gravitee-apim-distribution'))) {
      addValidationJob = true;
      requires.push('Build backend');
    }

    // compute check-workflow job
    if (addValidationJob && requires.length > 0) {
      const checkWorkflowJob = new Job('job-validate-workflow-status', BaseExecutor.create('small'), [
        new commands.Run({
          name: 'Check workflow jobs',
          command: 'echo "Congratulations! If you can read this, everything is OK"',
        }),
      ]);
      dynamicConfig.addJob(checkWorkflowJob);
      jobs.push(new workflow.WorkflowJob(checkWorkflowJob, { name: 'Validate workflow status', requires }));
    }

    return jobs;
  }

  private static getE2EJobs(dynamicConfig: Config, environment: CircleCIEnvironment): workflow.WorkflowJob[] {
    const buildDockerBackendImageJob = BuildDockerBackendImageJob.create(dynamicConfig, environment, false);
    dynamicConfig.addJob(buildDockerBackendImageJob);

    const e2eGenerateSdkJob = E2EGenerateSDKJob.create(dynamicConfig, environment);
    dynamicConfig.addJob(e2eGenerateSdkJob);

    const e2eLintBuildJob = E2ELintBuildJob.create(dynamicConfig, environment);
    dynamicConfig.addJob(e2eLintBuildJob);

    const e2eTestJob = E2ETestJob.create(dynamicConfig, environment);
    dynamicConfig.addJob(e2eTestJob);

    const e2eCypressJob = E2ECypressJob.create(dynamicConfig, environment);
    dynamicConfig.addJob(e2eCypressJob);

    const perfLintBuildJob = PerfLintBuildJob.create(dynamicConfig, environment);
    dynamicConfig.addJob(perfLintBuildJob);

    return [
      new workflow.WorkflowJob(buildDockerBackendImageJob, {
        context: config.jobContext,
        name: `Build APIM Management API docker image`,
        requires: ['Build backend'],
        'apim-project': config.components.managementApi.project,
        'docker-context': 'gravitee-apim-rest-api-standalone/gravitee-apim-rest-api-standalone-distribution/target',
        'docker-image-name': config.components.managementApi.image,
      }),
      new workflow.WorkflowJob(buildDockerBackendImageJob, {
        context: config.jobContext,
        name: `Build APIM Gateway docker image`,
        requires: ['Build backend'],
        'apim-project': config.components.gateway.project,
        'docker-context': 'gravitee-apim-gateway-standalone/gravitee-apim-gateway-standalone-distribution/target',
        'docker-image-name': config.components.gateway.image,
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

  private static getMasterAndSupportJobs(dynamicConfig: Config, environment: CircleCIEnvironment): workflow.WorkflowJob[] {
    const communityBuildJob = CommunityBuildBackendJob.create(dynamicConfig, environment);
    dynamicConfig.addJob(communityBuildJob);

    const publishOnArtifactoryJob = PublishJob.create(dynamicConfig, environment, 'artifactory');
    dynamicConfig.addJob(publishOnArtifactoryJob);

    const publishOnNexusJob = PublishJob.create(dynamicConfig, environment, 'nexus');
    dynamicConfig.addJob(publishOnNexusJob);

    const releaseHelmDryRunJob = ReleaseHelmJob.create(dynamicConfig, environment);
    dynamicConfig.addJob(releaseHelmDryRunJob);

    const deployOnAzureJob = DeployOnAzureJob.create(dynamicConfig, environment);
    dynamicConfig.addJob(deployOnAzureJob);

    const runTriggerSaasDockerImagesJob = TriggerSaasDockerImagesJob.create(
      {
        ...environment,
        isDryRun: false,
      },
      'dev',
    );
    dynamicConfig.addJob(runTriggerSaasDockerImagesJob);

    return [
      new workflow.WorkflowJob(communityBuildJob, {
        name: 'Check build as Community user',
        context: config.jobContext,
      }),
      // Trigger SaaS Docker images creation
      new workflow.WorkflowJob(runTriggerSaasDockerImagesJob, {
        context: [...config.jobContext, 'keeper-orb-publishing'],
        name: 'Trigger SaaS Docker images creation',
        requires: [
          'Build APIM Management API docker image',
          'Build APIM Gateway docker image',
          'Build APIM Console docker image',
          'Build APIM Portal docker image',
        ],
      }),
      new workflow.WorkflowJob(releaseHelmDryRunJob, {
        name: 'Publish Helm chart (internal repo)',
        context: config.jobContext,
        requires: ['Trigger SaaS Docker images creation'],
      }),
      new workflow.WorkflowJob(publishOnArtifactoryJob, {
        name: 'Publish on artifactory',
        context: config.jobContext,
        requires: ['Test definition', 'Test gateway', 'Test plugins', 'Test repository', 'Test rest-api'],
      }),
      new workflow.WorkflowJob(publishOnNexusJob, {
        name: 'Publish on nexus',
        context: config.jobContext,
        requires: ['Test definition', 'Test gateway', 'Test plugins', 'Test repository', 'Test rest-api'],
      }),
      new workflow.WorkflowJob(deployOnAzureJob, {
        name: 'Deploy on Azure cluster',
        context: config.jobContext,
        requires: [
          'Test definition',
          'Test gateway',
          'Test plugins',
          'Test repository',
          'Test rest-api',
          'Build APIM Management API docker image',
          'Build APIM Gateway docker image',
          'Build APIM Console docker image',
          'Build APIM Portal docker image',
        ],
      }),
    ];
  }
}

function shouldBuildAll(changedFiles: string[]): boolean {
  const baseDepsIdentifiers = ['.circleci', 'pom.xml', '.gitignore', '.prettierrc', 'gravitee-apim-e2e'];
  return changedFiles.some((file) => baseDepsIdentifiers.some((identifier) => file.includes(identifier)));
}

function shouldBuildHelm(changedFiles: string[]): boolean {
  return shouldBuildAll(changedFiles) || changedFiles.some((file) => file.includes('helm'));
}

function shouldBuildConsole(changedFiles: string[]): boolean {
  return shouldBuildAll(changedFiles) || changedFiles.some((file) => file.includes(config.components.console.project));
}

function shouldBuildPortal(changedFiles: string[]): boolean {
  return shouldBuildAll(changedFiles) || changedFiles.some((file) => file.includes(config.components.portal.project));
}

function shouldBuildBackend(changedFiles: string[]): boolean {
  const mavenProjectsIdentifiers = [
    'gravitee-apim-bom',
    'gravitee-apim-common',
    'gravitee-apim-definition',
    'gravitee-apim-distribution',
    'gravitee-apim-gateway',
    'gravitee-apim-integration-tests',
    'gravitee-apim-parent',
    'gravitee-apim-plugin',
    'gravitee-apim-repository',
    'gravitee-apim-rest-api',
  ];
  return (
    shouldBuildAll(changedFiles) || changedFiles.some((file) => mavenProjectsIdentifiers.some((identifier) => file.includes(identifier)))
  );
}

function shouldTestAllBackend(changedFiles: string[]): boolean {
  const mavenProjectsIdentifiers = [
    'gravitee-apim-bom',
    'gravitee-apim-common',
    'gravitee-apim-definition',
    'gravitee-apim-parent',
    'gravitee-apim-repository',
  ];
  return (
    shouldBuildAll(changedFiles) || changedFiles.some((file) => mavenProjectsIdentifiers.some((identifier) => file.includes(identifier)))
  );
}

function shouldTestDefinition(changedFiles: string[]): boolean {
  return shouldTestAllBackend(changedFiles) || changedFiles.some((file) => file.includes('gravitee-apim-definition'));
}

function shouldTestIntegrationTests(changedFiles: string[]): boolean {
  const mavenProjectsIdentifiers = [
    'gravitee-apim-bom',
    'gravitee-apim-common',
    'gravitee-apim-definition',
    'gravitee-apim-gateway',
    'gravitee-apim-integration-tests',
    'gravitee-apim-parent',
    'gravitee-apim-plugin',
  ];
  return (
    shouldTestAllBackend(changedFiles) ||
    changedFiles.some((file) => mavenProjectsIdentifiers.some((identifier) => file.includes(identifier)))
  );
}

function shouldTestGateway(changedFiles: string[]): boolean {
  const mavenProjectsIdentifiers = ['gravitee-apim-definition', 'gravitee-apim-repository', 'gravitee-apim-gateway'];
  return (
    shouldTestAllBackend(changedFiles) ||
    changedFiles.some((file) => mavenProjectsIdentifiers.some((identifier) => file.includes(identifier)))
  );
}

function shouldTestRepository(changedFiles: string[]): boolean {
  const mavenProjectsIdentifiers = ['gravitee-apim-definition', 'gravitee-apim-repository'];
  return (
    shouldTestAllBackend(changedFiles) ||
    changedFiles.some((file) => mavenProjectsIdentifiers.some((identifier) => file.includes(identifier)))
  );
}

function shouldTestPlugin(changedFiles: string[]): boolean {
  const mavenProjectsIdentifiers = ['gravitee-apim-definition', 'gravitee-apim-plugin'];
  return (
    shouldTestAllBackend(changedFiles) ||
    changedFiles.some((file) => mavenProjectsIdentifiers.some((identifier) => file.includes(identifier)))
  );
}

function shouldTestRestApi(changedFiles: string[]): boolean {
  const mavenProjectsIdentifiers = ['gravitee-apim-definition', 'gravitee-apim-repository', 'gravitee-apim-rest-api'];
  return (
    shouldTestAllBackend(changedFiles) ||
    changedFiles.some((file) => mavenProjectsIdentifiers.some((identifier) => file.includes(identifier)))
  );
}
