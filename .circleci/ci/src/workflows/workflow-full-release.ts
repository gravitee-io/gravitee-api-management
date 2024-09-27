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
  PackageBundleJob,
  PublishProdDockerImagesJob,
  PublishRpmPackagesJob,
  ReleaseCommitAndPrepareNextVersionJob,
  ReleaseNotesApimJob,
  SetupJob,
  SlackAnnouncementJob,
  WebuiBuildJob,
  WebuiPublishArtifactoryJob,
} from '../jobs';
import { CircleCIEnvironment } from '../pipelines';
import { config } from '../config';
import { BackendBuildAndPublishOnArtifactoryJob, NexusStagingJob } from '../jobs/backend';
import { ReleaseHelmJob } from '../jobs/helm';

export class FullReleaseWorkflow {
  private static workflowName = 'full_release';

  static create(dynamicConfig: Config, environment: CircleCIEnvironment) {
    const setupJob = SetupJob.create(dynamicConfig);
    dynamicConfig.addJob(setupJob);

    const slackAnnouncementJob = SlackAnnouncementJob.create(dynamicConfig);
    dynamicConfig.addJob(slackAnnouncementJob);

    const webuiBuildJob = WebuiBuildJob.create(dynamicConfig, environment);
    dynamicConfig.addJob(webuiBuildJob);

    const webuiPublishArtifactoryJob = WebuiPublishArtifactoryJob.create(dynamicConfig, environment);
    dynamicConfig.addJob(webuiPublishArtifactoryJob);

    const backendBuildAndPublishOnArtifactoryJob = BackendBuildAndPublishOnArtifactoryJob.create(dynamicConfig, environment);
    dynamicConfig.addJob(backendBuildAndPublishOnArtifactoryJob);

    const releaseCommitAndPrepareNextVersionJob = ReleaseCommitAndPrepareNextVersionJob.create(dynamicConfig, environment);
    dynamicConfig.addJob(releaseCommitAndPrepareNextVersionJob);

    const packageBundleJob = PackageBundleJob.create(dynamicConfig, environment.graviteeioVersion, environment.isDryRun);
    dynamicConfig.addJob(packageBundleJob);

    const releaseHelmJob = ReleaseHelmJob.create(dynamicConfig, environment);
    dynamicConfig.addJob(releaseHelmJob);

    const publishProdDockerImagesJob = PublishProdDockerImagesJob.create(dynamicConfig, environment);
    dynamicConfig.addJob(publishProdDockerImagesJob);

    const publishRpmPackagesJob = PublishRpmPackagesJob.create(dynamicConfig, environment);
    dynamicConfig.addJob(publishRpmPackagesJob);

    const releaseNoteApimJob = ReleaseNotesApimJob.create(dynamicConfig, environment);
    dynamicConfig.addJob(releaseNoteApimJob);

    const nexusStagingJob = NexusStagingJob.create(dynamicConfig, environment);
    dynamicConfig.addJob(nexusStagingJob);

    return new Workflow(FullReleaseWorkflow.workflowName, [
      // PREPARE
      new workflow.WorkflowJob(setupJob, { context: config.jobContext, name: 'Setup' }),
      new workflow.WorkflowJob(slackAnnouncementJob, {
        context: config.jobContext,
        name: 'Announce release is starting',
        message: `🚀 Starting APIM ${environment.graviteeioVersion} release!`,
      }),

      // APIM Portal
      new workflow.WorkflowJob(webuiBuildJob, {
        context: config.jobContext,
        name: 'Build APIM Portal and publish image',
        'apim-ui-project': config.dockerImages.portal.project,
        'docker-image-name': config.dockerImages.portal.image,
        node_version: '20.9',
        requires: ['Setup'],
      }),
      new workflow.WorkflowJob(webuiPublishArtifactoryJob, {
        context: config.jobContext,
        name: 'Publish APIM Portal to artifactory',
        'apim-ui-project': config.dockerImages.portal.project,
        requires: ['Build APIM Portal and publish image'],
      }),

      // APIM Console
      new workflow.WorkflowJob(webuiBuildJob, {
        context: config.jobContext,
        name: 'Build APIM Console and publish image',
        'apim-ui-project': config.dockerImages.console.project,
        'docker-image-name': config.dockerImages.console.image,
        requires: ['Setup'],
      }),
      new workflow.WorkflowJob(webuiPublishArtifactoryJob, {
        context: config.jobContext,
        name: 'Publish APIM Console to artifactory',
        'apim-ui-project': config.dockerImages.console.project,
        requires: ['Build APIM Console and publish image'],
      }),

      // APIM Backend
      new workflow.WorkflowJob(backendBuildAndPublishOnArtifactoryJob, {
        context: config.jobContext,
        name: 'Backend build and publish to artifactory',
        requires: ['Setup'],
      }),

      // Commit and set next version
      new workflow.WorkflowJob(releaseCommitAndPrepareNextVersionJob, {
        context: config.jobContext,
        name: 'Commit and prepare next version',
        requires: ['Backend build and publish to artifactory', 'Publish APIM Console to artifactory', 'Publish APIM Portal to artifactory'],
      }),

      // Package bundle
      new workflow.WorkflowJob(packageBundleJob, {
        context: config.jobContext,
        name: 'Package bundle',
        requires: ['Commit and prepare next version'],
      }),

      // Release Helm chart
      new workflow.WorkflowJob(releaseHelmJob, {
        context: config.jobContext,
        name: 'Release Helm Chart',
        requires: ['Package bundle'],
      }),

      // Publish Docker images
      new workflow.WorkflowJob(publishProdDockerImagesJob, {
        context: config.jobContext,
        name: `Build and push docker images for APIM ${environment.graviteeioVersion}${environment.isDryRun ? ' - Dry Run' : ''}`,
        requires: ['Package bundle'],
      }),

      // Publish RPM Packages
      new workflow.WorkflowJob(publishRpmPackagesJob, {
        context: config.jobContext,
        name: `Build and push RPM packages for APIM ${environment.graviteeioVersion}${environment.isDryRun ? ' - Dry Run' : ''}`,
        requires: ['Package bundle'],
      }),

      // Create Release note pull request
      new workflow.WorkflowJob(releaseNoteApimJob, {
        context: config.jobContext,
        name: 'Create release note pull request',
        requires: ['Package bundle'],
      }),

      // Nexus staging
      new workflow.WorkflowJob(nexusStagingJob, {
        context: config.jobContext,
        name: 'Nexus staging',
        requires: ['Package bundle'],
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
