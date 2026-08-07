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
import { commands, Config, Job, workflow, Workflow } from '../circleci-config';

import { CircleCIEnvironment } from '../pipelines';
import { isE2EBranch, isSupportBranchOrMaster } from '../utils';
import { config } from '../config';
import { BaseExecutor } from '../executors';
import {
  BuildBackendJob,
  BuildDockerWebUiImageJob,
  ConsoleWebuiBuildJob,
  DangerJsJob,
  PortalWebuiBuildJob,
  SetupJob,
  SonarCloudAnalysisJob,
  TestApimChartsJob,
  TestDefinitionJob,
  TestGatewayJob,
  TestIntegrationJob,
  TestPluginJob,
  TestReporterJob,
  TestRepositoryJob,
  TestRestApiJob,
  ValidateJob,
  NxFormatCheckJob,
  WebuiLintTestJob,
  GammaWebuiBuildJob,
} from '../jobs';
import { orbs } from '../orbs';
import { backendImageJobs } from './groups/backend-image-jobs';
import { e2eJobs } from './groups/e2e-jobs';
import { chainguardFipsJobs } from './groups/chainguard-fips-jobs';
import { masterAndSupportJobs } from './groups/master-and-support-jobs';
import {
  shouldBuildBackend,
  shouldBuildConsole,
  shouldBuildGammaUI,
  shouldBuildHelm,
  shouldBuildPortal,
  shouldBuildPortalNext,
  shouldBuildWebuiLibs,
  shouldTestDefinition,
  shouldTestGateway,
  shouldTestIntegrationTests,
  shouldTestPlugin,
  shouldTestReporter,
  shouldTestRepository,
  shouldTestRestApi,
} from './groups/changed-files';

export class PullRequestsWorkflow {
  static create(dynamicConfig: Config, environment: CircleCIEnvironment): Workflow {
    let jobs: workflow.WorkflowJob[] = [];
    const shouldBuildDockerImages: boolean = isSupportBranchOrMaster(environment.branch) || isE2EBranch(environment.branch);
    // Needed to publish helm chart in internal repository
    environment.isDryRun = true;
    if (isSupportBranchOrMaster(environment.branch)) {
      jobs.push(
        ...this.getCommonJobs(dynamicConfig, environment, false, false, shouldBuildDockerImages),
        ...backendImageJobs(dynamicConfig, environment),
        ...e2eJobs(dynamicConfig, environment),
        ...(shouldBuildDockerImages ? chainguardFipsJobs(dynamicConfig, environment) : []),
        ...masterAndSupportJobs(dynamicConfig, environment),
      );
    } else if (isE2EBranch(environment.branch)) {
      jobs.push(
        ...this.getCommonJobs(dynamicConfig, environment, false, true, shouldBuildDockerImages),
        ...backendImageJobs(dynamicConfig, environment),
        ...e2eJobs(dynamicConfig, environment),
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
    dynamicConfig.importOrb(orbs.keeper);

    const dangerJSJob = DangerJsJob.create(dynamicConfig);
    dynamicConfig.addJob(dangerJSJob);

    const jobs: workflow.WorkflowJob[] = [
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

      if (!filterJobs || shouldTestIntegrationTests(environment.changedFiles)) {
        // Force validation workflow in case only integration tests have change
        // addValidationJob = true;
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
    }

    // Format check (Prettier) for all frontend projects
    if (
      !filterJobs ||
      shouldBuildWebuiLibs(environment.changedFiles) ||
      shouldBuildConsole(environment.changedFiles) ||
      shouldBuildPortalNext(environment.changedFiles) ||
      shouldBuildPortal(environment.changedFiles) ||
      shouldBuildGammaUI(environment.changedFiles)
    ) {
      const formatCheckJob = NxFormatCheckJob.create(dynamicConfig, environment);
      dynamicConfig.addJob(formatCheckJob);
      jobs.push(
        new workflow.WorkflowJob(formatCheckJob, {
          name: 'Check prettier formatting for nx projects',
          context: config.jobContext,
        }),
      );
      requires.push('Check prettier formatting for nx projects');
    }

    // Lint & Test APIM Libs
    if (!filterJobs || shouldBuildWebuiLibs(environment.changedFiles)) {
      const webuiLibsLintTestJob = WebuiLintTestJob.createNxLibs(dynamicConfig, environment);
      dynamicConfig.addJob(webuiLibsLintTestJob);
      jobs.push(
        new workflow.WorkflowJob(webuiLibsLintTestJob, {
          name: 'Lint & test APIM Libs',
          context: config.jobContext,
        }),
      );
      requires.push('Lint & test APIM Libs');
    }

    if (!filterJobs || shouldBuildConsole(environment.changedFiles)) {
      const webuiLintTestJob = WebuiLintTestJob.createNx(dynamicConfig, environment);
      dynamicConfig.addJob(webuiLintTestJob);

      const consoleWebuiBuildJob = ConsoleWebuiBuildJob.create(dynamicConfig, environment);
      dynamicConfig.addJob(consoleWebuiBuildJob);

      const sonarCloudAnalysisJob = SonarCloudAnalysisJob.create(dynamicConfig, environment);
      dynamicConfig.addJob(sonarCloudAnalysisJob);

      jobs.push(
        new workflow.WorkflowJob(webuiLintTestJob, {
          name: 'Lint & test APIM Console',
          context: config.jobContext,
          'apim-ui-project-workdir': config.components.console.workdir,
          'nx-project': 'console',
          resource_class: 'xlarge',
          'max-workers': '7',
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
            'apim-project-workdir': config.components.console.workdir,
            'docker-context': '.',
            'docker-image-name': config.components.console.image,
          }),
        );
        requires.push('Build APIM Console docker image');
      }

      jobs.push(
        new workflow.WorkflowJob(sonarCloudAnalysisJob, {
          name: 'Sonar - gravitee-apim-console-webui',
          context: config.jobContext,
          requires: ['Lint & test APIM Console'],
          working_directory: config.components.console.project,
          cache_type: 'frontend',
        }),
      );
    }

    // Lint & Test APIM Portal Next
    if (!filterJobs || shouldBuildPortalNext(environment.changedFiles)) {
      const webuiLintTestJobNx = WebuiLintTestJob.createNx(dynamicConfig, environment);
      dynamicConfig.addJob(webuiLintTestJobNx);

      const sonarCloudAnalysisJob = SonarCloudAnalysisJob.create(dynamicConfig, environment);
      dynamicConfig.addJob(sonarCloudAnalysisJob);

      jobs.push(
        new workflow.WorkflowJob(webuiLintTestJobNx, {
          name: 'Lint & test APIM Portal Next',
          context: config.jobContext,
          'apim-ui-project-workdir': config.components.portal.next.project,
          'nx-project': 'portal-next',
          'max-workers': '2',
        }),
        new workflow.WorkflowJob(sonarCloudAnalysisJob, {
          name: 'Sonar - gravitee-apim-portal-webui-next',
          context: config.jobContext,
          requires: ['Lint & test APIM Portal Next'],
          working_directory: config.components.portal.next.project,
          cache_type: 'frontend',
        }),
      );
      requires.push('Lint & test APIM Portal Next');
    }

    if (!filterJobs || shouldBuildPortal(environment.changedFiles)) {
      const webuiLintTestJob = WebuiLintTestJob.create(dynamicConfig, environment);
      dynamicConfig.addJob(webuiLintTestJob);

      const sonarCloudAnalysisJob = SonarCloudAnalysisJob.create(dynamicConfig, environment);
      dynamicConfig.addJob(sonarCloudAnalysisJob);

      jobs.push(
        new workflow.WorkflowJob(webuiLintTestJob, {
          name: 'Lint & test APIM Portal',
          context: config.jobContext,
          'apim-ui-project': config.components.portal.project,
          'apim-ui-project-workdir': config.components.portal.workdir,
          resource_class: 'large',
        }),
      );
      requires.push('Lint & test APIM Portal');

      jobs.push(
        new workflow.WorkflowJob(sonarCloudAnalysisJob, {
          name: 'Sonar - gravitee-apim-portal-webui',
          context: config.jobContext,
          requires: ['Lint & test APIM Portal'],
          working_directory: config.components.portal.workdir,
          cache_type: 'frontend',
        }),
      );
    }

    if (!filterJobs || shouldBuildPortal(environment.changedFiles) || shouldBuildPortalNext(environment.changedFiles)) {
      const portalWebuiBuildJob = PortalWebuiBuildJob.create(dynamicConfig, environment);
      dynamicConfig.addJob(portalWebuiBuildJob);
      jobs.push(
        new workflow.WorkflowJob(portalWebuiBuildJob, {
          name: 'Build APIM Portal',
          context: config.jobContext,
        }),
      );

      requires.push('Build APIM Portal');

      if (shouldBuildDockerImages) {
        const buildDockerWebUiImageJob = BuildDockerWebUiImageJob.create(dynamicConfig, environment, false);
        dynamicConfig.addJob(buildDockerWebUiImageJob);

        jobs.push(
          new workflow.WorkflowJob(buildDockerWebUiImageJob, {
            context: config.jobContext,
            name: `Build APIM Portal docker image`,
            requires: ['Build APIM Portal'],
            'apim-project': config.components.portal.project,
            'apim-project-workdir': config.components.portal.workdir,
            'docker-context': '.',
            'docker-image-name': config.components.portal.image,
          }),
        );
        requires.push('Build APIM Portal docker image');
      }
    }

    if (!filterJobs || shouldBuildGammaUI(environment.changedFiles)) {
      const webuiLintTestJob = WebuiLintTestJob.createNx(dynamicConfig, environment);
      dynamicConfig.addJob(webuiLintTestJob);

      const gammaWebuiBuildJob = GammaWebuiBuildJob.create(dynamicConfig, environment);
      dynamicConfig.addJob(gammaWebuiBuildJob);

      jobs.push(
        new workflow.WorkflowJob(webuiLintTestJob, {
          name: 'Lint & test Gamma Console',
          context: config.jobContext,
          'apim-ui-project-workdir': config.components.gamma.workdir,
          'nx-project': 'gamma-console',
          resource_class: 'xlarge',
          'max-workers': '4',
        }),
        new workflow.WorkflowJob(gammaWebuiBuildJob, {
          name: 'Build Gamma Console',
          context: config.jobContext,
        }),
      );

      requires.push('Lint & test Gamma Console', 'Build Gamma Console');

      if (shouldBuildDockerImages) {
        const buildDockerWebUiImageJob = BuildDockerWebUiImageJob.create(dynamicConfig, environment, false);
        dynamicConfig.addJob(buildDockerWebUiImageJob);

        jobs.push(
          new workflow.WorkflowJob(buildDockerWebUiImageJob, {
            context: config.jobContext,
            name: `Build Gamma Console docker image`,
            requires: ['Build Gamma Console'],
            'apim-project': config.components.gamma.project,
            'apim-project-workdir': config.components.gamma.workdir,
            'docker-context': '.',
            'docker-image-name': config.components.gamma.image,
          }),
        );
        requires.push('Build Gamma Console docker image');
      }
    }

    // Force validation workflow in case only distribution pom.xml has changed
    if (!requires.includes('Build backend') && environment.changedFiles.some((file) => file.includes('gravitee-apim-distribution'))) {
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

  // FIPS product images built on support-branch/master PRs, pushed to the private Azure
  // registry. Java components use the java-fips base; the UIs (nginx) use the nginx-fips base.
  // Relies on the 'Build backend' / 'Build APIM Console|Portal', 'Build Gamma Console' jobs
  // from getCommonJobs.
}
