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
  ConsoleWebuiBuildJob,
  PortalWebuiBuildJob,
  ReleaseCommitAndPrepareNextVersionJob,
  SetupJob,
  SlackAnnouncementJob,
} from '../jobs';
import { CircleCIEnvironment } from '../pipelines';
import { config } from '../config';

export class ReleaseWorkflow {
  private static workflowName = 'release';

  static create(dynamicConfig: Config, environment: CircleCIEnvironment) {
    const setupJob = SetupJob.create(dynamicConfig);
    dynamicConfig.addJob(setupJob);

    const slackAnnouncementJob = SlackAnnouncementJob.create(dynamicConfig);
    dynamicConfig.addJob(slackAnnouncementJob);

    const consoleWebuiBuildJob = ConsoleWebuiBuildJob.create(dynamicConfig, environment, true);
    dynamicConfig.addJob(consoleWebuiBuildJob);

    const portalWebuiBuildJob = PortalWebuiBuildJob.create(dynamicConfig, environment, true);
    dynamicConfig.addJob(portalWebuiBuildJob);

    const backendBuildAndPublishOnDownloadWebsiteJob = BackendBuildAndPublishOnDownloadWebsiteJob.create(dynamicConfig, environment, true);
    dynamicConfig.addJob(backendBuildAndPublishOnDownloadWebsiteJob);

    const releaseCommitAndPrepareNextVersionJob = ReleaseCommitAndPrepareNextVersionJob.create(dynamicConfig, environment);
    dynamicConfig.addJob(releaseCommitAndPrepareNextVersionJob);

    return new Workflow(ReleaseWorkflow.workflowName, [
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
        name: 'Build APIM Portal and publish on download website',
        requires: ['Setup'],
      }),

      // APIM Console
      new workflow.WorkflowJob(consoleWebuiBuildJob, {
        context: config.jobContext,
        name: 'Build APIM Console and publish on download website',
        requires: ['Setup'],
      }),

      // APIM Backend
      new workflow.WorkflowJob(backendBuildAndPublishOnDownloadWebsiteJob, {
        context: config.jobContext,
        name: 'Backend build and publish on download website',
        requires: ['Setup'],
      }),

      // Commit and set next version
      new workflow.WorkflowJob(releaseCommitAndPrepareNextVersionJob, {
        context: config.jobContext,
        name: 'Commit and prepare next version',
        requires: [
          'Backend build and publish on download website',
          'Build APIM Console and publish on download website',
          'Build APIM Portal and publish on download website',
        ],
      }),
    ]);
  }
}
