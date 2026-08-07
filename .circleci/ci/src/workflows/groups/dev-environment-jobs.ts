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
  BuildDockerWebUiImageJob,
  ConsoleWebuiBuildJob,
  GammaWebuiBuildJob,
  PortalWebuiBuildJob,
  SetupJob,
} from '../../jobs';
import { CircleCIEnvironment } from '../../pipelines';
import { config } from '../../config';
import { backendImageJobs } from './backend-image-jobs';

/**
 * Everything needed to refresh a branch's development environment: the engine, the three web
 * UIs, and the five docker images the cluster runs.
 *
 * No test job. A push to a branch and the scheduled build both need exactly this set — the
 * scheduled build then adds the suites that run nowhere else — and pull requests own their own
 * verification, on the scope the changed-files predicates work out.
 */
export function devEnvironmentJobs(dynamicConfig: Config, environment: CircleCIEnvironment): workflow.WorkflowJob[] {
  const setupJob = SetupJob.create(dynamicConfig);
  dynamicConfig.addJob(setupJob);

  const buildBackendJob = BuildBackendJob.create(dynamicConfig, environment);
  dynamicConfig.addJob(buildBackendJob);

  const consoleWebuiBuildJob = ConsoleWebuiBuildJob.create(dynamicConfig, environment);
  dynamicConfig.addJob(consoleWebuiBuildJob);

  const portalWebuiBuildJob = PortalWebuiBuildJob.create(dynamicConfig, environment);
  dynamicConfig.addJob(portalWebuiBuildJob);

  const gammaWebuiBuildJob = GammaWebuiBuildJob.create(dynamicConfig, environment);
  dynamicConfig.addJob(gammaWebuiBuildJob);

  const buildDockerWebUiImageJob = BuildDockerWebUiImageJob.create(dynamicConfig, environment, false);
  dynamicConfig.addJob(buildDockerWebUiImageJob);

  return [
    new workflow.WorkflowJob(setupJob, { name: 'Setup', context: config.jobContext }),
    new workflow.WorkflowJob(buildBackendJob, {
      name: 'Build backend',
      context: config.jobContext,
      requires: ['Setup'],
    }),

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
  ];
}
