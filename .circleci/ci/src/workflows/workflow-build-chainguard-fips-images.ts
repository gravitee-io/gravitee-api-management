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
  BuildDockerChainguardFipsImageJob,
  ConsoleWebuiBuildJob,
  GammaWebuiBuildJob,
  PortalWebuiBuildJob,
  SetupJob,
} from '../jobs';
import { config } from '../config';

export class BuildChainguardFipsImagesWorkflow {
  static create(dynamicConfig: Config, environment: CircleCIEnvironment) {
    const setupJob = SetupJob.create(dynamicConfig);
    dynamicConfig.addJob(setupJob);
    const consoleWebuiBuildJob = ConsoleWebuiBuildJob.create(dynamicConfig, environment);
    dynamicConfig.addJob(consoleWebuiBuildJob);
    const portalWebuiBuildJob = PortalWebuiBuildJob.create(dynamicConfig, environment);
    dynamicConfig.addJob(portalWebuiBuildJob);
    const gammaWebuiBuildJob = GammaWebuiBuildJob.create(dynamicConfig, environment);
    dynamicConfig.addJob(gammaWebuiBuildJob);
    const backendBuildJob = BackendBuildAndPublishOnDownloadWebsiteJob.create(dynamicConfig, environment, false);
    dynamicConfig.addJob(backendBuildJob);
    // isProd=false → azurecr with branch tags: this on-demand action is the test build.
    const buildDockerChainguardFipsImageJob = BuildDockerChainguardFipsImageJob.create(dynamicConfig, environment, false);
    dynamicConfig.addJob(buildDockerChainguardFipsImageJob);

    const jobs = [
      new workflow.WorkflowJob(setupJob, { context: config.jobContext, name: 'Setup' }),
      // APIM Portal
      new workflow.WorkflowJob(portalWebuiBuildJob, {
        context: config.jobContext,
        name: 'Build APIM Portal',
        requires: ['Setup'],
      }),
      new workflow.WorkflowJob(buildDockerChainguardFipsImageJob, {
        context: config.jobContext,
        name: `Build APIM Portal chainguard-fips docker image for APIM ${environment.graviteeioVersion}`,
        requires: ['Build APIM Portal'],
        'apim-project': config.components.portal.project,
        'apim-project-workdir': config.components.portal.workdir,
        'docker-context': '.',
        'docker-image-name': config.components.portal.image,
        'docker-fips-base-image': config.docker.fipsNginxBaseImage,
      }),

      // APIM Console
      new workflow.WorkflowJob(consoleWebuiBuildJob, {
        context: config.jobContext,
        name: 'Build APIM Console',
        requires: ['Setup'],
      }),
      new workflow.WorkflowJob(buildDockerChainguardFipsImageJob, {
        context: config.jobContext,
        name: `Build APIM Console chainguard-fips docker image for APIM ${environment.graviteeioVersion}`,
        requires: ['Build APIM Console'],
        'apim-project': config.components.console.project,
        'apim-project-workdir': config.components.console.workdir,
        'docker-context': '.',
        'docker-image-name': config.components.console.image,
        'docker-fips-base-image': config.docker.fipsNginxBaseImage,
      }),

      // Gamma Console
      new workflow.WorkflowJob(gammaWebuiBuildJob, {
        context: config.jobContext,
        name: 'Build Gamma Console',
        requires: ['Setup'],
      }),
      new workflow.WorkflowJob(buildDockerChainguardFipsImageJob, {
        context: config.jobContext,
        name: `Build Gamma Console chainguard-fips docker image for APIM ${environment.graviteeioVersion}`,
        requires: ['Build Gamma Console'],
        'apim-project': config.components.gamma.project,
        'apim-project-workdir': config.components.gamma.workdir,
        'docker-context': '.',
        'docker-image-name': config.components.gamma.image,
        'docker-fips-base-image': config.docker.fipsNginxBaseImage,
      }),

      // APIM Backend (Java)
      new workflow.WorkflowJob(backendBuildJob, {
        context: config.jobContext,
        name: 'Backend build',
        requires: ['Setup'],
      }),
      new workflow.WorkflowJob(buildDockerChainguardFipsImageJob, {
        context: config.jobContext,
        name: `Build APIM Management API chainguard-fips docker image for APIM ${environment.graviteeioVersion}`,
        requires: ['Backend build'],
        'apim-project': config.components.managementApi.project,
        'apim-project-workdir': config.components.managementApi.workdir,
        'docker-context': 'gravitee-apim-rest-api-standalone/gravitee-apim-rest-api-standalone-distribution/target',
        'docker-image-name': config.components.managementApi.image,
        'docker-fips-base-image': config.docker.fipsJavaBaseImage,
      }),
      new workflow.WorkflowJob(buildDockerChainguardFipsImageJob, {
        context: config.jobContext,
        name: `Build APIM Gateway chainguard-fips docker image for APIM ${environment.graviteeioVersion}`,
        requires: ['Backend build'],
        'apim-project': config.components.gateway.project,
        'apim-project-workdir': config.components.gateway.workdir,
        'docker-context': 'gravitee-apim-gateway-standalone/gravitee-apim-gateway-standalone-distribution/target',
        'docker-image-name': config.components.gateway.image,
        'docker-fips-base-image': config.docker.fipsJavaBaseImage,
      }),
    ];

    return new Workflow('build-chainguard-fips-images', jobs);
  }
}
