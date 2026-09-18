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

/**
 * The Maven snapshots of a branch of config.snapshotBranches, published on the Azure feed alone,
 * under a version of the branch's own.
 *
 * A copy of the Azure step of PublishJob rather than a third target of it: that job is where
 * master and the support branches publish, and it is being reworked for the move to the feed.
 * When they publish there alone, this job and that one meet again.
 */
/**
 * The lib orb's rule (common_publish-on-artifactory): a version the branch set itself is kept, and
 * master's plain one gets the branch name, so that master's snapshots are never overwritten.
 * 4.13.0-SNAPSHOT on agent_gateway publishes as 4.13.0-agent-gateway-SNAPSHOT.
 *
 * The pom is CI-friendly, so the name goes in as its `sha1` qualifier on the command line - what
 * that property is for - rather than by rewriting the poms. Exported for its test, which runs it
 * against a stub mvn.
 */
export const versionSnapshotsAfterBranch = `currentVersion=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout -s ${config.maven.settingsFile})
if [[ "$currentVersion" =~ ^[0-9]+\\.[0-9]+\\.[0-9]+-SNAPSHOT$ ]]; then
  # 's/[^a-z0-9]+/-/gi' => replace all non alphanumerical character with '-'
  # 's/-$//' => remove potential trailing '-'
  cleanBranchName=$(echo "$CIRCLE_BRANCH" | sed -r 's/[^a-z0-9]+/-/gi' | sed -r 's/-$//')
  echo "Publishing $currentVersion as \${currentVersion%-SNAPSHOT}-\${cleanBranchName}-SNAPSHOT"
  echo "export SNAPSHOT_VERSION_ARGS='-Dsha1=-\${cleanBranchName}'" >> $BASH_ENV
else
  echo "Publishing $currentVersion as it is"
  echo "export SNAPSHOT_VERSION_ARGS=''" >> $BASH_ENV
fi
`;

export class PublishSnapshotOnAzureJob {
  public static create(dynamicConfig: Config, environment: CircleCIEnvironment): Job {
    const jobName = 'job-publish-snapshot-on-azure';

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
        name: 'Version the snapshots after the branch',
        command: versionSnapshotsAfterBranch,
      }),
      new commands.Run({
        name: 'Maven deploy to the Azure feed (snapshots)',
        // Both flags on purpose: for a SNAPSHOT version maven-deploy-plugin reads
        // altSnapshotDeploymentRepository first, so a profile setting it would win over
        // altDeploymentRepository alone. No profile does today; this keeps the command line
        // authoritative if one ever does.
        //
        // Fatal: a swallowed failure here would leave the feed silently short of a snapshot while
        // the build stayed green, and the repositories built on this branch would not see it.
        command: `mvn deploy --no-transfer-progress -DskipTests -Dskip.validation=true -Dgravitee.archrules.skip=true \${SNAPSHOT_VERSION_ARGS} ${mavenParallelism('large')} -s ${config.maven.settingsFile} -U -P gio-artifactory-snapshot -DaltDeploymentRepository=azure-artifacts-gravitee-snapshots::${config.maven.azureSnapshotsFeedUrl} \\
  -DaltSnapshotDeploymentRepository=azure-artifacts-gravitee-snapshots::${config.maven.azureSnapshotsFeedUrl}`,
      }),
      new reusable.ReusedCommand(notifyOnFailureCmd),
      new reusable.ReusedCommand(saveMavenJobCacheCmd, { jobName }),
    ];

    return new Job(jobName, OpenJdkNodeExecutor.create('large'), steps);
  }
}
