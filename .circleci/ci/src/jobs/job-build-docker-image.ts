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
import { CreateDockerContextCommand, DockerLoginCommand, DockerLogoutCommand } from '../commands';
import { orbs } from '../orbs';
import { config, Variant } from '../config';
import { BaseExecutor } from '../executors';

export class BuildDockerWebUiImageJob {
  private static jobName = 'job-build-docker-webui-image';

  private static customParametersList = new parameters.CustomParametersList([
    new parameters.CustomParameter('apim-project', 'string', '', 'the name of the project to build'),
    new parameters.CustomParameter('apim-project-workdir', 'string', '', 'the directory of the project to build'),
    new parameters.CustomParameter('docker-context', 'string', '', 'the name of context folder for docker build'),
    new parameters.CustomParameter('docker-image-name', 'string', '', 'the name of the image'),
  ]);

  public static create(dynamicConfig: Config, environment: CircleCIEnvironment, isProd: boolean): reusable.ParameterizedJob {
    dynamicConfig.importOrb(orbs.keeper).importOrb(orbs.aquasec);

    const createDockerContextCommand = CreateDockerContextCommand.get();
    dynamicConfig.addReusableCommand(createDockerContextCommand);

    const dockerLoginCommand = DockerLoginCommand.get(dynamicConfig, environment, isProd);
    dynamicConfig.addReusableCommand(dockerLoginCommand);

    const dockerLogoutCommand = DockerLogoutCommand.get(environment, isProd);
    dynamicConfig.addReusableCommand(dockerLogoutCommand);

    const parsedGraviteeioVersion = parse(environment.graviteeioVersion);

    const dockerTags: string[] = dockerTagsArgument(environment, parsedGraviteeioVersion, isProd);
    return new reusable.ParameterizedJob(
      BuildDockerWebUiImageJob.jobName,
      BaseExecutor.create(),
      BuildDockerWebUiImageJob.customParametersList,
      [
        new commands.Checkout(),
        new commands.workspace.Attach({ at: '.' }),
        new commands.SetupRemoteDocker({ version: config.docker.version }),
        new reusable.ReusedCommand(createDockerContextCommand),
        new reusable.ReusedCommand(dockerLoginCommand),
        new commands.Run({
          name: 'Build docker image for << parameters.apim-project >>',
          command: `${dockerBuildCommand(environment, dockerTags, isProd)}`,
          working_directory: '<< parameters.apim-project-workdir >>',
        }),
        ...(isProd || isSupportBranchOrMaster(environment.branch)
          ? [
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
              }),
              new reusable.ReusedCommand(orbs.aquasec.commands['scan_docker_image'], {
                docker_image_to_scan: `${dockerTags[0]}`,
                scanner_url: config.aqua.scannerUrl,
              }),
            ]
          : []),
        new reusable.ReusedCommand(dockerLogoutCommand),
      ],
    );
  }
}

export class BuildDockerBackendImageJob {
  private static jobName = 'job-build-docker-backend-image';

  private static customParametersList = new parameters.CustomParametersList([
    new parameters.CustomParameter('apim-project', 'string', '', 'the name of the project to build'),
    new parameters.CustomParameter('apim-project-workdir', 'string', '', 'the directory of the project to build'),
    new parameters.CustomParameter('docker-context', 'string', '', 'the name of context folder for docker build'),
    new parameters.CustomParameter('docker-image-name', 'string', '', 'the name of the image'),
  ]);

  public static create(dynamicConfig: Config, environment: CircleCIEnvironment, isProd: boolean): reusable.ParameterizedJob {
    dynamicConfig.importOrb(orbs.keeper).importOrb(orbs.aquasec);

    const createDockerContextCommand = CreateDockerContextCommand.get();
    dynamicConfig.addReusableCommand(createDockerContextCommand);

    const dockerLoginCommand = DockerLoginCommand.get(dynamicConfig, environment, isProd);
    dynamicConfig.addReusableCommand(dockerLoginCommand);

    const dockerLogoutCommand = DockerLogoutCommand.get(environment, isProd);
    dynamicConfig.addReusableCommand(dockerLogoutCommand);

    const parsedGraviteeioVersion = parse(environment.graviteeioVersion);

    const variants: Variant[] = ['alpine', 'debian'];
    return new reusable.ParameterizedJob(
      BuildDockerBackendImageJob.jobName,
      BaseExecutor.create(),
      BuildDockerBackendImageJob.customParametersList,
      [
        new commands.Checkout(),
        new commands.workspace.Attach({ at: '.' }),
        new commands.SetupRemoteDocker({ version: config.docker.version }),
        new reusable.ReusedCommand(createDockerContextCommand),
        new reusable.ReusedCommand(dockerLoginCommand),
        ...aquaSetupCommands(),
        ...variants.flatMap((variant) => {
          const dockerTags: string[] = dockerTagsArgument(environment, parsedGraviteeioVersion, isProd, variant);
          return [
            new commands.Run({
              name: `Build docker image for << parameters.apim-project >>-${variant}`,
              command: `${dockerBuildCommand(environment, dockerTags, isProd, variant)}`,
              working_directory: '<< parameters.apim-project-workdir >>',
            }),
            ...(isProd || isSupportBranchOrMaster(environment.branch)
              ? [
                  new reusable.ReusedCommand(orbs.aquasec.commands['register_artifact'], {
                    artifact_to_register: `${dockerTags[0]}`,
                  }),
                  new reusable.ReusedCommand(orbs.aquasec.commands['scan_docker_image'], {
                    docker_image_to_scan: `${dockerTags[0]}`,
                    scanner_url: config.aqua.scannerUrl,
                  }),
                ]
              : []),
          ];
        }),
        new reusable.ReusedCommand(dockerLogoutCommand),
      ],
    );
  }
}

export class BuildDockerChainguardImageJob {
  private static jobName = 'job-build-docker-chainguard-image';

  private static customParametersList = new parameters.CustomParametersList([
    new parameters.CustomParameter('apim-project', 'string', '', 'the name of the project to build'),
    new parameters.CustomParameter('apim-project-workdir', 'string', '', 'the directory of the project to build'),
    new parameters.CustomParameter('docker-context', 'string', '', 'the name of context folder for docker build'),
    new parameters.CustomParameter('docker-image-name', 'string', '', 'the name of the image'),
  ]);

  public static create(dynamicConfig: Config, environment: CircleCIEnvironment, isProd: boolean): reusable.ParameterizedJob {
    dynamicConfig.importOrb(orbs.keeper);

    const createDockerContextCommand = CreateDockerContextCommand.get();
    dynamicConfig.addReusableCommand(createDockerContextCommand);

    // Push registry follows isProd (Docker Hub when prod, azurecr otherwise), like
    // the alpine/debian variants — only the '-chainguard' suffix and Dockerfile differ.
    const dockerLoginCommand = DockerLoginCommand.get(dynamicConfig, environment, isProd);
    dynamicConfig.addReusableCommand(dockerLoginCommand);
    const dockerLogoutCommand = DockerLogoutCommand.get(environment, isProd);
    dynamicConfig.addReusableCommand(dockerLogoutCommand);

    // The chainguard base image (graviteeio.azurecr.io/*-chainguard) stays private, so
    // when pushing the component to Docker Hub (isProd) we also need an azurecr login to
    // pull the base. When !isProd the single azurecr login above already covers pull+push.
    const azurecrPullLoginCommand = DockerLoginCommand.get(dynamicConfig, environment, false, 'cmd-docker-login-azurecr');
    const azurecrPullLogoutCommand = DockerLogoutCommand.get(environment, false, 'cmd-docker-logout-azurecr');
    if (isProd) {
      dynamicConfig.addReusableCommand(azurecrPullLoginCommand);
      dynamicConfig.addReusableCommand(azurecrPullLogoutCommand);
    }

    const parsedGraviteeioVersion = parse(environment.graviteeioVersion);
    const dockerTags: string[] = dockerTagsArgument(environment, parsedGraviteeioVersion, isProd, 'chainguard');

    return new reusable.ParameterizedJob(
      BuildDockerChainguardImageJob.jobName,
      BaseExecutor.create(),
      BuildDockerChainguardImageJob.customParametersList,
      [
        new commands.Checkout(),
        new commands.workspace.Attach({ at: '.' }),
        new commands.SetupRemoteDocker({ version: config.docker.version }),
        new reusable.ReusedCommand(createDockerContextCommand),
        ...(isProd ? [new reusable.ReusedCommand(azurecrPullLoginCommand)] : []),
        new reusable.ReusedCommand(dockerLoginCommand),
        new commands.Run({
          name: 'Build Chainguard docker image for << parameters.apim-project >>',
          command: `${dockerBuildCommand(environment, dockerTags, isProd, 'chainguard')}`,
          working_directory: '<< parameters.apim-project-workdir >>',
        }),
        new reusable.ReusedCommand(dockerLogoutCommand),
        ...(isProd ? [new reusable.ReusedCommand(azurecrPullLogoutCommand)] : []),
      ],
    );
  }
}

export class BuildDockerChainguardFipsImageJob {
  private static jobName = 'job-build-docker-chainguard-fips-image';

  private static customParametersList = new parameters.CustomParametersList([
    new parameters.CustomParameter('apim-project', 'string', '', 'the name of the project to build'),
    new parameters.CustomParameter('apim-project-workdir', 'string', '', 'the directory of the project to build'),
    new parameters.CustomParameter('docker-context', 'string', '', 'the name of context folder for docker build'),
    new parameters.CustomParameter('docker-image-name', 'string', '', 'the name of the image'),
    new parameters.CustomParameter(
      'docker-fips-base-image',
      'string',
      '',
      'the FIPS base image passed as BASE_IMAGE build-arg (java or nginx chainguard-fips)',
    ),
  ]);

  public static create(dynamicConfig: Config, environment: CircleCIEnvironment, isProd: boolean): reusable.ParameterizedJob {
    dynamicConfig.importOrb(orbs.keeper);

    const createDockerContextCommand = CreateDockerContextCommand.get();
    dynamicConfig.addReusableCommand(createDockerContextCommand);

    // FIPS images are always published to the private Azure registry (never Docker Hub),
    // and both the chainguard-fips base and the produced image live on azurecr — so a
    // single azurecr login covers pull + push regardless of release mode.
    // Register under a distinct command name ('cmd-docker-login-azurecr') so this azurecr
    // login does not overwrite the shared 'cmd-docker-login' (Docker Hub) used by the
    // isProd jobs sharing the same dynamicConfig in build-docker-images / full-release.
    const dockerLoginCommand = DockerLoginCommand.get(dynamicConfig, environment, false, 'cmd-docker-login-azurecr');
    dynamicConfig.addReusableCommand(dockerLoginCommand);
    const dockerLogoutCommand = DockerLogoutCommand.get(environment, false, 'cmd-docker-logout-azurecr');
    dynamicConfig.addReusableCommand(dockerLogoutCommand);

    const parsedGraviteeioVersion = parse(environment.graviteeioVersion);
    const dockerTags: string[] = dockerTagsArgument(environment, parsedGraviteeioVersion, isProd, 'chainguard-fips');

    return new reusable.ParameterizedJob(
      BuildDockerChainguardFipsImageJob.jobName,
      BaseExecutor.create(),
      BuildDockerChainguardFipsImageJob.customParametersList,
      [
        new commands.Checkout(),
        new commands.workspace.Attach({ at: '.' }),
        new commands.SetupRemoteDocker({ version: config.docker.version }),
        new reusable.ReusedCommand(createDockerContextCommand),
        new reusable.ReusedCommand(dockerLoginCommand),
        new commands.Run({
          name: 'Build Chainguard FIPS docker image for << parameters.apim-project >>',
          command: `${dockerBuildCommand(environment, dockerTags, isProd, 'chainguard-fips')}`,
          working_directory: '<< parameters.apim-project-workdir >>',
        }),
        new reusable.ReusedCommand(dockerLogoutCommand),
      ],
    );
  }
}

function aquaSetupCommands() {
  return [
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
  ];
}

function dockerBuildCommand(environment: CircleCIEnvironment, dockerTags: string[], isProd: boolean, variant?: Variant) {
  let command = 'docker buildx build';

  let dockerfile = 'docker/Dockerfile';
  if (variant === 'debian') {
    dockerfile = 'docker/Dockerfile.debian';
  } else if (variant === 'chainguard' || variant === 'chainguard-fips') {
    // The FIPS variant reuses the chainguard Dockerfile; only the BASE_IMAGE build-arg differs.
    dockerfile = 'docker/Dockerfile.chainguard';
  }

  // Only publish if not dry run or not prod
  if (!isProd || !environment.isDryRun) {
    command += ' --push';
  }

  if (isProd) {
    command += ` --quiet`;
  }

  command += ` --platform=linux/arm64,linux/amd64 -f ${dockerfile} \\\n`;

  if (variant === 'chainguard-fips') {
    // The FIPS base (java or nginx) is supplied per-component via the docker-fips-base-image
    // job parameter; the chainguard Dockerfiles all read it through the BASE_IMAGE arg.
    command += `--build-arg BASE_IMAGE=<< parameters.docker-fips-base-image >> \\\n`;
  }

  command += `${dockerTags.map((t) => `-t ${t}`).join(' ')} \\\n`;
  command += `<< parameters.docker-context >>`;

  return command;
}

function dockerTagsArgument(
  environment: CircleCIEnvironment,
  graviteeioVersion: GraviteeioVersion,
  isProd: boolean,
  variant?: Variant,
): string[] {
  const tags: string[] = [];
  let suffix = '';
  if (variant === 'debian') {
    suffix = '-debian';
  } else if (variant === 'chainguard') {
    suffix = '-chainguard';
  } else if (variant === 'chainguard-fips') {
    suffix = '-chainguard-fips';
  }
  // FIPS images are never published to Docker Hub, only to the private Azure registry
  // (even for a release). Keep the isProd version-tag logic but swap the registry stub.
  const isFips = variant === 'chainguard-fips';
  if (isProd) {
    const stub = isFips ? `graviteeio.azurecr.io/<< parameters.docker-image-name >>:` : `graviteeio/<< parameters.docker-image-name >>:`;

    // Default tag
    tags.push(stub + graviteeioVersion.full + suffix);

    if (isBlank(graviteeioVersion.qualifier.full)) {
      // Only major and minor for one tag if no qualifier
      tags.push(stub + graviteeioVersion.version.major + '.' + graviteeioVersion.version.minor + suffix);

      if (environment.dockerTagAsLatest) {
        // Add two tags: major and 'latest'
        tags.push(stub + graviteeioVersion.version.major + suffix);
        tags.push(stub + 'latest' + suffix);
      }
    } else {
      // Include qualifier name after full version
      tags.push(stub + graviteeioVersion.version.full + '-' + graviteeioVersion.qualifier.name + suffix);
    }
  } else if (isSupportBranchOrMaster(environment.branch)) {
    // master-latest
    tags.push(`graviteeio.azurecr.io/<< parameters.docker-image-name >>:${computeImagesTag(environment.branch)}${suffix}`);
    // master-sha1
    tags.push(
      `graviteeio.azurecr.io/<< parameters.docker-image-name >>:${computeImagesTag(environment.branch, environment.sha1)}${suffix}`,
    );
  } else {
    const tag = computeImagesTag(environment.branch, environment.sha1);
    tags.push(`graviteeio.azurecr.io/<< parameters.docker-image-name >>:${tag}${suffix}`);
  }
  return tags;
}
