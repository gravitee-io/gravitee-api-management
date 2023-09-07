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
      new commands.SetupRemoteDocker(),
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

  private static dockerBuildCommand(environment: CircleCIEnvironment, image: string, graviteeioVersion: GraviteeioVersion) {
    const graviteeioDownloadUrl = this.getGraviteeioDownloadUrl(environment, graviteeioVersion);
    const dockerBuildArgs = `--build-arg GRAVITEEIO_VERSION=${graviteeioVersion.full} --build-arg GRAVITEEIO_DOWNLOAD_URL=${graviteeioDownloadUrl}`;

    let command = 'docker buildx build';

    // Only publish if not dry run
    if (!environment.isDryRun) {
      command += ' --push';
    }

    command += ` --platform=linux/arm64,linux/amd64 ${dockerBuildArgs}`;
    command += ` --quiet ${this.dockerTagArgument(environment, image, graviteeioVersion)}`;
    command += ` ${this.dockerFileArgument(image)}`;

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
    const targetFolder = isBlank(graviteeioVersion.qualifier.full) ? '/graviteeio-apim' : '/pre-releases/graviteeio-apim';
    const downloadHost = environment.isDryRun
      ? 'https://gravitee-dry-releases-downloads.cellar-c2.services.clever-cloud.com'
      : 'https://download.gravitee.io';

    return downloadHost + targetFolder;
  }

  private static dockerFileArgument(image: string) {
    return ` -f ./gravitee-${image}/docker/Dockerfile-from-download ./gravitee-${image}/docker`;
  }
}
