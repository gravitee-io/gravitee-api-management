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
import { OpenJdkExecutor } from '../executors';
import { Command } from '@circleci/circleci-config-sdk/dist/src/lib/Components/Commands/exports/Command';
import { computeImagesTag } from '../utils';
import { CircleCIEnvironment } from '../pipelines';
import { CreateDockerContextCommand, DockerAzureLoginCommand, DockerAzureLogoutCommand } from '../commands';
import { config } from '../config';

export class BuildBackendImagesJob {
  public static create(dynamicConfig: Config, environment: CircleCIEnvironment): Job {
    const tag = computeImagesTag(environment.branch);

    const createDockerContextCmd = CreateDockerContextCommand.get();
    dynamicConfig.addReusableCommand(createDockerContextCmd);

    const dockerAzureLoginCmd = DockerAzureLoginCommand.get(dynamicConfig);
    dynamicConfig.addReusableCommand(dockerAzureLoginCmd);

    const dockerAzureLogoutCmd = DockerAzureLogoutCommand.get();
    dynamicConfig.addReusableCommand(dockerAzureLogoutCmd);

    const steps: Command[] = [
      new commands.Checkout(),
      new commands.workspace.Attach({ at: '.' }),
      new commands.SetupRemoteDocker({ version: config.docker.version }),
      new reusable.ReusedCommand(createDockerContextCmd),
      new reusable.ReusedCommand(dockerAzureLoginCmd),
      new commands.Run({
        name: 'Build rest api and gateway docker images',
        command: `docker buildx build --push --platform=linux/arm64,linux/amd64 -f gravitee-apim-rest-api/docker/Dockerfile \\
-t graviteeio.azurecr.io/apim-management-api:${tag} \\
rest-api-docker-context

docker buildx build --push --platform=linux/arm64,linux/amd64 -f gravitee-apim-gateway/docker/Dockerfile \\
-t graviteeio.azurecr.io/apim-gateway:${tag} \\
gateway-docker-context`,
      }),
    ];

    steps.push(new reusable.ReusedCommand(DockerAzureLogoutCommand.get()));

    return new Job('job-build-images', OpenJdkExecutor.create(), steps); // TODO check if we can use cimg/base:stable executor
  }
}
