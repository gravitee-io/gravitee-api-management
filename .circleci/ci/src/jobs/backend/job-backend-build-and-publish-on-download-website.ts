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

    // Which line this release belongs to, computed here rather than in shell: the version is already
    // parsed for the artefact paths below, and one parser is enough.
    const { version: releasedVersion } = parse(environment.graviteeioVersion);
    const releasedLine = `${releasedVersion.major}.${releasedVersion.minor}`;

    const steps: Command[] = [
      new commands.Checkout(),
      new commands.workspace.Attach({ at: '.' }),
      new reusable.ReusedCommand(restoreMavenJobCacheCommand, { jobName: BackendBuildAndPublishOnDownloadWebsiteJob.jobName }),
      new commands.Run({
        // First, before anything is built: placed after the engine build it fired half an hour into
        // the release. Reading the pin needs no reactor and no installed artifact — the pom parents
        // to the organisation pom with <relativePath/> — and the `versions:set -DremoveSnapshot`
        // below only touches the project version, never this property.
        //
        // What replaces `Check both reactors carry the same version`. A SNAPSHOT is mutable, so a
        // distribution assembled on one is not reproducible: the same tag rebuilt tomorrow would
        // carry a different core. Read through Maven rather than parsed out of the pom — a parsing
        // slip here would let a release through silently, which is the failure this exists to stop.
        name: 'Refuse a core pin this release cannot assemble',
        command: `PIN=$(mvn --settings ${config.maven.settingsFile} -q -N -f gravitee-apim-distribution/pom.xml help:evaluate -Dexpression=apim.core.version -DforceStdout)
echo "Releasing ${environment.graviteeioVersion}, which pins core $PIN"

case "$PIN" in
*-SNAPSHOT)
  echo
  echo "A release cannot assemble a SNAPSHOT core: it is mutable, so this tag would not rebuild to"
  echo "the same artefacts. Release the core first, then merge the pull request that pins it."
  exit 1
  ;;
esac

# The pin may trail the release by a few patches — it moves only when someone decides a core is
# ready — but never by a minor. A pin from another line is a hand-edit, or a branch whose code
# freeze never moved it off the previous line, and either ships a core nobody meant to ship.
PIN_BASE=\${PIN%%-*}
if [ "\${PIN_BASE%.*}" != "${releasedLine}" ]; then
  echo
  echo "core $PIN is not on the ${releasedLine} line, which ${environment.graviteeioVersion} releases."
  echo "Release a ${releasedLine} core and merge the pull request that pins it, or fix the pin by hand."
  exit 1
fi`,
      }),
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
        // Nothing overrides apim.core.version here, so the distribution assembles the core it pins.
        // That is the whole point of pinning: what ships is what someone reviewed and chose, not
        // whatever this build happened to compile.
        name: 'Maven build APIM distribution',
        command: `mvn --settings ${config.maven.settingsFile} -B -nsu -f gravitee-apim-distribution/pom.xml -P gio-release -Dbundle clean verify -DskipTests=true -Dskip.validation -Dgravitee.archrules.skip=true -T 4 --no-transfer-progress`,
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
