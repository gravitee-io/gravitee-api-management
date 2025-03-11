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
import { config } from '../config';
import { ReleaseHelmJob, TestApimChartsJob } from '../jobs/helm';

export class ReleaseHelmWorkflow {
  private static workflowName = 'release_helm';

  static create(dynamicConfig: Config, environment: CircleCIEnvironment) {
    const testApimChartsJob = TestApimChartsJob.create(dynamicConfig);
    dynamicConfig.addJob(testApimChartsJob);

    const releaseHelmJob = ReleaseHelmJob.create(dynamicConfig, environment);
    dynamicConfig.addJob(releaseHelmJob);

    return new Workflow(ReleaseHelmWorkflow.workflowName, [
      new workflow.WorkflowJob(testApimChartsJob, { name: 'APIM - Lint & Test' }),
      new workflow.WorkflowJob(releaseHelmJob, { requires: ['APIM - Lint & Test'], context: config.jobContext }),
    ]);
  }
}
