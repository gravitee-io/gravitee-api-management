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
import { NodeLtsExecutor } from '../executors';
import { InstallYarnCommand, NotifyOnFailureCommand, WebuiInstallCommand } from '../commands';

export class PerfLintBuildJob {
  private static jobName = 'job-perf-lint-build';

  public static create(dynamicConfig: Config): Job {
    const installYarnCmd = InstallYarnCommand.get();
    dynamicConfig.addReusableCommand(installYarnCmd);

    const webUiInstallCommand = WebuiInstallCommand.get();
    dynamicConfig.addReusableCommand(webUiInstallCommand);

    const notifyOnFailureCommand = NotifyOnFailureCommand.get(dynamicConfig);
    dynamicConfig.addReusableCommand(notifyOnFailureCommand);

    const steps: Command[] = [
      new commands.Checkout(),
      new reusable.ReusedCommand(installYarnCmd),
      new reusable.ReusedCommand(webUiInstallCommand, { 'apim-ui-project': 'gravitee-apim-perf' }),
      new commands.workspace.Attach({ at: '.' }),
      new commands.Run({
        name: 'Check License',
        command: 'yarn lint:license',
        working_directory: 'gravitee-apim-perf',
      }),
      new commands.Run({
        name: 'Run Prettier and ESLint',
        command: 'yarn lint',
        working_directory: 'gravitee-apim-perf',
      }),
      new commands.Run({
        name: 'Build',
        command: 'yarn build',
        working_directory: 'gravitee-apim-perf',
      }),
      new reusable.ReusedCommand(notifyOnFailureCommand),
    ];

    return new Job(PerfLintBuildJob.jobName, NodeLtsExecutor.create('small'), steps);
  }
}
