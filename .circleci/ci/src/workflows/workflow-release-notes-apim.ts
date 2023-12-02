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
import { CircleCIEnvironment } from '../pipelines';
import { ReleaseNotesApimJob } from '../jobs';
import { config } from '../config';

export class ReleaseNotesApimWorkflow {
  static create(dynamicConfig: Config, environment: CircleCIEnvironment) {
    const releaseNotesApimJob = ReleaseNotesApimJob.create(dynamicConfig, environment);
    dynamicConfig.addJob(releaseNotesApimJob);

    return new Workflow('release-notes-apim', [
      new workflow.WorkflowJob(releaseNotesApimJob, {
        context: config.jobContext,
        name: 'Generate release note and create PR',
      }),
    ]);
  }
}
