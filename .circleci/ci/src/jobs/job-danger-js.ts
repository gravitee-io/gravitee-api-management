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
import { orbs } from '../orbs';
import { config } from '../config';
import { NodeLtsExecutor } from '../executors';
import { InstallYarnCommand } from '../commands';

export class DangerJsJob {
  private static jobName = 'job-danger-js';
  public static create(dynamicConfig: Config): Job {
    dynamicConfig.importOrb(orbs.keeper);

    const installYarnCmd = InstallYarnCommand.get();
    dynamicConfig.addReusableCommand(installYarnCmd);

    const steps: Command[] = [
      new commands.Checkout(),
      new reusable.ReusedCommand(installYarnCmd),
      new reusable.ReusedCommand(orbs.keeper.commands['env-export'], {
        'secret-url': config.secrets.githubApiToken,
        'var-name': 'DANGER_GITHUB_API_TOKEN',
      }),
      new commands.Run({
        name: 'Run Danger JS',
        command: 'cd .circleci/danger && yarn install && yarn run danger',
      }),
    ];
    return new Job(DangerJsJob.jobName, NodeLtsExecutor.create('small'), steps);
  }
}
