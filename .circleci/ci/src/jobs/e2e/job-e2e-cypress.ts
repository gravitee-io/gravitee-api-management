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
import { DockerLoginCommand, DockerLogoutCommand, InstallYarnCommand, NotifyOnFailureCommand } from '../../commands';
import { Command } from '@circleci/circleci-config-sdk/dist/src/lib/Components/Commands/exports/Command';
import { computeImagesTag } from '../../utils';
import { CircleCIEnvironment } from '../../pipelines';
import { UbuntuExecutor } from '../../executors';
import { keeper } from '../../orbs/keeper';
import { config } from '../../config';

export class E2ECypressJob {
  private static jobName = `job-e2e-cypress`;
  public static create(dynamicConfig: Config, environment: CircleCIEnvironment): Job {
    const installYarnCmd = InstallYarnCommand.get();
    const dockerLoginCmd = DockerLoginCommand.get(dynamicConfig, environment, false);
    const dockerLogoutCmd = DockerLogoutCommand.get(environment, false);
    const notifyOnFailureCmd = NotifyOnFailureCommand.get(dynamicConfig, environment);
    dynamicConfig.addReusableCommand(installYarnCmd);
    dynamicConfig.addReusableCommand(dockerLoginCmd);
    dynamicConfig.addReusableCommand(dockerLogoutCmd);
    dynamicConfig.addReusableCommand(notifyOnFailureCmd);

    const dockerImageTag = computeImagesTag(environment.branch);

    const steps: Command[] = [
      new commands.Checkout(),
      new commands.workspace.Attach({ at: '.' }),
      new reusable.ReusedCommand(dockerLoginCmd),
      new reusable.ReusedCommand(keeper.commands['env-export'], {
        'secret-url': config.secrets.cypressCloudKey,
        'var-name': 'CYPRESS_CLOUD_KEY',
      }),
      new reusable.ReusedCommand(installYarnCmd),
      new commands.Run({
        name: `Running UI tests`,
        command: `cd gravitee-apim-e2e
APIM_REGISTRY=graviteeio.azurecr.io APIM_TAG=${dockerImageTag} yarn test:ui`,
      }),
      new reusable.ReusedCommand(dockerLogoutCmd),
      new reusable.ReusedCommand(notifyOnFailureCmd),
      new commands.StoreArtifacts({
        path: './gravitee-apim-e2e/.tmp/screenshots',
      }),
      new commands.StoreArtifacts({
        path: './gravitee-apim-e2e/.tmp/videos',
      }),
      new commands.StoreArtifacts({
        path: './gravitee-apim-e2e/.logs',
      }),
    ];
    return new Job(E2ECypressJob.jobName, UbuntuExecutor.create('large'), steps);
  }
}
