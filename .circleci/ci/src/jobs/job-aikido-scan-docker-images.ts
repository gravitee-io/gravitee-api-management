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
import { Command, Config, Job, commands, reusable, workflow } from '../circleci-config';
import { CircleCIEnvironment } from '../pipelines';
import { DockerLoginCommand, DockerLogoutCommand } from '../commands';
import { orbs } from '../orbs';
import { config, Variant } from '../config';
import { UbuntuExecutor } from '../executors';
import { parse } from '../utils';
import { dockerTagsArgument } from './job-build-docker-image';

const IMAGE_LIST_FILE = '/tmp/aikido-images.txt';

/**
 * Scans every image the pipeline has just built and pushed with the Aikido local scanner.
 *
 * The scanner bind-mounts /var/run/docker.sock, which `setup_remote_docker` does not
 * provide, so this cannot be extra steps of the build jobs: it is a single machine
 * executor job pulling the images back from the registry they were pushed to. Scanning
 * them all in one job keeps a single daemon, so the layers the images share (the java and
 * nginx bases, and the variants of a component) are pulled once instead of once per job.
 */
export class AikidoScanDockerImagesJob {
  private static jobName = 'job-aikido-scan-docker-images';

  public static create(dynamicConfig: Config, environment: CircleCIEnvironment, isProd: boolean, imagesToScan: string[]): Job {
    dynamicConfig.importOrb(orbs.keeper).importOrb(orbs.aikido);

    // Non-FIPS images follow isProd (Docker Hub on a release, azurecr otherwise) while
    // the FIPS ones always live on azurecr, so a release needs both logins.
    const dockerLoginCommand = DockerLoginCommand.get(dynamicConfig, environment, isProd);
    dynamicConfig.addReusableCommand(dockerLoginCommand);
    const dockerLogoutCommand = DockerLogoutCommand.get(environment, isProd);
    dynamicConfig.addReusableCommand(dockerLogoutCommand);

    const needsAzurecrLogin = isProd && imagesToScan.some((image) => image.startsWith('graviteeio.azurecr.io/'));
    const steps: Command[] = [
      new reusable.ReusedCommand(orbs.keeper.commands['env-export'], {
        'secret-url': config.secrets.aikidoApiKey,
        'var-name': 'AIKIDO_API_KEY',
      }),
      new reusable.ReusedCommand(dockerLoginCommand),
    ];

    if (needsAzurecrLogin) {
      const azurecrLoginCommand = DockerLoginCommand.get(dynamicConfig, environment, false, 'cmd-docker-login-azurecr');
      dynamicConfig.addReusableCommand(azurecrLoginCommand);
      steps.push(new reusable.ReusedCommand(azurecrLoginCommand));
    }

    steps.push(
      new commands.Run({
        name: 'Pull docker images to scan',
        // The scanner only reads images from the local daemon, and only the host
        // architecture of these multi-arch images is pulled — and therefore scanned.
        //
        // An image that cannot be pulled is dropped from the list rather than failing the
        // step: the scan itself is report only, so losing every other image to one registry
        // hiccup would be the worst outcome. Pulling none of them is an infrastructure
        // failure though, and stays red so the job never reports a silent empty scan.
        command: [
          'IMAGES="',
          ...imagesToScan,
          '"',
          `: > ${IMAGE_LIST_FILE}`,
          'for image in $IMAGES; do',
          `  if docker pull "$image"; then`,
          `    echo "$image" >> ${IMAGE_LIST_FILE}`,
          '  else',
          '    echo "WARN: could not pull $image, it will not be scanned"',
          '  fi',
          'done',
          `if [ ! -s ${IMAGE_LIST_FILE} ]; then`,
          '  echo "ERROR: none of the images could be pulled"',
          '  exit 1',
          'fi',
        ].join('\n'),
      }),
      new reusable.ReusedCommand(orbs.aikido.commands['scan_docker_image'], {
        built_docker_image_file: IMAGE_LIST_FILE,
        // Report only: findings are pushed to the Aikido platform but never break the
        // build. This also keeps every image scanned, since the orb stops at the first
        // image that fails its gate.
        fail_on: '',
      }),
      new reusable.ReusedCommand(dockerLogoutCommand),
    );

    if (needsAzurecrLogin) {
      const azurecrLogoutCommand = DockerLogoutCommand.get(environment, false, 'cmd-docker-logout-azurecr');
      dynamicConfig.addReusableCommand(azurecrLogoutCommand);
      steps.push(new reusable.ReusedCommand(azurecrLogoutCommand));
    }

    return new Job(AikidoScanDockerImagesJob.jobName, UbuntuExecutor.create(), steps);
  }

  /**
   * Declares the scan job and the workflow entry waiting for every build job that pushes
   * an image it scans. `nameSuffix` matches the suffix the build job names carry in that
   * workflow. Nothing is scanned on a release dry run, where no image is pushed.
   */
  public static workflowJobs(
    dynamicConfig: Config,
    environment: CircleCIEnvironment,
    isProd: boolean,
    nameSuffix: string,
    // The chainguard variants this workflow builds, and therefore scans. A workflow scans what it
    // builds: the images to pull and the jobs to wait for are both derived from this list, so the
    // two cannot drift apart.
    chainguardVariants: Variant[],
  ): workflow.WorkflowJob[] {
    if (isProd && environment.isDryRun) {
      return [];
    }

    const components = [
      { label: 'APIM Portal', image: config.components.portal.image, variants: [undefined, ...chainguardVariants] },
      { label: 'APIM Console', image: config.components.console.image, variants: [undefined, ...chainguardVariants] },
      { label: 'Gamma Console', image: config.components.gamma.image, variants: [undefined, ...chainguardVariants] },
      {
        label: 'APIM Management API',
        image: config.components.managementApi.image,
        variants: ['alpine', 'debian', ...chainguardVariants],
      },
      { label: 'APIM Gateway', image: config.components.gateway.image, variants: ['alpine', 'debian', ...chainguardVariants] },
    ] as { label: string; image: string; variants: (Variant | undefined)[] }[];

    const parsedGraviteeioVersion = parse(environment.graviteeioVersion);
    const imagesToScan = components.flatMap(({ image, variants }) =>
      variants.map((variant) => dockerTagsArgument(environment, parsedGraviteeioVersion, isProd, variant, image)[0]),
    );

    const scanJob = AikidoScanDockerImagesJob.create(dynamicConfig, environment, isProd, imagesToScan);
    dynamicConfig.addJob(scanJob);

    return [
      new workflow.WorkflowJob(scanJob, {
        context: config.jobContext,
        name: `Scan docker images with Aikido${nameSuffix}`,
        requires: components.flatMap(({ label }) => [
          `Build ${label} docker image${nameSuffix}`,
          ...chainguardVariants.map((variant) => `Build ${label} ${variant} docker image${nameSuffix}`),
        ]),
      }),
    ];
  }
}
