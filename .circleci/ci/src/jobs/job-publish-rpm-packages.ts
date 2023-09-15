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
import { Command } from '@circleci/circleci-config-sdk/dist/src/lib/Components/Commands/exports/Command';
import { UbuntuExecutor } from '../executors';
import { keeper } from '../orbs/keeper';
import { config } from '../config';

export class PublishRpmPackagesJob {
  private static jobName = 'job-publish-rpm-packages';
  public static create(dynamicConfig: Config, environment: CircleCIEnvironment): Job {
    dynamicConfig.importOrb(keeper);

    const steps: Command[] = [
      new reusable.ReusedCommand(keeper.commands['env-export'], {
        'secret-url': config.secrets.graviteePackageCloudToken,
        'var-name': 'GIO_PACKAGECLOUD_TOKEN',
      }),
      new commands.Run({
        name: 'Building and publishing RPMs',
        command: this.getBuildingAndPublishingRPMsCmd(environment),
      }),
    ];

    return new Job(PublishRpmPackagesJob.jobName, UbuntuExecutor.create(), steps);
  }

  private static getBuildingAndPublishingRPMsCmd(environment: CircleCIEnvironment) {
    let cmd = `export GIT_GRAVITEE_PACKAGES_REPO=$(mktemp -d)
git clone git@github.com:gravitee-io/packages.git \${GIT_GRAVITEE_PACKAGES_REPO}

cd \${GIT_GRAVITEE_PACKAGES_REPO}/apim/4.x
./build.sh -v ${environment.graviteeioVersion}
`;

    let publishLocation = '';
    if (environment.isDryRun) {
      cmd += `echo "This is just a DRY RUN, RPMs will be published in https://packagecloud.io/graviteeio/nightly"
`;
      publishLocation = 'nightly';
    } else {
      publishLocation = 'rpms';
    }

    cmd += `docker run --rm -v "\${GIT_GRAVITEE_PACKAGES_REPO}/apim/4.x:/packages" -e PACKAGECLOUD_TOKEN=\${GIO_PACKAGECLOUD_TOKEN} digitalocean/packagecloud push --yes --skip-errors --verbose graviteeio/${publishLocation}/el/7 /packages/*.rpm`;

    return cmd;
  }
}
