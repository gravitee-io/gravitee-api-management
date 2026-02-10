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
import { InstallYarnCommand, NotifyOnFailureCommand, WebuiInstallCommand, WorkspaceInstallCommand } from '../../commands';
import { CircleCIEnvironment } from '../../pipelines';
import { computeApimVersion } from '../../utils';
import { config } from '../../config';

export class PortalWebuiBuildJob {
  private static jobName = 'job-portal-webui-build';

  public static create(dynamicConfig: Config, environment: CircleCIEnvironment): Job {
    const installYarnCmd = InstallYarnCommand.get();
    dynamicConfig.addReusableCommand(installYarnCmd);

    const webUiInstallCommand = WebuiInstallCommand.get();
    dynamicConfig.addReusableCommand(webUiInstallCommand);

    const workspaceInstallCommand = WorkspaceInstallCommand.get();
    dynamicConfig.addReusableCommand(workspaceInstallCommand);

    const notifyOnFailureCommand = NotifyOnFailureCommand.get(dynamicConfig, environment);
    dynamicConfig.addReusableCommand(notifyOnFailureCommand);

    const apimVersion = ['build_rpm', 'build_docker_images', 'release', 'full_release'].includes(environment.action)
      ? environment.graviteeioVersion
      : computeApimVersion(environment);

    const steps: Command[] = [
      new commands.Checkout(),
      new commands.workspace.Attach({ at: '.' }),
      new commands.SetupRemoteDocker({ version: config.docker.version }),
      new reusable.ReusedCommand(installYarnCmd),
      // Build old portal (still uses own package.json)
      new reusable.ReusedCommand(webUiInstallCommand, { 'apim-ui-project': `${config.components.portal.project}` }),
      new commands.Run({
        name: 'Update Build version',
        command: `sed -i 's/"version": ".*"/"version": "${apimVersion}"/' ${config.components.portal.project}/build.json`,
      }),
      new commands.Run({
        name: 'Build',
        command: 'yarn build:prod',
        environment: {
          NODE_OPTIONS: '--max_old_space_size=4086',
        },
        working_directory: `${config.components.portal.project}`,
      }),
      new reusable.ReusedCommand(workspaceInstallCommand),
      new commands.Run({
        name: 'Update Build version',
        command: `sed -i 's/"version": ".*"/"version": "${apimVersion}"/' ${config.components.portal.next.project}/build.json`,
      }),
      new commands.Run({
        name: 'Build Portal Next',
        command: `NODE_OPTIONS=--max_old_space_size=4086 yarn portal-next:build:prod`,
      }),
    ];

    steps.push(
      new reusable.ReusedCommand(notifyOnFailureCommand),
      new commands.workspace.Persist({
        root: '.',
        paths: [`${config.components.portal.project}/dist`],
      }),
    );

    return new Job(PortalWebuiBuildJob.jobName, NodeLtsExecutor.create('large'), steps);
  }
}
