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
import { Config, Job, workflow } from '../../circleci-config';
import {
  BuildDockerWebUiImageJob,
  ConsoleWebuiBuildJob,
  GammaWebuiBuildJob,
  NxFormatCheckJob,
  PortalWebuiBuildJob,
  SonarCloudAnalysisJob,
  WebuiLintTestJob,
} from '../../jobs';
import { CircleCIEnvironment } from '../../pipelines';
import { config } from '../../config';
import { shouldBuildConsole, shouldBuildGammaUI, shouldBuildPortal, shouldBuildPortalNext, shouldBuildWebuiLibs } from './changed-files';
import { analysisJobFor, FRONTEND_SUITES, Suite, suiteJobFor, suiteNamed } from './analysed-projects';

/**
 * The frontend projects: prettier check, lint and test, builds, and their docker images.
 *
 * Same contract as `backendJobs` — see the note there about the two accumulated lists.
 */
export function frontendJobs(
  dynamicConfig: Config,
  environment: CircleCIEnvironment,
  filterJobs: boolean,
  shouldBuildDockerImages: boolean,
): { jobs: workflow.WorkflowJob[]; requires: string[] } {
  const jobs: workflow.WorkflowJob[] = [];
  const requires: string[] = [];

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
    const consoleWebuiBuildJob = ConsoleWebuiBuildJob.create(dynamicConfig, environment);
    dynamicConfig.addJob(consoleWebuiBuildJob);

    jobs.push(
      suiteJobFor(suiteNamed('Lint & test APIM Console'), dynamicConfig, environment),
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
  }

  // Lint & Test APIM Portal Next
  if (!filterJobs || shouldBuildPortalNext(environment.changedFiles)) {
    jobs.push(suiteJobFor(suiteNamed('Lint & test APIM Portal Next'), dynamicConfig, environment));
    requires.push('Lint & test APIM Portal Next');
  }

  if (!filterJobs || shouldBuildPortal(environment.changedFiles)) {
    jobs.push(suiteJobFor(suiteNamed('Lint & test APIM Portal'), dynamicConfig, environment));
    requires.push('Lint & test APIM Portal');
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

  // Created on the first project that survives the predicate, so a pipeline analysing nothing
  // emits no orphan analysis job.
  let sonarAnalysisJob: Job | undefined;

  FRONTEND_SUITES.forEach((project: Suite) => {
    if (filterJobs && !project.predicate(environment.changedFiles)) {
      return;
    }
    if (!sonarAnalysisJob) {
      sonarAnalysisJob = SonarCloudAnalysisJob.create(dynamicConfig, environment);
      dynamicConfig.addJob(sonarAnalysisJob);
    }
    jobs.push(analysisJobFor(project, sonarAnalysisJob));
  });

  return { jobs, requires };
}
