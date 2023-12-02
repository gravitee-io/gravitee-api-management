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
import { commands, Config, Job, reusable } from '@circleci/circleci-config-sdk';
import { Command } from '@circleci/circleci-config-sdk/dist/src/lib/Components/Commands/exports/Command';
import { orbs } from '../../orbs';
import { config } from '../../config';
import { CircleCIEnvironment } from '../../pipelines';
import { BaseExecutor } from '../../executors';

export class SnykApimChartsJob {
  private static jobName = 'job-snyk-apim-charts';
  public static create(dynamicConfig: Config, environment: CircleCIEnvironment): Job {
    dynamicConfig.importOrb(orbs.helm);
    dynamicConfig.importOrb(orbs.keeper);
    dynamicConfig.importOrb(orbs.snyk);

    const steps: Command[] = [
      new commands.Checkout(),
      new reusable.ReusedCommand(orbs.keeper.commands['env-export'], {
        'secret-url': config.secrets.snykApiToken,
        'var-name': 'SNYK_TOKEN',
      }),
      new reusable.ReusedCommand(orbs.helm.commands['install-helm-client']),
      new reusable.ReusedCommand(orbs.snyk.commands['install']),
      new commands.Run({
        name: 'Build the Charts ouput and scan',
        command: `helm dependency update
helm template . --output-dir ./output
snyk iac test ./output --report --target-reference="${environment.branch}" --project-tags=version=${environment.branch} --severity-threshold=high`,
        working_directory: './helm',
      }),
    ];
    return new Job(SnykApimChartsJob.jobName, BaseExecutor.create('small'), steps);
  }
}
