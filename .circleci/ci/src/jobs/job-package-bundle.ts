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
import { OpenJdkNodeExecutor } from '../executors';
import { orbs } from '../orbs';
import { config } from '../config';
import { ReusedCommand } from '@circleci/circleci-config-sdk/dist/src/lib/Components/Commands/exports/Reusable';
import { keeper } from '../orbs/keeper';
import { awsS3 } from '../orbs/aws-s3';
import { GraviteeioVersion, parse } from '../utils';
import { InstallYarnCommand } from '../commands';

export class PackageBundleJob {
  private static readonly ARTIFACTORY_REPO_URL = `${config.artifactoryUrl}/external-dependencies-n-gravitee-all`;

  public static create(dynamicConfig: Config, graviteeioVersion: string, isDryRun: boolean) {
    dynamicConfig.importOrb(keeper);
    dynamicConfig.importOrb(awsS3);

    const installYarnCmd = InstallYarnCommand.get();
    dynamicConfig.addReusableCommand(installYarnCmd);

    const parsedGraviteeioVersion = parse(graviteeioVersion);

    return new Job('job-package-bundle', OpenJdkNodeExecutor.create(), [
      new reusable.ReusedCommand(orbs.keeper.commands['env-export'], {
        'secret-url': config.secrets.artifactoryUser,
        'var-name': 'ARTIFACTORY_USERNAME',
      }),
      new reusable.ReusedCommand(orbs.keeper.commands['env-export'], {
        'secret-url': config.secrets.artifactoryApiKey,
        'var-name': 'ARTIFACTORY_PASSWORD',
      }),
      new commands.Checkout(),
      new commands.workspace.Attach({ at: '.' }),
      new commands.Run({
        name: `Checkout tag ${parsedGraviteeioVersion.full}`,
        command: `git checkout ${parsedGraviteeioVersion.full}`,
      }),
      new reusable.ReusedCommand(installYarnCmd),
      new commands.Run({
        name: 'Install dependencies',
        command: 'yarn',
        working_directory: './release',
      }),
      new commands.Run({
        name: 'Building package bundle',
        command: `yarn zx --quiet --experimental ci-steps/package-bundles.mjs --version=${parsedGraviteeioVersion.full}`,
        working_directory: './release',
        environment: {
          ARTIFACTORY_REPO_URL: PackageBundleJob.ARTIFACTORY_REPO_URL,
        },
      }),
      new reusable.ReusedCommand(orbs.keeper.commands['env-export'], {
        'secret-url': config.secrets.awsAccessKeyId,
        'var-name': 'AWS_ACCESS_KEY_ID',
      }),
      new reusable.ReusedCommand(orbs.keeper.commands['env-export'], {
        'secret-url': config.secrets.awsSecretAccessKey,
        'var-name': 'AWS_SECRET_ACCESS_KEY',
      }),
      PackageBundleJob.getSyncCommand(parsedGraviteeioVersion, isDryRun),
    ]);
  }

  private static getSyncCommand(graviteeioVersion: GraviteeioVersion, isDryRun: boolean): ReusedCommand {
    const targetFolder =
      graviteeioVersion.qualifier.full && graviteeioVersion.qualifier.full.length > 0
        ? '/pre-releases/graviteeio-apim'
        : '/graviteeio-apim';

    let to = '';
    if (isDryRun) {
      to = `s3://gravitee-dry-releases-downloads${targetFolder}`;
    } else {
      to = `s3://gravitee-releases-downloads${targetFolder}`;
    }
    return new reusable.ReusedCommand(orbs.awsS3.commands.sync, {
      arguments: '--endpoint-url https://cellar-c2.services.clever-cloud.com --acl public-read',
      from: `./release/.tmp/${graviteeioVersion.full}/dist`,
      to: `${to}`,
    });
  }
}
