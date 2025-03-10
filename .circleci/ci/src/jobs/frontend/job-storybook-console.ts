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
import { commands, Config, Job, parameters, reusable } from '@circleci/circleci-config-sdk';
import { Command } from '@circleci/circleci-config-sdk/dist/src/lib/Components/Commands/exports/Command';
import { NodeLtsExecutor } from '../../executors';
import { NotifyOnFailureCommand, WebuiInstallCommand } from '../../commands';
import { config } from '../../config';
import { CommandParameterLiteral } from '@circleci/circleci-config-sdk/dist/src/lib/Components/Parameters/types/CustomParameterLiterals.types';
import { CircleCIEnvironment } from '../../pipelines';

export class StorybookConsoleJob {
  private static jobName = 'job-console-webui-build-storybook';

  private static customParametersList = new parameters.CustomParametersList<CommandParameterLiteral>([
    new parameters.CustomParameter('node_version', 'string', config.executor.node.console.version, 'Node version to use for executor'),
  ]);

  public static create(dynamicConfig: Config, environment: CircleCIEnvironment): Job {
    const webUiInstallCommand = WebuiInstallCommand.get();
    dynamicConfig.addReusableCommand(webUiInstallCommand);

    const notifyOnFailureCommand = NotifyOnFailureCommand.get(dynamicConfig, environment);
    dynamicConfig.addReusableCommand(notifyOnFailureCommand);

    const steps: Command[] = [
      new commands.Checkout(),
      new reusable.ReusedCommand(webUiInstallCommand, { 'apim-ui-project': config.dockerImages.console.project }),
      new commands.Run({
        name: 'Build',
        command: 'npm run build-storybook',
        working_directory: config.dockerImages.console.project,
        environment: {
          NODE_OPTIONS: '--max_old_space_size=8192',
        },
      }),
      new reusable.ReusedCommand(notifyOnFailureCommand),
      new commands.workspace.Persist({
        root: '.',
        paths: ['gravitee-apim-console-webui/storybook-static'],
      }),
    ];

    return new reusable.ParameterizedJob(
      StorybookConsoleJob.jobName,
      NodeLtsExecutor.create('large', '<< parameters.node_version >>'),
      StorybookConsoleJob.customParametersList,
      steps,
    );
  }
}
