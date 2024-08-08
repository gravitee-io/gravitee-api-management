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
import { PrepareGpgCmd, RestoreMavenJobCacheCommand, SaveMavenJobCacheCommand } from '../../commands';
import { config } from '../../config';
import { CircleCIEnvironment } from '../../pipelines';

export class BackendBuildAndPublishOnArtifactoryJob {
  private static jobName = 'job-backend-build-and-publish-artifactory';

  public static create(dynamicConfig: Config, environment: CircleCIEnvironment): Job {
    const restoreMavenJobCacheCommand = RestoreMavenJobCacheCommand.get(environment);
    dynamicConfig.addReusableCommand(restoreMavenJobCacheCommand);

    const prepareGpgCommand = PrepareGpgCmd.get(dynamicConfig);
    dynamicConfig.addReusableCommand(prepareGpgCommand);

    const saveMavenJobCacheCommand = SaveMavenJobCacheCommand.get();
    dynamicConfig.addReusableCommand(saveMavenJobCacheCommand);

    const steps: Command[] = [
      new commands.Checkout(),
      new commands.workspace.Attach({ at: '.' }),
      new reusable.ReusedCommand(restoreMavenJobCacheCommand, { jobName: 'job-backend-build-and-publish-artifactory' }),
      new commands.Run({
        name: 'Remove `-SNAPSHOT` from versions',
        command: `mvn -B versions:set -DremoveSnapshot=true -DgenerateBackupPoms=false
sed -i "s#<changelist>.*</changelist>#<changelist></changelist>#" pom.xml`,
      }),
      new reusable.ReusedCommand(prepareGpgCommand),
      new commands.Run({
        name: "Maven deploy to Gravitee's private Artifactory",
        command: `mvn --settings ${config.maven.settingsFile} -B -U -P all-modules,gio-artifactory-release,gio-release,bundle-default clean deploy -DskipTests=true -Dskip.validation -T 4 --no-transfer-progress`,
        environment: {
          BUILD_ID: environment.buildId,
          BUILD_NUMBER: environment.buildNum,
          GIT_COMMIT: environment.sha1,
        },
      }),
      new reusable.ReusedCommand(saveMavenJobCacheCommand, { jobName: 'job-backend-build-and-publish-artifactory' }),
    ];
    return new Job(BackendBuildAndPublishOnArtifactoryJob.jobName, OpenJdkExecutor.create('large'), steps);
  }
}
