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
import { PackageBundleJob, SetupJob } from '../jobs';
import { config } from '../config';

export class PackageBundleWorkflow {
  static create(dynamicConfig: Config, graviteeioVersion: string, isDryRun: boolean) {
    const setupJob = SetupJob.create(dynamicConfig);
    dynamicConfig.addJob(setupJob);

    const bundleJob = PackageBundleJob.create(dynamicConfig, graviteeioVersion, isDryRun);
    dynamicConfig.addJob(bundleJob);
    return new Workflow('package_bundle', [
      new workflow.WorkflowJob(setupJob, { context: config.jobContext, name: 'Setup' }),
      new workflow.WorkflowJob(bundleJob, { context: config.jobContext, name: 'Package bundle', requires: ['Setup'] }),
    ]);
  }
}
