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
import { Command, Config, Job, commands, reusable } from '../../circleci-config';
import { OpenJdkNodeExecutor } from '../../executors';
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
        // The distribution carries its own version properties now, so it needs the same treatment.
        name: 'Remove `-SNAPSHOT` from versions',
        command: `mvn -B versions:set -DremoveSnapshot=true -DgenerateBackupPoms=false
sed -i "s#<changelist>.*</changelist>#<changelist></changelist>#" pom.xml
mvn -B -f gravitee-apim-distribution/pom.xml versions:set -DremoveSnapshot=true -DgenerateBackupPoms=false
sed -i "s#<changelist>.*</changelist>#<changelist></changelist>#" gravitee-apim-distribution/pom.xml`,
      }),
      new reusable.ReusedCommand(prepareGpgCommand),
      new commands.Run({
        // install, not verify: the distribution resolves the engine from the local repository.
        name: 'Maven build APIM engine',
        command: `mvn --settings ${config.maven.settingsFile} -B -U -P all-modules,gio-release clean install -DskipTests=true -Dskip.validation -Dgravitee.archrules.skip=true -T 4 --no-transfer-progress`,
        environment: {
          BUILD_ID: environment.buildId,
          BUILD_NUMBER: environment.buildNum,
          GIT_COMMIT: environment.sha1,
        },
      }),
      new commands.Run({
        // -Dbundle, not -P bundle-default: the profile that adds the Cloud initializer and the MCP
        // libraries to lib/ is declared by the gateway container, which is now an external
        // dependency. -P only activates profiles of the projects in the reactor, so it no longer
        // reaches it; the property activation does. Without this the released zip and images ship
        // without those two jars, and no pull-request build would show it — job-build-backend
        // passes -Dbundle=dev and so activates the profile by property already.
        // engine-snapshot resolves ${revision}${sha1}${changelist}, which the step above has just
        // set to the version being released: the distribution ships the engine this build produced,
        // not the one its pom is pinned to.
        name: 'Maven build APIM distribution',
        command: `mvn --settings ${config.maven.settingsFile} -B -nsu -f gravitee-apim-distribution/pom.xml -P gio-release,engine-snapshot -Dbundle clean verify -DskipTests=true -Dskip.validation -Dgravitee.archrules.skip=true -T 4 --no-transfer-progress`,
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
         * We browse the distribution reactor for ZIP files and check if they have a "publish folder path" property in
         * pom.xml. Because we don't want to publish every artefact, we only rely on that maven property to determine
         * whether a ZIP has to be published. Each artefact is uploaded into a folder based on its name.
         * Example:
         *   gravitee-apim-jdbc-migrations-x.x.x.zip is published into graviteeio-apim/resources/gravitee-apim-jdbc-migrations
         *
         * The search is scoped to gravitee-apim-distribution on purpose. Repositories, reporters, endpoints and
         * entrypoints also carry publish-folder-path, and have not been published since 4.8 — widening the search
         * here would silently resume publishing thirteen more artefacts, which is a product decision of its own.
         */
        new commands.Run({
          name: 'Prepare plugin zip to upload',
          command: `workingDir=$(pwd)
for pathToArtefactFile in $(find ./gravitee-apim-distribution -path '*target/gravitee-apim*.zip'); do
  # Extract folder of the artefact to publish
  # e.g. ./gravitee-apim-repository/gravitee-apim-repository-mongodb/target/gravitee-apim-repository-mongodb-4.4.21.zip => ./gravitee-apim-repository/gravitee-apim-repository-mongodb
  artefactFolder=\${pathToArtefactFile%/target*}

  # extract publish folder from pom.xml properties, return '/' if no property found
  publishFolderPath=/$(grep -Po '(?<=<publish-folder-path>).*(?=</publish-folder-path>)' $artefactFolder/pom.xml || echo '')

  if [[ "$publishFolderPath" != "/" ]]; then
    # extract artefact file of the artefact to publish
    # e.g. ./gravitee-apim-repository/gravitee-apim-repository-mongodb/target/gravitee-apim-repository-mongodb-4.4.21.zip => gravitee-apim-repository-mongodb-4.4.21.zip
    artefactFile=\${pathToArtefactFile##*/}

    regex="(.*)-[0-9]+.[0-9]+.[0-9]+(-(alpha|beta|milestone|rc).[0-9]+)?"
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
        new reusable.ReusedCommand(syncFolderToS3Cmd, {
          'folder-to-sync': 'folder_to_sync',
        }),
      );
    }
    steps.push(
      new commands.workspace.Persist({
        root: '.',
        paths: [
          './gravitee-apim-distribution/gravitee-apim-distribution-standalone/gravitee-apim-distribution-standalone-rest-api/target/distribution',
          './gravitee-apim-distribution/gravitee-apim-distribution-standalone/gravitee-apim-distribution-standalone-gateway/target/distribution',
        ],
      }),
    );
    return new Job(BackendBuildAndPublishOnDownloadWebsiteJob.jobName, OpenJdkNodeExecutor.create('large'), steps);
  }
}
