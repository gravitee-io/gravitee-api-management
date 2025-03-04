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
import { Config, parameters, reusable } from '@circleci/circleci-config-sdk';
import { ReusableCommand, ReusedCommand } from '@circleci/circleci-config-sdk/dist/src/lib/Components/Commands/exports/Reusable';
import { orbs } from '../orbs';
import { config } from '../config';
import { GraviteeioVersion } from '../utils';

export class SyncFolderToS3Command {
  private static commandName = 'cmd-sync-folder-to-s3';

  private static customParametersList = new parameters.CustomParametersList([
    new parameters.CustomParameter('folder-to-sync', 'string', '', 'the path of the folder to sync to S3'),
  ]);

  public static get(dynamicConfig: Config, graviteeioVersion: GraviteeioVersion, isDryRun: boolean): ReusableCommand {
    dynamicConfig.importOrb(orbs.keeper);
    dynamicConfig.importOrb(orbs.awsCli);
    dynamicConfig.importOrb(orbs.awsS3);

    return new reusable.ReusableCommand(
      SyncFolderToS3Command.commandName,
      [
        new reusable.ReusedCommand(orbs.keeper.commands['env-export'], {
          'secret-url': config.secrets.awsAccessKeyId,
          'var-name': 'AWS_ACCESS_KEY_ID',
        }),
        new reusable.ReusedCommand(orbs.keeper.commands['env-export'], {
          'secret-url': config.secrets.awsSecretAccessKey,
          'var-name': 'AWS_SECRET_ACCESS_KEY',
        }),
        new reusable.ReusedCommand(orbs.awsCli.commands.setup, {
          region: 'cloudfront',
          version: `${config.awsCliVersion}`,
        }),
        SyncFolderToS3Command.getSyncCommand(graviteeioVersion, isDryRun),
      ],
      SyncFolderToS3Command.customParametersList,
      'Sync folder content to Gravitee download website',
    );
  }

  private static getSyncCommand(graviteeioVersion: GraviteeioVersion, isDryRun: boolean): ReusedCommand {
    const targetFolder = graviteeioVersion.qualifier.full && graviteeioVersion.qualifier.full.length > 0 ? '/pre-releases' : '';

    let to = '';
    if (isDryRun) {
      to = `s3://gravitee-dry-releases-downloads${targetFolder}`;
    } else {
      to = `s3://gravitee-releases-downloads${targetFolder}`;
    }
    return new reusable.ReusedCommand(orbs.awsS3.commands.sync, {
      arguments: '--endpoint-url https://cellar-c2.services.clever-cloud.com --acl public-read',
      from: '<< parameters.folder-to-sync>>',
      to: `${to}`,
    });
  }
}
