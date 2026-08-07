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
import { BuildDockerChainguardImageJob } from '../../jobs';
import { CircleCIEnvironment } from '../../pipelines';
import { config } from '../../config';

/**
 * The chainguard component images, published to Docker Hub alongside the alpine and debian
 * variants. The development environment does not run them, so they are not part of refreshing
 * it — but they are shipped, so something has to build and scan them regularly.
 */
export function chainguardImageJobs(dynamicConfig: Config, environment: CircleCIEnvironment): workflow.WorkflowJob[] {
  const buildDockerChainguardImageJob = BuildDockerChainguardImageJob.create(dynamicConfig, environment, false);
  dynamicConfig.addJob(buildDockerChainguardImageJob);

  return [
    new workflow.WorkflowJob(buildDockerChainguardImageJob, {
      context: config.jobContext,
      name: `Build APIM Management API chainguard docker image`,
      requires: ['Build backend'],
      'apim-project': config.components.managementApi.project,
      'apim-project-workdir': config.components.managementApi.distribution,
      'docker-context': 'target',
      'docker-image-name': config.components.managementApi.image,
    }),
    new workflow.WorkflowJob(buildDockerChainguardImageJob, {
      context: config.jobContext,
      name: `Build APIM Gateway chainguard docker image`,
      requires: ['Build backend'],
      'apim-project': config.components.gateway.project,
      'apim-project-workdir': config.components.gateway.distribution,
      'docker-context': 'target',
      'docker-image-name': config.components.gateway.image,
    }),
    new workflow.WorkflowJob(buildDockerChainguardImageJob, {
      context: config.jobContext,
      name: `Build APIM Console chainguard docker image`,
      requires: ['Build APIM Console'],
      'apim-project': config.components.console.project,
      'apim-project-workdir': config.components.console.workdir,
      'docker-context': '.',
      'docker-image-name': config.components.console.image,
    }),
    new workflow.WorkflowJob(buildDockerChainguardImageJob, {
      context: config.jobContext,
      name: `Build APIM Portal chainguard docker image`,
      requires: ['Build APIM Portal'],
      'apim-project': config.components.portal.project,
      'apim-project-workdir': config.components.portal.workdir,
      'docker-context': '.',
      'docker-image-name': config.components.portal.image,
    }),
    new workflow.WorkflowJob(buildDockerChainguardImageJob, {
      context: config.jobContext,
      name: `Build Gamma Console chainguard docker image`,
      requires: ['Build Gamma Console'],
      'apim-project': config.components.gamma.project,
      'apim-project-workdir': config.components.gamma.workdir,
      'docker-context': '.',
      'docker-image-name': config.components.gamma.image,
    }),
  ];
}
