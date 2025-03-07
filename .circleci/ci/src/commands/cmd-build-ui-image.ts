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
import { commands, Config, parameters, reusable } from '@circleci/circleci-config-sdk';
import { computeImagesTag, isSupportBranchOrMaster } from '../utils';
import { CircleCIEnvironment } from '../pipelines';
import { CreateDockerContextCommand } from './cmd-create-docker-context';
import { DockerAzureLogoutCommand } from './cmd-docker-azure-logout';
import { DockerLoginCommand } from './index';
import { Command } from '@circleci/circleci-config-sdk/dist/src/lib/Components/Commands/exports/Command';
import { orbs } from '../orbs';
import { config } from '../config';

export class BuildUiImageCommand {
  private static commandName = 'cmd-build-ui-image';

  private static customParametersList = new parameters.CustomParametersList([
    new parameters.CustomParameter('docker-image-name', 'string', '', 'the name of the image'),
    new parameters.CustomParameter('apim-ui-project', 'string', '', 'the name of the UI project to build'),
  ]);

  public static get(dynamicConfig: Config, environment: CircleCIEnvironment): reusable.ReusableCommand {
    const tag = computeImagesTag(environment.branch);

    const createDockerContextCommand = CreateDockerContextCommand.get();
    dynamicConfig.addReusableCommand(createDockerContextCommand);

    const dockerLoginCommand = DockerLoginCommand.get(dynamicConfig, environment, false);
    dynamicConfig.addReusableCommand(dockerLoginCommand);

    const dockerAzureLogoutCommand = DockerAzureLogoutCommand.get();
    dynamicConfig.addReusableCommand(dockerAzureLogoutCommand);

    const steps: Command[] = [
      new reusable.ReusedCommand(createDockerContextCommand),
      new reusable.ReusedCommand(dockerLoginCommand),
      new commands.Run({
        name: 'Build UI docker image',
        command: `docker buildx build --push --platform=linux/arm64,linux/amd64 -f docker/Dockerfile \\
-t graviteeio.azurecr.io/<< parameters.docker-image-name >>:${tag} \\
.`,
        working_directory: '<< parameters.apim-ui-project >>',
      }),
    ];

    if (isSupportBranchOrMaster(environment.branch)) {
      dynamicConfig.importOrb(orbs.keeper).importOrb(orbs.aquasec);
      steps.push(
        new reusable.ReusedCommand(orbs.keeper.commands['env-export'], {
          'secret-url': config.secrets.aquaKey,
          'var-name': 'AQUA_KEY',
        }),
        new reusable.ReusedCommand(orbs.keeper.commands['env-export'], {
          'secret-url': config.secrets.aquaSecret,
          'var-name': 'AQUA_SECRET',
        }),
        new reusable.ReusedCommand(orbs.keeper.commands['env-export'], {
          'secret-url': config.secrets.aquaRegistryUsername,
          'var-name': 'AQUA_USERNAME',
        }),
        new reusable.ReusedCommand(orbs.keeper.commands['env-export'], {
          'secret-url': config.secrets.aquaRegistryPassword,
          'var-name': 'AQUA_PASSWORD',
        }),
        new reusable.ReusedCommand(orbs.keeper.commands['env-export'], {
          'secret-url': config.secrets.aquaScannerKey,
          'var-name': 'SCANNER_TOKEN',
        }),
        new reusable.ReusedCommand(orbs.keeper.commands['env-export'], {
          'secret-url': config.secrets.githubApiToken,
          'var-name': 'GITHUB_TOKEN',
        }),
        new reusable.ReusedCommand(orbs.aquasec.commands['install_billy']),
        new reusable.ReusedCommand(orbs.aquasec.commands['pull_aqua_scanner_image']),
        new reusable.ReusedCommand(orbs.aquasec.commands['register_artifact'], {
          artifact_to_register: `graviteeio.azurecr.io/<< parameters.docker-image-name >>:${tag}`,
          debug: true,
        }),
        new reusable.ReusedCommand(orbs.aquasec.commands['scan_docker_image'], {
          docker_image_to_scan: `graviteeio.azurecr.io/<< parameters.docker-image-name >>:${tag}`,
          scanner_url: config.aqua.scannerUrl,
        }),
      );
    }

    steps.push(new reusable.ReusedCommand(dockerAzureLogoutCommand));

    return new reusable.ReusableCommand(BuildUiImageCommand.commandName, steps, BuildUiImageCommand.customParametersList);
  }
}
