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
import { keeper } from '../orbs/keeper';
import { awsCli } from '../orbs/aws-cli';
import { awsS3 } from '../orbs/aws-s3';
import { parse } from '../utils';
import { InstallYarnCommand } from '../commands';
import { SyncFolderToS3Command } from '../commands/cmd-sync-folder-to-s3';

export class PackageBundleJob {
  private static readonly ARTIFACTORY_REPO_URL = `${config.artifactoryUrl}/external-dependencies-n-gravitee-all`;

  public static create(dynamicConfig: Config, graviteeioVersion: string, isDryRun: boolean) {
    dynamicConfig.importOrb(keeper);
    dynamicConfig.importOrb(awsS3);
    dynamicConfig.importOrb(awsCli);

    const parsedGraviteeioVersion = parse(graviteeioVersion);

    const installYarnCmd = InstallYarnCommand.get();
    const syncFolderToS3Cmd = SyncFolderToS3Command.get(dynamicConfig, parsedGraviteeioVersion, isDryRun);
    dynamicConfig.addReusableCommand(installYarnCmd);
    dynamicConfig.addReusableCommand(syncFolderToS3Cmd);

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
      new reusable.ReusedCommand(syncFolderToS3Cmd, {
        'folder-to-sync': `./release/.tmp/${parsedGraviteeioVersion.full}/dist`,
      }),
    ]);
  }
}
