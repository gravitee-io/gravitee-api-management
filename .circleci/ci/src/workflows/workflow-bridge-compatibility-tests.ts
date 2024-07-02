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
import { BuildBackendJob, SetupJob } from '../jobs';
import { E2EGenerateSDKJob, E2ELintBuildJob, E2ETestJob } from '../jobs/e2e';
import { config } from '../config';
import { CircleCIEnvironment } from '../pipelines';
import { ValidateJob } from '../jobs/backend';

export class BridgeCompatibilityTestsWorkflow {
  static create(dynamicConfig: Config, environment: CircleCIEnvironment) {
    const setupJob = SetupJob.create(dynamicConfig);
    dynamicConfig.addJob(setupJob);

    const validateJob = ValidateJob.create(dynamicConfig, environment);
    dynamicConfig.addJob(validateJob);

    const buildBackendJob = BuildBackendJob.create(dynamicConfig, environment);
    dynamicConfig.addJob(buildBackendJob);

    const e2eGenerateSdkJob = E2EGenerateSDKJob.create(dynamicConfig);
    dynamicConfig.addJob(e2eGenerateSdkJob);

    const e2eLintBuildJob = E2ELintBuildJob.create(dynamicConfig);
    dynamicConfig.addJob(e2eLintBuildJob);

    const e2eTestJob = E2ETestJob.create(dynamicConfig, environment);
    dynamicConfig.addJob(e2eTestJob);

    const jobs = [
      new workflow.WorkflowJob(setupJob, { context: config.jobContext, name: 'Setup' }),
      new workflow.WorkflowJob(validateJob, { context: config.jobContext, name: 'Validate', requires: ['Setup'] }),
      new workflow.WorkflowJob(buildBackendJob, { context: config.jobContext, name: 'Build backend', requires: ['Validate'] }),
      new workflow.WorkflowJob(e2eGenerateSdkJob, {
        context: config.jobContext,
        name: 'Generate e2e tests SDK',
        requires: ['Build backend'],
      }),
      new workflow.WorkflowJob(e2eLintBuildJob, {
        context: config.jobContext,
        name: 'Lint & Build APIM e2e',
        requires: ['Generate e2e tests SDK'],
      }),
      new workflow.WorkflowJob(e2eTestJob, {
        context: config.jobContext,
        name: 'E2E - << matrix.execution_mode >> - << matrix.apim_client_tag >>',
        requires: ['Lint & Build APIM e2e'],
        matrix: {
          execution_mode: ['v3', 'v4-emulation-engine'],
          database: ['bridge'],
          apim_client_tag: ['4.4.x-latest', '4.3.x-latest', '4.2.x-latest', '4.1.x-latest'],
        },
      }),
    ];

    return new Workflow('bridge_compatibility_tests', jobs);
  }
}
