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

export class DockerAzureLoginCommand {
  private static commandName = 'cmd-docker-azure-login';

  public static get(dynamicConfig: Config): ReusableCommand {
    dynamicConfig.importOrb(orbs.keeper);

    return new reusable.ReusableCommand(
      DockerAzureLoginCommand.commandName,
      [
        new reusable.ReusedCommand(orbs.keeper.commands['env-export'], {
          'secret-url': config.secrets.azureRegistryUsername,
          'var-name': 'AZURE_DOCKER_REGISTRY_USERNAME',
        }),
        new reusable.ReusedCommand(orbs.keeper.commands['env-export'], {
          'secret-url': config.secrets.azureRegistryPassword,
          'var-name': 'AZURE_DOCKER_REGISTRY_PASSWORD',
        }),
        new commands.Run({
          name: 'Login to Azure Container Registry',
          command:
            'echo $AZURE_DOCKER_REGISTRY_PASSWORD | docker login --username $AZURE_DOCKER_REGISTRY_USERNAME --password-stdin graviteeio.azurecr.io',
        }),
      ],
      undefined,
      'Login to Azure Container Registry',
    );
  }
}
