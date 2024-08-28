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
import { CommandParameterLiteral } from '@circleci/circleci-config-sdk/dist/src/lib/Components/Parameters/types/CustomParameterLiterals.types';
import { NodeLtsExecutor } from '../../executors';
import { BuildUiImageCommand, NotifyOnFailureCommand, WebuiInstallCommand } from '../../commands';
import { CircleCIEnvironment } from '../../pipelines';
import { computeApimVersion } from '../../utils';
import { config } from '../../config';

export class WebuiBuildJob {
  private static jobName = 'job-webui-build';

  private static customParametersList = new parameters.CustomParametersList<CommandParameterLiteral>([
    new parameters.CustomParameter('apim-ui-project', 'string', '', 'the name of the UI project to build'),
    new parameters.CustomParameter('docker-image-name', 'string', '', 'the name of the image'),
    new parameters.CustomParameter('node_version', 'string', config.executor.node.version, 'Node version to use for executor'),
  ]);

  public static create(dynamicConfig: Config, environment: CircleCIEnvironment): Job {
    const webUiInstallCommand = WebuiInstallCommand.get();
    dynamicConfig.addReusableCommand(webUiInstallCommand);

    const notifyOnFailureCommand = NotifyOnFailureCommand.get(dynamicConfig);
    dynamicConfig.addReusableCommand(notifyOnFailureCommand);

    const buildUiImageCommand = BuildUiImageCommand.get(dynamicConfig, environment);
    dynamicConfig.addReusableCommand(buildUiImageCommand);

    const apimVersion = computeApimVersion(environment);

    const steps: Command[] = [
      new commands.Checkout(),
      new commands.workspace.Attach({ at: '.' }),
      new commands.SetupRemoteDocker({ version: config.docker.version }),
      new reusable.ReusedCommand(webUiInstallCommand, { 'apim-ui-project': '<< parameters.apim-ui-project >>' }),
      new commands.Run({
        name: 'Update Build version',
        command: `sed -i 's/"version": ".*"/"version": "${apimVersion}"/' << parameters.apim-ui-project >>/build.json`,
      }),
      new commands.Run({
        name: 'Build',
        command: 'npm run build:prod',
        environment: {
          NODE_OPTIONS: '--max_old_space_size=4086',
        },
        working_directory: '<< parameters.apim-ui-project >>',
      }),
      new reusable.ReusedCommand(buildUiImageCommand, {
        'docker-image-name': '<< parameters.docker-image-name >>',
        'apim-ui-project': '<< parameters.apim-ui-project >>',
      }),
      new reusable.ReusedCommand(notifyOnFailureCommand),
      new commands.workspace.Persist({
        root: '.',
        paths: ['<< parameters.apim-ui-project >>/dist'],
      }),
    ];

    return new reusable.ParameterizedJob(
      WebuiBuildJob.jobName,
      NodeLtsExecutor.create('large', '<< parameters.node_version >>'),
      WebuiBuildJob.customParametersList,
      steps,
    );
  }
}
