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
import { commands, Config, reusable } from '@circleci/circleci-config-sdk';
import { ReusableCommand } from '@circleci/circleci-config-sdk/dist/src/lib/Components/Commands/exports/Reusable';
import { orbs } from '../orbs';
import { config } from '../config';
import { CircleCIEnvironment } from '../pipelines';
import { Command } from '@circleci/circleci-config-sdk/dist/src/lib/Components/Commands/exports/Command';

export class DockerLoginCommand {
  private static commandName = 'cmd-docker-login';

  public static get(dynamicConfig: Config, environment: CircleCIEnvironment, isProd: boolean): ReusableCommand {
    dynamicConfig.importOrb(orbs.keeper);

    const dockerRegistryUsernameSecretUrl = isProd ? config.secrets.dockerhubBotUserName : config.secrets.azureRegistryUsername;
    const dockerRegistryPasswordSecretUrl = isProd ? config.secrets.dockerhubBotUserToken : config.secrets.azureRegistryPassword;
    const dockerRegistryName = isProd ? 'Docker Hub' : 'Azure Container Registry';
    const dockerRegistry = isProd ? '' : 'graviteeio.azurecr.io';

    const steps: Command[] = [
      new reusable.ReusedCommand(orbs.keeper.commands['env-export'], {
        'secret-url': dockerRegistryUsernameSecretUrl,
        'var-name': 'DOCKER_REGISTRY_USERNAME',
      }),
      new reusable.ReusedCommand(orbs.keeper.commands['env-export'], {
        'secret-url': dockerRegistryPasswordSecretUrl,
        'var-name': 'DOCKER_REGISTRY_PASSWORD',
      }),
    ];
    if (isProd && environment.isDryRun) {
      steps.push(
        new commands.Run({
          name: `No login to ${dockerRegistryName} - Dry-Run`,
          command: `echo "DRY RUN Mode. Build only"`,
        }),
      );
    } else {
      steps.push(
        new commands.Run({
          name: `Login to ${dockerRegistryName}`,
          command: `echo $DOCKER_REGISTRY_PASSWORD | docker login --username $DOCKER_REGISTRY_USERNAME --password-stdin ${dockerRegistry}`,
        }),
      );
    }
    return new reusable.ReusableCommand(DockerLoginCommand.commandName, steps, undefined, `Login to ${dockerRegistryName}`);
  }
}
