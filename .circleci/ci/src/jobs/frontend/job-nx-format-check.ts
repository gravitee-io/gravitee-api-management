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
import { NodeLtsExecutor } from '../../executors';
import { InstallYarnCommand, NotifyOnFailureCommand, WorkspaceInstallCommand } from '../../commands';
import { CircleCIEnvironment } from '../../pipelines';

export class NxFormatCheckJob {
  private static jobName = 'job-nx-format-check';

  public static create(dynamicConfig: Config, environment: CircleCIEnvironment): Job {
    const installYarnCmd = InstallYarnCommand.get();
    dynamicConfig.addReusableCommand(installYarnCmd);

    const workspaceInstallCommand = WorkspaceInstallCommand.get();
    dynamicConfig.addReusableCommand(workspaceInstallCommand);

    const notifyOnFailureCommand = NotifyOnFailureCommand.get(dynamicConfig, environment);
    dynamicConfig.addReusableCommand(notifyOnFailureCommand);

    const steps: Command[] = [
      new commands.Checkout(),
      new reusable.ReusedCommand(installYarnCmd),
      new reusable.ReusedCommand(workspaceInstallCommand),
      new commands.workspace.Attach({ at: '.' }),
      new commands.Run({
        name: 'Check prettier formatting for nx projects',
        command: 'yarn prettier',
      }),
      new reusable.ReusedCommand(notifyOnFailureCommand),
    ];

    return new Job(NxFormatCheckJob.jobName, NodeLtsExecutor.create('medium'), steps);
  }
}
