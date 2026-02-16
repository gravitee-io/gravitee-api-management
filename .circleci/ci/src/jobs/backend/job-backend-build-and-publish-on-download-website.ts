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
import { OpenJdkExecutor } from '../../executors';
import { Command } from '@circleci/circleci-config-sdk/dist/src/lib/Components/Commands/exports/Command';
import { PrepareGpgCmd, RestoreMavenJobCacheCommand, SaveMavenJobCacheCommand, SyncFolderToS3Command } from '../../commands';
import { config } from '../../config';
import { CircleCIEnvironment } from '../../pipelines';
import { parse } from '../../utils';

export class BackendBuildAndPublishOnDownloadWebsiteJob {
  private static jobName = 'job-backend-build-and-publish-on-download-website';

  public static create(dynamicConfig: Config, environment: CircleCIEnvironment, publishOnDownloadWebsite: boolean): Job {
    const restoreMavenJobCacheCommand = RestoreMavenJobCacheCommand.get(environment);
    dynamicConfig.addReusableCommand(restoreMavenJobCacheCommand);

    const prepareGpgCommand = PrepareGpgCmd.get(dynamicConfig);
    dynamicConfig.addReusableCommand(prepareGpgCommand);

    const saveMavenJobCacheCommand = SaveMavenJobCacheCommand.get();
    dynamicConfig.addReusableCommand(saveMavenJobCacheCommand);

    const steps: Command[] = [
      new commands.Checkout(),
      new commands.workspace.Attach({ at: '.' }),
      new reusable.ReusedCommand(restoreMavenJobCacheCommand, { jobName: BackendBuildAndPublishOnDownloadWebsiteJob.jobName }),
      new commands.Run({
        name: 'Remove `-SNAPSHOT` from versions',
        command: `mvn -B versions:set -DremoveSnapshot=true -DgenerateBackupPoms=false
sed -i "s#<changelist>.*</changelist>#<changelist></changelist>#" pom.xml`,
      }),
      new reusable.ReusedCommand(prepareGpgCommand),
      new commands.Run({
        name: 'Maven build APIM backend',
        command: `mvn --settings ${config.maven.settingsFile} -B -U -P all-modules,gio-release,bundle-default clean verify -DskipTests=true -Dskip.validation -Dgravitee.archrules.skip=true -T 4 --no-transfer-progress`,
        environment: {
          BUILD_ID: environment.buildId,
          BUILD_NUMBER: environment.buildNum,
          GIT_COMMIT: environment.sha1,
        },
      }),
      new reusable.ReusedCommand(saveMavenJobCacheCommand, { jobName: BackendBuildAndPublishOnDownloadWebsiteJob.jobName }),
    ];
    if (publishOnDownloadWebsite) {
      const syncFolderToS3Cmd = SyncFolderToS3Command.get(dynamicConfig, parse(environment.graviteeioVersion), environment.isDryRun);
      dynamicConfig.addReusableCommand(syncFolderToS3Cmd);

      steps.push(
        /**
         * In order to upload repositories, endpoints and entrypoints embedded in APIM mono-repository, we browse for all ZIP files in the project and check if they have a "publish folder path" property in pom.xml.
         * Because we don't want to publish EVERY plugins (we don't want, apim-services or rest-api-idp-memory for instance), we only rely on this publish-folder-path maven property to determine if a ZIP has to be published or not.
         * Each plugins is uploaded into a folder based on its name.
         * Example:
         *   gravitee-apim-repository-mongodb-x.x.x.zip is published into graviteeio-apim/plugins/repositories/gravitee-apim-repository-mongodb
         *
         *
         */
        new commands.Run({
          name: 'Prepare plugin zip to upload',
          command: `workingDir=$(pwd)
for pathToArtefactFile in $(find . -path '*target/gravitee-apim*.zip'); do
  # Extract folder of the artefact to publish
  # e.g. ./gravitee-apim-repository/gravitee-apim-repository-mongodb/target/gravitee-apim-repository-mongodb-4.4.21.zip => ./gravitee-apim-repository/gravitee-apim-repository-mongodb
  artefactFolder=\${pathToArtefactFile%/target*}

  # extract publish folder from pom.xml properties, return '/' if no property found
  publishFolderPath=/$(grep -Po '(?<=<publish-folder-path>).*(?=</publish-folder-path>)' $artefactFolder/pom.xml || echo '')

  if [[ "$publishFolderPath" != "/" ]]; then
    # extract artefact file of the artefact to publish
    # e.g. ./gravitee-apim-repository/gravitee-apim-repository-mongodb/target/gravitee-apim-repository-mongodb-4.4.21.zip => gravitee-apim-repository-mongodb-4.4.21.zip
    artefactFile=\${pathToArtefactFile##*/}

    regex="(.*)-[0-9]+.[0-9]+.[0-9]+(-(alpha|beta|rc).[0-9]+)?"
    [[ $artefactFile =~ $regex ]]
    artefactName=\${BASH_REMATCH[1]}

    # compute the destination folder on S3 to publish the artefact
    # e.g. gravitee-apim-repository-mongodb-4.4.21.zip => folder_to_sync/graviteeio-apim/plugins/repositories/gravitee-apim-repository-mongodb
    artefactFolderToSync=folder_to_sync\${publishFolderPath}/\${artefactName}

    mkdir -p $artefactFolderToSync
    cp $pathToArtefactFile $artefactFolderToSync/

    cd $artefactFolderToSync

    md5sum $artefactFile > $artefactFile.md5
    sha512sum $artefactFile > $artefactFile.sha512sum
    sha1sum $artefactFile > $artefactFile.sha1

    cd $workingDir
  fi
done`,
        }),
      );
    }
    steps.push(
      new commands.workspace.Persist({
        root: '.',
        paths: [
          './gravitee-apim-rest-api/gravitee-apim-rest-api-standalone/gravitee-apim-rest-api-standalone-distribution/target/distribution',
          './gravitee-apim-gateway/gravitee-apim-gateway-standalone/gravitee-apim-gateway-standalone-distribution/target/distribution',
        ],
      }),
    );
    return new Job(BackendBuildAndPublishOnDownloadWebsiteJob.jobName, OpenJdkExecutor.create('large'), steps);
  }
}
