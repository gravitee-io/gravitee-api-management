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
import { config } from '../../config';
import { AzureArtifactsTokenCommand, NotifyOnFailureCommand, RestoreMavenJobCacheCommand, SaveMavenJobCacheCommand } from '../../commands';
import { OpenJdkNodeExecutor } from '../../executors';
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
        command: `mvn deploy --no-transfer-progress -DskipTests -Dskip.validation=true -Dgravitee.archrules.skip=true ${mavenParallelism('large')} -s ${config.maven.settingsFile} -U -P gio-artifactory-snapshot`,
      }),
      // This deploys a second time rather than reusing what the step above produced: the
      // OpenAPI generator rewrites its sources on every run — 868 files on the management-v2
      // model alone — so the compiler finds them newer than the classes and rebuilds. The two
      // repositories end up with equivalent jars, not identical bytes; comparing checksums
      // between them proves nothing.
      //
      // altDeploymentRepository overrides what the profile sets, leaving profiles and signing
      // alone. Fatal on failure, like the deploy above.

      new commands.Run({
        name: 'Maven deploy to the Azure feed (snapshots)',
        // Both flags on purpose: for a SNAPSHOT version maven-deploy-plugin reads
        // altSnapshotDeploymentRepository first, so a profile setting it would win over
        // altDeploymentRepository alone. No profile does today; this keeps the command
        // line authoritative if one ever does.
        command: `mvn deploy --no-transfer-progress -DskipTests -Dskip.validation=true -Dgravitee.archrules.skip=true ${mavenParallelism('large')} -s ${config.maven.settingsFile} -U -P gio-artifactory-snapshot -DaltDeploymentRepository=azure-artifacts-gravitee-snapshots::${config.maven.azureSnapshotsFeedUrl} \\
  -DaltSnapshotDeploymentRepository=azure-artifacts-gravitee-snapshots::${config.maven.azureSnapshotsFeedUrl}`,
      }),
      new reusable.ReusedCommand(notifyOnFailureCmd),
      new reusable.ReusedCommand(saveMavenJobCacheCmd, { jobName }),
    ];
    return new Job(jobName, OpenJdkNodeExecutor.create('large'), steps);
  }
}
