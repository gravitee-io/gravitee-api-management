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
import { commands, Config, Job } from '@circleci/circleci-config-sdk';
import { CircleCIEnvironment } from '../pipelines';
import { Command } from '@circleci/circleci-config-sdk/dist/src/lib/Components/Commands/exports/Command';
import { BaseExecutor } from '../executors';
import { InstallYarnCommand } from '../commands';

export class TriggerSaasDockerImagesJob {
  private static jobName: string = 'job-trigger-saas-docker-images';
  public static create(dynamicConfig: Config, environment: CircleCIEnvironment): Job {
    const installYarnCmd = InstallYarnCommand.get();
    dynamicConfig.addReusableCommand(installYarnCmd);

    const steps: Command[] = [
      new commands.Run({
        name: 'Trigger SaaS Docker images pipeline',
        command: `curl --request POST \
  --url https://circleci.com/api/v2/project/github/gravitee-io/cloud-distributions/pipeline \
  --header "Circle-Token: \${CIRCLE_TOKEN}" \
  --header 'content-type: application/json' \
  --data '{"parameters":{"project":"apim", "branch-version":"${environment.branch}", "release-version":"${environment.branch}", "dry-run":${environment.isDryRun}}}'`,
      }),
    ];

    return new Job(TriggerSaasDockerImagesJob.jobName, BaseExecutor.create(), steps);
  }
}
