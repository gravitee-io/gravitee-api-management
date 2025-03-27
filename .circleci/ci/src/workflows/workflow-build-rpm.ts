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
import { CircleCIEnvironment } from '../pipelines';
import { PublishRpmPackagesJob } from '../jobs';
import { config } from '../config';

export class BuildRpmWorkflow {
  static create(dynamicConfig: Config, environment: CircleCIEnvironment) {
    const publishRpmPackagesJob = PublishRpmPackagesJob.create(dynamicConfig, environment);
    dynamicConfig.addJob(publishRpmPackagesJob);

    let publishRpmPackagesJobName = `Build and push RPM packages for APIM ${environment.graviteeioVersion}`;

    if (environment.isDryRun) {
      publishRpmPackagesJobName += ' - Dry Run';
    }

    const jobs = [
      new workflow.WorkflowJob(publishRpmPackagesJob, {
        context: config.jobContext,
        name: publishRpmPackagesJobName,
      }),
    ];

    return new Workflow('build-rpm', jobs);
  }
}
