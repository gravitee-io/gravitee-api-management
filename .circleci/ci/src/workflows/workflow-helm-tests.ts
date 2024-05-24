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
import { TestApimChartsJob } from '../jobs';

export class HelmTestsWorkflow {
  static create(dynamicConfig: Config) {
    const apimChartsTestJob = TestApimChartsJob.create(dynamicConfig);
    dynamicConfig.addJob(apimChartsTestJob);

    const jobs = [
      new workflow.WorkflowJob(apimChartsTestJob, {
        name: 'Helm Chart - Lint & Test - << matrix.helmClientVersion >>',
        matrix: {
          helmClientVersion: ['v3.9.4', 'v3.10.3', 'v3.11.3', 'v3.12.3', 'v3.13.3', 'v3.14.4', 'v3.15.1'],
        },
      }),
    ];

    return new Workflow('helm-tests', jobs);
  }
}
