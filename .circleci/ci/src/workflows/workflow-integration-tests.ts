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
import { Config, workflow, Workflow } from '../circleci-config';
import { BuildBackendJob, SetupJob, TestIntegrationJob } from '../jobs';
import { CircleCIEnvironment } from '../pipelines';
import { config } from '../config';

/**
 * Runs the real-plugin integration tests on demand, on whichever branch the pipeline is
 * triggered from.
 *
 * This is the counterpart to engine pull requests no longer triggering these tests: an
 * engineer who wants them on their branch asks for them here. It is deliberately the whole
 * chain rather than the tests alone — the suite runs against a freshly assembled
 * distribution, and `Build backend` produces both halves of it.
 */
export class IntegrationTestsWorkflow {
  static create(dynamicConfig: Config, environment: CircleCIEnvironment): Workflow {
    const setupJob = SetupJob.create(dynamicConfig);
    dynamicConfig.addJob(setupJob);

    const buildBackendJob = BuildBackendJob.create(dynamicConfig, environment);
    dynamicConfig.addJob(buildBackendJob);

    const testIntegrationJob = TestIntegrationJob.create(dynamicConfig, environment);
    dynamicConfig.addJob(testIntegrationJob);

    return new Workflow('integration_tests', [
      new workflow.WorkflowJob(setupJob, { name: 'Setup', context: config.jobContext }),
      new workflow.WorkflowJob(buildBackendJob, {
        name: 'Build backend',
        context: config.jobContext,
        requires: ['Setup'],
      }),
      new workflow.WorkflowJob(testIntegrationJob, {
        name: 'Integration tests',
        context: config.jobContext,
        requires: ['Build backend'],
      }),
    ]);
  }
}
