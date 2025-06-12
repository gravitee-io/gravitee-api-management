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
  BackendBuildAndPublishOnDownloadWebsiteJob,
  BuildDockerBackendImageJob,
  BuildDockerWebUiImageJob,
  ConsoleWebuiBuildJob,
  NexusStagingJob,
  PackageBundleJob,
  PortalWebuiBuildJob,
  PublishRpmPackagesJob,
  ReleaseCommitAndPrepareNextVersionJob,
  ReleaseHelmJob,
  ReleaseNotesApimJob,
  SetupJob,
  SlackAnnouncementJob,
  TriggerSaasDockerImagesJob,
} from '../jobs';
import { CircleCIEnvironment } from '../pipelines';
import { config } from '../config';

export class FullReleaseWorkflow {
  private static workflowName = 'full_release';

  static create(dynamicConfig: Config, environment: CircleCIEnvironment) {
    const setupJob = SetupJob.create(dynamicConfig);
    dynamicConfig.addJob(setupJob);

    const slackAnnouncementJob = SlackAnnouncementJob.create(dynamicConfig);
    dynamicConfig.addJob(slackAnnouncementJob);

    const consoleWebuiBuildJob = ConsoleWebuiBuildJob.create(dynamicConfig, environment);
    dynamicConfig.addJob(consoleWebuiBuildJob);

    const portalWebuiBuildJob = PortalWebuiBuildJob.create(dynamicConfig, environment);
    dynamicConfig.addJob(portalWebuiBuildJob);

    const buildDockerWebUiImageJob = BuildDockerWebUiImageJob.create(dynamicConfig, environment, true);
    dynamicConfig.addJob(buildDockerWebUiImageJob);
    const buildDockerBackendImageJob = BuildDockerBackendImageJob.create(dynamicConfig, environment, true);
    dynamicConfig.addJob(buildDockerBackendImageJob);

    const backendBuildAndPublishOnDownloadWebsiteJob = BackendBuildAndPublishOnDownloadWebsiteJob.create(dynamicConfig, environment, true);
    dynamicConfig.addJob(backendBuildAndPublishOnDownloadWebsiteJob);

    const releaseCommitAndPrepareNextVersionJob = ReleaseCommitAndPrepareNextVersionJob.create(dynamicConfig, environment);
    dynamicConfig.addJob(releaseCommitAndPrepareNextVersionJob);

    const packageBundleJob = PackageBundleJob.create(dynamicConfig, environment.graviteeioVersion, environment.isDryRun);
    dynamicConfig.addJob(packageBundleJob);

    const releaseHelmJob = ReleaseHelmJob.create(dynamicConfig, environment);
    dynamicConfig.addJob(releaseHelmJob);

    const publishRpmPackagesJob = PublishRpmPackagesJob.create(dynamicConfig, environment);
    dynamicConfig.addJob(publishRpmPackagesJob);

    const releaseNoteApimJob = ReleaseNotesApimJob.create(dynamicConfig, environment);
    dynamicConfig.addJob(releaseNoteApimJob);

    const nexusStagingJob = NexusStagingJob.create(dynamicConfig, environment);
    dynamicConfig.addJob(nexusStagingJob);

    const runTriggerSaasDockerImagesJob = TriggerSaasDockerImagesJob.create(environment, 'prod');
    dynamicConfig.addJob(runTriggerSaasDockerImagesJob);

    return new Workflow(FullReleaseWorkflow.workflowName, [
      // PREPARE
      new workflow.WorkflowJob(setupJob, { context: config.jobContext, name: 'Setup' }),
      new workflow.WorkflowJob(slackAnnouncementJob, {
        context: config.jobContext,
        name: 'Announce release is starting',
        message: `🚀 Starting APIM ${environment.graviteeioVersion} release!`,
      }),

      // APIM Portal
      new workflow.WorkflowJob(portalWebuiBuildJob, {
        context: config.jobContext,
        name: 'Build APIM Portal',
        requires: ['Setup'],
      }),
      new workflow.WorkflowJob(buildDockerWebUiImageJob, {
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
      new workflow.WorkflowJob(buildDockerWebUiImageJob, {
        context: config.jobContext,
        name: `Build APIM Console docker image for APIM ${environment.graviteeioVersion}${environment.isDryRun ? ' - Dry Run' : ''}`,
        requires: ['Build APIM Console'],
        'apim-project': config.components.console.project,
        'docker-context': '.',
        'docker-image-name': config.components.console.image,
      }),

      // APIM Backend
      new workflow.WorkflowJob(backendBuildAndPublishOnDownloadWebsiteJob, {
        context: config.jobContext,
        name: 'Backend build and publish on download website',
        requires: ['Setup'],
      }),
      new workflow.WorkflowJob(buildDockerBackendImageJob, {
        context: config.jobContext,
        name: `Build APIM Management API docker image for APIM ${environment.graviteeioVersion}${environment.isDryRun ? ' - Dry Run' : ''}`,
        requires: ['Backend build and publish on download website'],
        'apim-project': config.components.managementApi.project,
        'docker-context': 'gravitee-apim-rest-api-standalone/gravitee-apim-rest-api-standalone-distribution/target',
        'docker-image-name': config.components.managementApi.image,
      }),
      new workflow.WorkflowJob(buildDockerBackendImageJob, {
        context: config.jobContext,
        name: `Build APIM Gateway docker image for APIM ${environment.graviteeioVersion}${environment.isDryRun ? ' - Dry Run' : ''}`,
        requires: ['Backend build and publish on download website'],
        'apim-project': config.components.gateway.project,
        'docker-context': 'gravitee-apim-gateway-standalone/gravitee-apim-gateway-standalone-distribution/target',
        'docker-image-name': config.components.gateway.image,
      }),

      // Commit and set next version
      new workflow.WorkflowJob(releaseCommitAndPrepareNextVersionJob, {
        context: config.jobContext,
        name: 'Commit and prepare next version',
        requires: ['Backend build and publish on download website', 'Build APIM Console', 'Build APIM Portal'],
      }),

      // Package bundle
      new workflow.WorkflowJob(packageBundleJob, {
        context: config.jobContext,
        name: 'Package bundle',
        requires: ['Commit and prepare next version'],
      }),

      // Publish RPM Packages
      new workflow.WorkflowJob(publishRpmPackagesJob, {
        context: config.jobContext,
        name: `Build and push RPM packages for APIM ${environment.graviteeioVersion}${environment.isDryRun ? ' - Dry Run' : ''}`,
        requires: ['Package bundle'],
      }),

      // Trigger SaaS Docker images creation
      new workflow.WorkflowJob(runTriggerSaasDockerImagesJob, {
        context: [...config.jobContext, 'keeper-orb-publishing'],
        name: 'Trigger SaaS Docker images creation',
        requires: [
          `Build APIM Portal docker image for APIM ${environment.graviteeioVersion}${environment.isDryRun ? ' - Dry Run' : ''}`,
          `Build APIM Console docker image for APIM ${environment.graviteeioVersion}${environment.isDryRun ? ' - Dry Run' : ''}`,
          `Build APIM Management API docker image for APIM ${environment.graviteeioVersion}${environment.isDryRun ? ' - Dry Run' : ''}`,
          `Build APIM Gateway docker image for APIM ${environment.graviteeioVersion}${environment.isDryRun ? ' - Dry Run' : ''}`,
        ],
      }),

      // Nexus staging
      new workflow.WorkflowJob(nexusStagingJob, {
        context: config.jobContext,
        name: 'Nexus staging',
        requires: ['Trigger SaaS Docker images creation'],
      }),

      // Release Helm chart
      new workflow.WorkflowJob(releaseHelmJob, {
        context: config.jobContext,
        name: 'Release Helm Chart',
        requires: ['Trigger SaaS Docker images creation'],
      }),

      // Create Release note pull request
      new workflow.WorkflowJob(releaseNoteApimJob, {
        context: config.jobContext,
        name: 'Create release note pull request',
        requires: [
          'Release Helm Chart',
          `Build and push RPM packages for APIM ${environment.graviteeioVersion}${environment.isDryRun ? ' - Dry Run' : ''}`,
        ],
      }),

      // Notify APIM team
      new workflow.WorkflowJob(slackAnnouncementJob, {
        context: config.jobContext,
        name: 'Announce release is completed',
        message: `🎆 APIM - ${environment.graviteeioVersion} released!`,
        requires: [
          'Nexus staging',
          'Release Helm Chart',
          `Build and push RPM packages for APIM ${environment.graviteeioVersion}${environment.isDryRun ? ' - Dry Run' : ''}`,
        ],
      }),
    ]);
  }
}
