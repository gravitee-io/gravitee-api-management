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
import { ReusableCommand } from '@circleci/circleci-config-sdk/dist/src/lib/Components/Commands/exports/Reusable';
import { orbs } from '../orbs';
import { SyncFolderToS3Command } from './cmd-sync-folder-to-s3';
import { parse } from '../utils';

export class WebuiPublishOnDownloadWebsiteCommand {
  private static commandName = 'cmd-webui-publish-on-download-website';

  private static customParametersList = new parameters.CustomParametersList([
    new parameters.CustomParameter('apim-ui-project', 'string', '', 'the name of the UI project to build'),
    new parameters.CustomParameter('apim-ui-publish-folder-path', 'string', '', 'the path to publication folder in download website'),
  ]);

  public static get(dynamicConfig: Config, graviteeioVersion: string, isDryRun: boolean): ReusableCommand {
    dynamicConfig.importOrb(orbs.keeper);
    dynamicConfig.importOrb(orbs.awsS3);
    dynamicConfig.importOrb(orbs.awsCli);

    const parsedGraviteeioVersion = parse(graviteeioVersion);

    const syncFolderToS3Cmd = SyncFolderToS3Command.get(dynamicConfig, parsedGraviteeioVersion, isDryRun);
    dynamicConfig.addReusableCommand(syncFolderToS3Cmd);

    return new reusable.ReusableCommand(
      WebuiPublishOnDownloadWebsiteCommand.commandName,
      [
        new commands.Run({
          name: 'Prepare zip to upload',
          command: `cp -r dist << parameters.apim-ui-project >>-${graviteeioVersion}
zip -r << parameters.apim-ui-project >>-${graviteeioVersion}.zip << parameters.apim-ui-project >>-${graviteeioVersion}

mkdir -p folder_to_sync/<< parameters.apim-ui-publish-folder-path >>
mv << parameters.apim-ui-project >>-${graviteeioVersion}.zip folder_to_sync/<< parameters.apim-ui-publish-folder-path >>/<< parameters.apim-ui-project >>-${graviteeioVersion}.zip
cd folder_to_sync/<< parameters.apim-ui-publish-folder-path >>

md5sum << parameters.apim-ui-project >>-${graviteeioVersion}.zip > << parameters.apim-ui-project >>-${graviteeioVersion}.zip.md5
sha512sum << parameters.apim-ui-project >>-${graviteeioVersion}.zip > << parameters.apim-ui-project >>-${graviteeioVersion}.zip.sha512sum
sha1sum << parameters.apim-ui-project >>-${graviteeioVersion}.zip > << parameters.apim-ui-project >>-${graviteeioVersion}.zip.sha1
`,
          working_directory: '<< parameters.apim-ui-project >>',
        }),

        new reusable.ReusedCommand(syncFolderToS3Cmd, {
          'folder-to-sync': '<< parameters.apim-ui-project >>/folder_to_sync',
        }),
      ],
      WebuiPublishOnDownloadWebsiteCommand.customParametersList,
    );
  }
}
