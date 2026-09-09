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
import { Config, Workflow, workflow } from '../circleci-config';
import { NexusStagingJob, PinCoreJob, SetupJob } from '../jobs';
import { CircleCIEnvironment } from '../pipelines';
import { config } from '../config';
import { CORE_TAG_FILTER } from '../utils';

/**
 * Publishes the core reactor from the tag that started the pipeline.
 *
 * There is no commit, tag or version bump here: `yarn prepare-core-release` has already done all
 * three, and pushing the tag is what started this. The lane only publishes, which is what makes it
 * safe to replay — running it again on the same tag produces the same artefacts.
 */
export class WorkflowCoreRelease {
  private static workflowName = 'core_release';

  /**
   * Every job repeats the tag filter. The continued configuration is a pipeline of its own, so it
   * inherits nothing from the filter that let the setup job run: without this, a pushed tag starts
   * the pipeline and then runs no job at all.
   */
  private static readonly tagOnly = {
    branches: { ignore: ['/.*/'] },
    tags: { only: [CORE_TAG_FILTER] },
  };

  static create(dynamicConfig: Config, environment: CircleCIEnvironment) {
    const setupJob = SetupJob.create(dynamicConfig);
    dynamicConfig.addJob(setupJob);

    const nexusStagingJob = NexusStagingJob.create(dynamicConfig, environment, environment.tag);
    dynamicConfig.addJob(nexusStagingJob);

    const pinCoreJob = PinCoreJob.create(dynamicConfig, environment);
    dynamicConfig.addJob(pinCoreJob);

    return new Workflow(WorkflowCoreRelease.workflowName, [
      new workflow.WorkflowJob(setupJob, { context: config.jobContext, name: 'Setup', filters: WorkflowCoreRelease.tagOnly }),

      new workflow.WorkflowJob(nexusStagingJob, {
        context: config.jobContext,
        name: 'Nexus staging',
        requires: ['Setup'],
        filters: WorkflowCoreRelease.tagOnly,
      }),

      // After publication, never before: the pull request's integration tests resolve the core it
      // pins, and that core has to exist by then.
      new workflow.WorkflowJob(pinCoreJob, {
        context: config.jobContext,
        name: 'Open the pinning pull request',
        requires: ['Nexus staging'],
        filters: WorkflowCoreRelease.tagOnly,
      }),
    ]);
  }
}
