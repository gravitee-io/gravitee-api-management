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
import { computeImagesTag, GraviteeioVersion, isBlank, isSupportBranchOrMaster, parse } from '../utils';
import { CircleCIEnvironment } from '../pipelines';
import { CreateDockerContextCommand } from './cmd-create-docker-context';
import { DockerLogoutCommand } from './cmd-docker-logout';
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

  public static get(dynamicConfig: Config, environment: CircleCIEnvironment, isProd: boolean): reusable.ReusableCommand {
    const createDockerContextCommand = CreateDockerContextCommand.get();
    dynamicConfig.addReusableCommand(createDockerContextCommand);

    const dockerLoginCommand = DockerLoginCommand.get(dynamicConfig, environment, isProd);
    dynamicConfig.addReusableCommand(dockerLoginCommand);

    const dockerLogoutCommand = DockerLogoutCommand.get(environment, isProd);
    dynamicConfig.addReusableCommand(dockerLogoutCommand);

    const parsedGraviteeioVersion = parse(environment.graviteeioVersion);

    const dockerTags: string[] = this.dockerTagsArgument(environment, parsedGraviteeioVersion, isProd);

    const steps: Command[] = [
      new reusable.ReusedCommand(createDockerContextCommand),
      new reusable.ReusedCommand(dockerLoginCommand),
      new commands.Run({
        name: 'Build UI docker image',
        command: `${this.dockerBuildCommand(environment, dockerTags, isProd)}`,
        working_directory: '<< parameters.apim-ui-project >>',
      }),
    ];

    if (isProd || isSupportBranchOrMaster(environment.branch)) {
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
          artifact_to_register: `${dockerTags[0]}`,
          debug: true,
        }),
        new reusable.ReusedCommand(orbs.aquasec.commands['scan_docker_image'], {
          docker_image_to_scan: `${dockerTags[0]}`,
          scanner_url: config.aqua.scannerUrl,
        }),
      );
    }

    steps.push(new reusable.ReusedCommand(dockerLogoutCommand));

    return new reusable.ReusableCommand(BuildUiImageCommand.commandName, steps, BuildUiImageCommand.customParametersList);
  }

  private static dockerBuildCommand(environment: CircleCIEnvironment, dockerTags: string[], isProd: boolean) {
    let command = 'docker buildx build';

    // Only publish if not dry run or not prod
    if (!isProd || !environment.isDryRun) {
      command += ' --push';
    }

    command += ` --platform=linux/arm64,linux/amd64 -f docker/Dockerfile \\\n`;

    if (isProd) {
      command += ` --quiet`;
    }

    command += `${dockerTags.map((t) => `-t ${t}`).join(' ')} \\\n`;
    command += `.`;

    return command;
  }

  private static dockerTagsArgument(environment: CircleCIEnvironment, graviteeioVersion: GraviteeioVersion, isProd: boolean): string[] {
    const tags: string[] = [];
    if (isProd) {
      const stub = `graviteeio/<< parameters.docker-image-name >>:`;

      // Default tag
      tags.push(stub + graviteeioVersion.full);

      if (isBlank(graviteeioVersion.qualifier.full)) {
        // Only major and minor for one tag if no qualifier
        tags.push(stub + graviteeioVersion.version.major + '.' + graviteeioVersion.version.minor);

        if (environment.dockerTagAsLatest) {
          // Add two tags: major and 'latest'
          tags.push(stub + graviteeioVersion.version.major);
          tags.push(stub + 'latest');
        }
      } else {
        // Include qualifier name after full version
        tags.push(stub + graviteeioVersion.version.full + '-' + graviteeioVersion.qualifier.name);
      }
    } else {
      const tag = computeImagesTag(environment.branch);
      tags.push(`graviteeio.azurecr.io/<< parameters.docker-image-name >>:${tag}`);
    }
    return tags;
  }
}
