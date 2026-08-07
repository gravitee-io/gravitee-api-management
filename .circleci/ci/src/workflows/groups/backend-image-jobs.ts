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
import { BuildDockerBackendImageJob } from '../../jobs';
import { CircleCIEnvironment } from '../../pipelines';
import { config } from '../../config';

/**
 * The Management API and Gateway docker images.
 *
 * These used to live inside the e2e job group, because e2e was the only thing consuming them.
 * They are a group of their own: the dev-environment deployment needs them just as much, and
 * a workflow that updates the environment without running e2e must still be able to ask for
 * them. Keep them here rather than folding them back into a caller.
 */
export function backendImageJobs(dynamicConfig: Config, environment: CircleCIEnvironment): workflow.WorkflowJob[] {
  const buildDockerBackendImageJob = BuildDockerBackendImageJob.create(dynamicConfig, environment, false);
  dynamicConfig.addJob(buildDockerBackendImageJob);

  return [
    new workflow.WorkflowJob(buildDockerBackendImageJob, {
      context: config.jobContext,
      name: `Build APIM Management API docker image`,
      requires: ['Build backend'],
      'apim-project': config.components.managementApi.project,
      'apim-project-workdir': config.components.managementApi.distribution,
      'docker-context': 'target',
      'docker-image-name': config.components.managementApi.image,
    }),
    new workflow.WorkflowJob(buildDockerBackendImageJob, {
      context: config.jobContext,
      name: `Build APIM Gateway docker image`,
      requires: ['Build backend'],
      'apim-project': config.components.gateway.project,
      'apim-project-workdir': config.components.gateway.distribution,
      'docker-context': 'target',
      'docker-image-name': config.components.gateway.image,
    }),
  ];
}
