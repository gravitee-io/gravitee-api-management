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
import { Config, Workflow, workflow } from '@circleci/circleci-config-sdk';
import { CircleCIEnvironment } from '../pipelines';
import {
  BackendBuildAndPublishOnDownloadWebsiteJob,
  BuildDockerImageJob,
  ConsoleWebuiBuildJob,
  PortalWebuiBuildJob,
  SetupJob,
} from '../jobs';
import { config } from '../config';

export class BuildDockerImagesWorkflow {
  static create(dynamicConfig: Config, environment: CircleCIEnvironment) {
    const setupJob = SetupJob.create(dynamicConfig);
    dynamicConfig.addJob(setupJob);
    const consoleWebuiBuildJob = ConsoleWebuiBuildJob.create(dynamicConfig, environment, false);
    dynamicConfig.addJob(consoleWebuiBuildJob);
    const portalWebuiBuildJob = PortalWebuiBuildJob.create(dynamicConfig, environment, false);
    dynamicConfig.addJob(portalWebuiBuildJob);
    const backendBuildJob = BackendBuildAndPublishOnDownloadWebsiteJob.create(dynamicConfig, environment, false);
    dynamicConfig.addJob(backendBuildJob);
    const buildDockerImageJob = BuildDockerImageJob.create(dynamicConfig, environment, true);
    dynamicConfig.addJob(buildDockerImageJob);

    const jobs = [
      new workflow.WorkflowJob(setupJob, { context: config.jobContext, name: 'Setup' }),
      // APIM Portal
      new workflow.WorkflowJob(portalWebuiBuildJob, {
        context: config.jobContext,
        name: 'Build APIM Portal',
        requires: ['Setup'],
      }),
      new workflow.WorkflowJob(buildDockerImageJob, {
        context: config.jobContext,
        name: `Build APIM Portal docker image for APIM ${environment.graviteeioVersion}${environment.isDryRun ? ' - Dry Run' : ''}`,
        requires: ['Build APIM Portal'],
        'apim-project': config.components.portal.project,
        'docker-context': '.',
        'docker-image-name': config.components.portal.image,
      }),

      // APIM Console
      new workflow.WorkflowJob(consoleWebuiBuildJob, {
        context: config.jobContext,
        name: 'Build APIM Console',
        requires: ['Setup'],
      }),
      new workflow.WorkflowJob(buildDockerImageJob, {
        context: config.jobContext,
        name: `Build APIM Console docker image for APIM ${environment.graviteeioVersion}${environment.isDryRun ? ' - Dry Run' : ''}`,
        requires: ['Build APIM Console'],
        'apim-project': config.components.console.project,
        'docker-context': '.',
        'docker-image-name': config.components.console.image,
      }),

      // APIM Backend
      new workflow.WorkflowJob(backendBuildJob, {
        context: config.jobContext,
        name: 'Backend build',
        requires: ['Setup'],
      }),
      new workflow.WorkflowJob(buildDockerImageJob, {
        context: config.jobContext,
        name: `Build APIM Management API docker image for APIM ${environment.graviteeioVersion}${environment.isDryRun ? ' - Dry Run' : ''}`,
        requires: ['Backend build'],
        'apim-project': config.components.managementApi.project,
        'docker-context': 'gravitee-apim-rest-api-standalone/gravitee-apim-rest-api-standalone-distribution/target',
        'docker-image-name': config.components.managementApi.image,
      }),
      new workflow.WorkflowJob(buildDockerImageJob, {
        context: config.jobContext,
        name: `Build APIM Gateway docker image for APIM ${environment.graviteeioVersion}${environment.isDryRun ? ' - Dry Run' : ''}`,
        requires: ['Backend build'],
        'apim-project': config.components.gateway.project,
        'docker-context': 'gravitee-apim-gateway-standalone/gravitee-apim-gateway-standalone-distribution/target',
        'docker-image-name': config.components.gateway.image,
      })
    ];

    return new Workflow('build-rpm-&-docker-images', jobs);
  }
}
