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
import { BuildBackendImagesJob, BuildBackendJob, ConsoleWebuiBuildJob, PortalWebuiBuildJob, SetupJob } from '../jobs';
import { config } from '../config';
import { CircleCIEnvironment } from '../pipelines';
import { PublishPrDockerImagesJob } from '../jobs/job-publish-pr-docker-images';

export class PublishDockerImagesWorkflow {
  static create(dynamicConfig: Config, environment: CircleCIEnvironment) {
    const setupJob = SetupJob.create(dynamicConfig);
    dynamicConfig.addJob(setupJob);

    const buildBackendJob = BuildBackendJob.create(dynamicConfig, environment);
    dynamicConfig.addJob(buildBackendJob);

    const buildBackendImagesJob = BuildBackendImagesJob.create(dynamicConfig, environment);
    dynamicConfig.addJob(buildBackendImagesJob);

    const consoleWebuiBuildJob = ConsoleWebuiBuildJob.create(dynamicConfig, environment, true, false);
    dynamicConfig.addJob(consoleWebuiBuildJob);

    const portalWebuiBuildJob = PortalWebuiBuildJob.create(dynamicConfig, environment, true, false);
    dynamicConfig.addJob(portalWebuiBuildJob);

    const publishPrDockerImagesJob = PublishPrDockerImagesJob.create(dynamicConfig, environment);
    dynamicConfig.addJob(publishPrDockerImagesJob);

    const jobs = [
      new workflow.WorkflowJob(setupJob, { context: config.jobContext, name: 'Setup' }),
      new workflow.WorkflowJob(buildBackendJob, { context: config.jobContext, requires: ['Setup'], name: 'Build backend' }),
      new workflow.WorkflowJob(buildBackendImagesJob, {
        context: config.jobContext,
        requires: ['Build backend'],
        name: 'Build and push rest api and gateway images',
      }),
      new workflow.WorkflowJob(consoleWebuiBuildJob, {
        context: config.jobContext,
        requires: ['Setup'],
        name: 'Build APIM Console and publish image',
      }),
      new workflow.WorkflowJob(portalWebuiBuildJob, {
        context: config.jobContext,
        requires: ['Setup'],
        name: 'Build APIM Portal and publish image',
      }),
      new workflow.WorkflowJob(publishPrDockerImagesJob, {
        name: 'Publish docker Images in Github PR',
        context: config.jobContext,
        requires: [
          'Build and push rest api and gateway images',
          'Build APIM Console and publish image',
          'Build APIM Portal and publish image',
        ],
      }),
    ];

    return new Workflow('publish_docker_images', jobs);
  }
}
