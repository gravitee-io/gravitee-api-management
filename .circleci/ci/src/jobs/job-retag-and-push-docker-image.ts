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
import { computeImagesTag } from '../utils';
import { CircleCIEnvironment } from '../pipelines';
import { DockerLoginCommand, DockerLogoutCommand } from '../commands';
import { Command } from '@circleci/circleci-config-sdk/dist/src/lib/Components/Commands/exports/Command';
import { config } from '../config';
import { BaseExecutor } from '../executors';
import { orbs } from '../orbs';
import { aquaSetupCommands } from './job-build-docker-image';

export class ReTagAndPushDockerImageJob {
  private static jobName = 'job-retag-and-push-docker-image';

  private static customParametersList = new parameters.CustomParametersList([
    new parameters.CustomParameter('docker-image-name', 'string', '', 'the name of the image'),
  ]);

  public static create(dynamicConfig: Config, environment: CircleCIEnvironment, isProd: boolean): reusable.ParameterizedJob {
    dynamicConfig.importOrb(orbs.keeper).importOrb(orbs.aquasec);

    const dockerLoginCommand = DockerLoginCommand.get(dynamicConfig, environment, isProd);
    dynamicConfig.addReusableCommand(dockerLoginCommand);

    const dockerLogoutCommand = DockerLogoutCommand.get(environment, isProd);
    dynamicConfig.addReusableCommand(dockerLogoutCommand);

    const sourceTag = computeImagesTag(environment.baseBranch);
    const destinationTag = computeImagesTag(environment.branch);

    const steps: Command[] = [
      new commands.Checkout(),
      new commands.SetupRemoteDocker({ version: config.docker.version }),
      new reusable.ReusedCommand(dockerLoginCommand),
      ...aquaSetupCommands(),
      new commands.Run({
        name: 'Pull existing image',
        command: `docker pull graviteeio.azurecr.io/<< parameters.docker-image-name >>:${sourceTag}`,
      }),
      new commands.Run({
        name: 'Retag image',
        command: `
          docker tag \\
            graviteeio.azurecr.io/<< parameters.docker-image-name >>:${sourceTag} \\
            graviteeio.azurecr.io/<< parameters.docker-image-name >>:${destinationTag}
        `,
      }),
      new commands.Run({
        name: 'Push with new tag',
        command: `docker push graviteeio.azurecr.io/<< parameters.docker-image-name >>:${destinationTag}`,
      }),
    ];

    steps.push(new reusable.ReusedCommand(dockerLogoutCommand));

    return new reusable.ParameterizedJob(
      ReTagAndPushDockerImageJob.jobName,
      BaseExecutor.create(),
      ReTagAndPushDockerImageJob.customParametersList,
      steps,
    );
  }
}
