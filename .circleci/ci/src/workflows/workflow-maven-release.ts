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
import { NexusStagingJob, ReleaseCommitAndPrepareNextVersionJob, SetupJob } from '../jobs';
import { CircleCIEnvironment } from '../pipelines';
import { config } from '../config';

export class WorkflowMavenRelease {
  private static workflowName = 'maven_release';

  static create(dynamicConfig: Config, environment: CircleCIEnvironment) {
    const setupJob = SetupJob.create(dynamicConfig);
    dynamicConfig.addJob(setupJob);

    const releaseCommitAndPrepareNextVersionJob = ReleaseCommitAndPrepareNextVersionJob.create(dynamicConfig, environment);
    dynamicConfig.addJob(releaseCommitAndPrepareNextVersionJob);

    const nexusStagingJob = NexusStagingJob.create(dynamicConfig, environment);
    dynamicConfig.addJob(nexusStagingJob);

    return new Workflow(WorkflowMavenRelease.workflowName, [
      // Setup
      new workflow.WorkflowJob(setupJob, { context: config.jobContext, name: 'Setup' }),

      // Tag release and prepare next version
      new workflow.WorkflowJob(releaseCommitAndPrepareNextVersionJob, {
        context: config.jobContext,
        name: 'Commit and prepare next version',
        requires: ['Setup'],
      }),

      // Build and deploy JARs to Maven Central
      new workflow.WorkflowJob(nexusStagingJob, {
        context: config.jobContext,
        name: 'Nexus staging',
        requires: ['Commit and prepare next version'],
      }),
    ]);
  }
}
