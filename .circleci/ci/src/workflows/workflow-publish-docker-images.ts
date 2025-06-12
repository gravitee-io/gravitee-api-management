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
  BuildBackendJob,
  BuildDockerBackendImageJob,
  BuildDockerWebUiImageJob,
  ConsoleWebuiBuildJob,
  PortalWebuiBuildJob,
  PublishPrEnvUrlsJob,
  SetupJob,
} from '../jobs';
import { config } from '../config';
import { CircleCIEnvironment } from '../pipelines';

export class PublishDockerImagesWorkflow {
  static create(dynamicConfig: Config, environment: CircleCIEnvironment) {
    const setupJob = SetupJob.create(dynamicConfig);
    dynamicConfig.addJob(setupJob);

    const buildBackendJob = BuildBackendJob.create(dynamicConfig, environment);
    dynamicConfig.addJob(buildBackendJob);

    const consoleWebuiBuildJob = ConsoleWebuiBuildJob.create(dynamicConfig, environment);
    dynamicConfig.addJob(consoleWebuiBuildJob);

    const portalWebuiBuildJob = PortalWebuiBuildJob.create(dynamicConfig, environment);
    dynamicConfig.addJob(portalWebuiBuildJob);

    const buildDockerWebUiImageJob = BuildDockerWebUiImageJob.create(dynamicConfig, environment, false);
    dynamicConfig.addJob(buildDockerWebUiImageJob);
    const buildDockerBackendImageJob = BuildDockerBackendImageJob.create(dynamicConfig, environment, false);
    dynamicConfig.addJob(buildDockerBackendImageJob);

    const publishPrEnvUrlsJob = PublishPrEnvUrlsJob.create(dynamicConfig, environment);
    dynamicConfig.addJob(publishPrEnvUrlsJob);

    const jobs = [
      new workflow.WorkflowJob(setupJob, { context: config.jobContext, name: 'Setup' }),
      new workflow.WorkflowJob(buildBackendJob, { context: config.jobContext, requires: ['Setup'], name: 'Build backend' }),

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

      new workflow.WorkflowJob(consoleWebuiBuildJob, {
        context: config.jobContext,
        requires: ['Setup'],
        name: 'Build APIM Console',
      }),
      new workflow.WorkflowJob(buildDockerWebUiImageJob, {
        context: config.jobContext,
        name: `Build APIM Console docker image`,
        requires: ['Build APIM Console'],
        'apim-project': config.components.console.project,
        'docker-context': '.',
        'docker-image-name': config.components.console.image,
      }),

      new workflow.WorkflowJob(portalWebuiBuildJob, {
        context: config.jobContext,
        requires: ['Setup'],
        name: 'Build APIM Portal',
      }),
      new workflow.WorkflowJob(buildDockerWebUiImageJob, {
        context: config.jobContext,
        name: `Build APIM Portal docker image`,
        requires: ['Build APIM Portal'],
        'apim-project': config.components.portal.project,
        'docker-context': '.',
        'docker-image-name': config.components.portal.image,
      }),

      new workflow.WorkflowJob(publishPrEnvUrlsJob, {
        name: 'Publish environment URLs in Github PR',
        context: config.jobContext,
        requires: [
          'Build APIM Management API docker image',
          'Build APIM Gateway docker image',
          'Build APIM Console docker image',
          'Build APIM Portal docker image',
        ],
      }),
    ];

    return new Workflow('publish_docker_images', jobs);
  }
}
