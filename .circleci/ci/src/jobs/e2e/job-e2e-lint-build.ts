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
import { InstallYarnCommand, NotifyOnFailureCommand, WebuiInstallCommand } from '../../commands';
import { Command } from '@circleci/circleci-config-sdk/dist/src/lib/Components/Commands/exports/Command';
import { NodeLtsExecutor } from '../../executors';
import { CircleCIEnvironment } from '../../pipelines';

export class E2ELintBuildJob {
  private static jobName = 'job-e2e-lint-build';
  public static create(dynamicConfig: Config, environment: CircleCIEnvironment): Job {
    const webuiInstallCmd = WebuiInstallCommand.get();
    const installYarnCmd = InstallYarnCommand.get();
    const notifyOnFailureCmd = NotifyOnFailureCommand.get(dynamicConfig, environment);
    dynamicConfig.addReusableCommand(installYarnCmd);
    dynamicConfig.addReusableCommand(webuiInstallCmd);
    dynamicConfig.addReusableCommand(notifyOnFailureCmd);

    const steps: Command[] = [
      new commands.Checkout(),
      new reusable.ReusedCommand(installYarnCmd),
      new reusable.ReusedCommand(webuiInstallCmd, { 'apim-ui-project': 'gravitee-apim-e2e' }),
      new commands.workspace.Attach({ at: '.' }),
      new commands.Run({
        name: 'Check License',
        command: `yarn lint:license`,
        working_directory: 'gravitee-apim-e2e',
      }),
      new commands.Run({
        name: 'Run Prettier and ESLint',
        command: `yarn lint`,
        working_directory: 'gravitee-apim-e2e',
      }),
      new commands.Run({
        name: 'Build',
        command: `yarn build`,
        working_directory: 'gravitee-apim-e2e',
      }),
      new reusable.ReusedCommand(notifyOnFailureCmd),
      new commands.workspace.Persist({
        root: '.',
        paths: ['gravitee-apim-e2e/dist', 'gravitee-apim-e2e/node_modules'],
      }),
    ];
    return new Job(E2ELintBuildJob.jobName, NodeLtsExecutor.create('small'), steps);
  }
}
