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
import { Command } from '@circleci/circleci-config-sdk/dist/src/lib/Components/Commands/exports/Command';
import { config } from '../../config';
import { AzureArtifactsTokenCommand, NotifyOnFailureCommand, RestoreMavenJobCacheCommand, SaveMavenJobCacheCommand } from '../../commands';
import { OpenJdkExecutor } from '../../executors';
import { CircleCIEnvironment } from '../../pipelines';
import { mavenParallelism } from '../../utils';

export class PublishJob {
  public static create(dynamicConfig: Config, environment: CircleCIEnvironment): Job {
    const jobName = 'job-publish-snapshot';

    const restoreMavenJobCacheCmd = RestoreMavenJobCacheCommand.get(environment);
    const saveMavenJobCacheCmd = SaveMavenJobCacheCommand.get();
    const notifyOnFailureCmd = NotifyOnFailureCommand.get(dynamicConfig, environment);
    const azureArtifactsTokenCmd = AzureArtifactsTokenCommand.get(dynamicConfig);
    dynamicConfig.addReusableCommand(restoreMavenJobCacheCmd);
    dynamicConfig.addReusableCommand(saveMavenJobCacheCmd);
    dynamicConfig.addReusableCommand(notifyOnFailureCmd);
    dynamicConfig.addReusableCommand(azureArtifactsTokenCmd);

    const steps: Command[] = [
      new commands.Checkout(),
      new commands.workspace.Attach({ at: '.' }),
      new reusable.ReusedCommand(restoreMavenJobCacheCmd, { jobName }),
      new reusable.ReusedCommand(azureArtifactsTokenCmd),
      new commands.Run({
        name: 'Maven Package and deploy to Artifactory ([gravitee-snapshots] repository)',
        command: `mvn deploy --no-transfer-progress -DskipTests -Dskip.validation=true ${mavenParallelism('large')} -s ${config.maven.settingsFile} -U -P gio-artifactory-snapshot`,
      }),
      // Reuses the target/ the step above produced — maven-jar-plugin leaves a jar alone when its
      // classes have not changed — so the feed gets the bytes Artifactory got, not a rebuild.
      // altDeploymentRepository overrides what the profile sets, leaving profiles and signing alone.
      //
      // Fatal, like the deploy to Artifactory above. A swallowed failure here would leave the
      // feed silently short of a snapshot while the build stayed green, and nothing would
      // surface it until Artifactory is switched off. This step goes when Artifactory does.
      new commands.Run({
        name: 'Maven deploy to the Azure feed (snapshots)',
        // Both flags on purpose: for a SNAPSHOT version maven-deploy-plugin reads
        // altSnapshotDeploymentRepository first, so a profile setting it would win over
        // altDeploymentRepository alone. No profile does today; this keeps the command
        // line authoritative if one ever does.
        command: `mvn deploy --no-transfer-progress -DskipTests -Dskip.validation=true ${mavenParallelism('large')} -s ${config.maven.settingsFile} -U -P gio-artifactory-snapshot -DaltDeploymentRepository=azure-artifacts-gravitee-snapshots::${config.maven.azureSnapshotsFeedUrl} \\
  -DaltSnapshotDeploymentRepository=azure-artifacts-gravitee-snapshots::${config.maven.azureSnapshotsFeedUrl}`,
      }),
      new reusable.ReusedCommand(notifyOnFailureCmd),
      new reusable.ReusedCommand(saveMavenJobCacheCmd, { jobName }),
    ];
    return new Job(jobName, OpenJdkExecutor.create('large'), steps);
  }
}
