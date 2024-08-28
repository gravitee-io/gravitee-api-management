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
import { CircleCIEnvironment } from '../pipelines';
import { keeper } from '../orbs/keeper';
import { Command } from '@circleci/circleci-config-sdk/dist/src/lib/Components/Commands/exports/Command';
import { config } from '../config';
import { GraviteeioVersion, isBlank, parse } from '../utils';
import { CreateDockerContextCommand } from '../commands';
import { BaseExecutor } from '../executors';

export class PublishProdDockerImagesJob {
  private static jobName = 'job-publish-prod-docker-images';
  public static create(dynamicConfig: Config, environment: CircleCIEnvironment): Job {
    dynamicConfig.importOrb(keeper);

    const createDockerContextCommand = CreateDockerContextCommand.get();
    dynamicConfig.addReusableCommand(createDockerContextCommand);

    const parsedGraviteeioVersion = parse(environment.graviteeioVersion);

    const steps: Command[] = [
      new commands.SetupRemoteDocker({ version: config.docker.version }),
      new commands.Checkout(),
      new reusable.ReusedCommand(keeper.commands['env-export'], {
        'secret-url': config.secrets.dockerhubBotUserName,
        'var-name': 'DOCKERHUB_BOT_USER_NAME',
      }),
      new reusable.ReusedCommand(keeper.commands['env-export'], {
        'secret-url': config.secrets.dockerhubBotUserToken,
        'var-name': 'DOCKERHUB_BOT_USER_TOKEN',
      }),
      new reusable.ReusedCommand(createDockerContextCommand),
      new commands.Run({
        name: 'Build & Publish Gravitee.io APIM Docker images',
        command: this.buildAndPublishDockerImages(environment, parsedGraviteeioVersion),
      }),
    ];

    return new Job(PublishProdDockerImagesJob.jobName, BaseExecutor.create(), steps);
  }

  private static buildAndPublishDockerImages(environment: CircleCIEnvironment, graviteeioVersion: GraviteeioVersion): string {
    let command = environment.isDryRun
      ? `echo "DRY RUN Mode. Build only"`
      : `docker login --username="\${DOCKERHUB_BOT_USER_NAME}" -p="\${DOCKERHUB_BOT_USER_TOKEN}"`;

    command += `
${this.dockerBuildCommand(environment, config.dockerImages.gateway, graviteeioVersion)}
${this.dockerBuildCommand(environment, config.dockerImages.managementApi, graviteeioVersion)}
${this.dockerBuildCommand(environment, config.dockerImages.console, graviteeioVersion)}
${this.dockerBuildCommand(environment, config.dockerImages.portal, graviteeioVersion)}
`;

    if (!environment.isDryRun) {
      command += 'docker logout';
    }

    return command;
  }

  private static dockerBuildCommand(
    environment: CircleCIEnvironment,
    { project, image }: { project: string; image: string },
    graviteeioVersion: GraviteeioVersion,
  ) {
    const graviteeioDownloadUrl = this.getGraviteeioDownloadUrl(environment, graviteeioVersion);
    const dockerBuildArgs = `--build-arg GRAVITEEIO_VERSION=${graviteeioVersion.full} --build-arg GRAVITEEIO_DOWNLOAD_URL=${graviteeioDownloadUrl}`;

    let command = 'docker buildx build';

    // Only publish if not dry run
    if (!environment.isDryRun) {
      command += ' --push';
    }

    command += ` --platform=linux/arm64,linux/amd64 ${dockerBuildArgs}`;
    command += ` --quiet ${this.dockerTagArgument(environment, image, graviteeioVersion)}`;
    command += ` ${this.dockerFileArgument(project)}`;

    return command;
  }

  private static dockerTagArgument(environment: CircleCIEnvironment, image: string, graviteeioVersion: GraviteeioVersion): string {
    const stub = ` -t graviteeio/${image}:`;

    // Default tag
    let tag = stub + graviteeioVersion.full;

    if (isBlank(graviteeioVersion.qualifier.full)) {
      // Only major and minor for one tag if no qualifier
      tag += stub + graviteeioVersion.version.major + '.' + graviteeioVersion.version.minor;

      if (environment.dockerTagAsLatest) {
        // Add two tags: major and 'latest'
        tag += stub + graviteeioVersion.version.major + stub + 'latest';
      }
    } else {
      // Include qualifier name after full version
      tag += stub + graviteeioVersion.version.full + '-' + graviteeioVersion.qualifier.name;
    }

    return tag;
  }

  private static getGraviteeioDownloadUrl(environment: CircleCIEnvironment, graviteeioVersion: GraviteeioVersion): string {
    const targetFolder = isBlank(graviteeioVersion.qualifier.full)
      ? '/graviteeio-apim/distributions'
      : '/pre-releases/graviteeio-apim/distributions';
    const downloadHost = environment.isDryRun
      ? 'https://gravitee-dry-releases-downloads.cellar-c2.services.clever-cloud.com'
      : 'https://download.gravitee.io';

    return downloadHost + targetFolder;
  }

  private static dockerFileArgument(project: string) {
    return ` -f ./${project}/docker/Dockerfile-from-download ./${project}/docker`;
  }
}
