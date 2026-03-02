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

export class StorybookAllJob {
  private static jobName = 'job-build-all-storybooks';

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
      new commands.Run({
        name: 'Build all Storybooks',
        command: 'NODE_OPTIONS=--max_old_space_size=8192 yarn nx run storybook-all:build-storybook --parallel=2',
      }),
      new reusable.ReusedCommand(notifyOnFailureCommand),
      new commands.workspace.Persist({
        root: '.',
        paths: ['storybook-all/storybook-static', 'gravitee-apim-console-webui/dist-lib'],
      }),
    ];

    return new Job(StorybookAllJob.jobName, NodeLtsExecutor.create('large'), steps);
  }
}
