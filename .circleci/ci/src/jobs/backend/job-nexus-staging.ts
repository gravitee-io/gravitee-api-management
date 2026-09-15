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
import { config } from '../../config';
import { OpenJdkExecutor } from '../../executors';
import { Command } from '@circleci/circleci-config-sdk/dist/src/lib/Components/Commands/exports/Command';
import { CircleCIEnvironment } from '../../pipelines';
import { AzureArtifactsTokenCommand, PrepareGpgCmd, RestoreMavenJobCacheCommand, SaveMavenJobCacheCommand } from '../../commands';
import { keeper } from '../../orbs/keeper';

export class NexusStagingJob {
  private static jobName: string = 'job-nexus-staging';
  public static create(dynamicConfig: Config, environment: CircleCIEnvironment): Job {
    dynamicConfig.importOrb(keeper);

    const restoreMavenJobCacheCmd = RestoreMavenJobCacheCommand.get(environment);
    const azureArtifactsTokenCmd = AzureArtifactsTokenCommand.get(dynamicConfig);
    dynamicConfig.addReusableCommand(restoreMavenJobCacheCmd);

    const prepareGpgCmd = PrepareGpgCmd.get(dynamicConfig);
    dynamicConfig.addReusableCommand(prepareGpgCmd);

    const saveMavenCacheCmd = SaveMavenJobCacheCommand.get();
    dynamicConfig.addReusableCommand(saveMavenCacheCmd);
    dynamicConfig.addReusableCommand(azureArtifactsTokenCmd);

    const steps: Command[] = [
      new commands.Checkout(),
      new commands.Run({
        name: `Checkout tag ${environment.graviteeioVersion}`,
        command: `git checkout ${environment.graviteeioVersion}`,
      }),
      new reusable.ReusedCommand(restoreMavenJobCacheCmd, { jobName: NexusStagingJob.jobName }),
      new commands.workspace.Attach({ at: '.' }),
      new reusable.ReusedCommand(prepareGpgCmd),
      new reusable.ReusedCommand(azureArtifactsTokenCmd),
      new commands.Run({
        name: 'Release on Nexus',
        command: `mvn clean deploy --activate-profiles gravitee-release --batch-mode -T 4 -DskipTests -Dskip.validation=true --settings ${config.maven.settingsFile} --update-snapshots`,
      }),
      new commands.Run({
        name: 'Maven deploy to the Azure feed (releases)',
        // No `clean`, unlike the step above: it would wipe the target/ this one is meant to
        // reuse, and the feed would get a rebuild rather than the bytes the staging repository
        // received. maven-jar-plugin leaves a jar alone when its classes have not changed.
        //
        // `gio-release`, not `gravitee-release`. The latter declares
        // central-publishing-maven-plugin with extensions=true, and that extension takes the
        // deploy phase away from maven-deploy-plugin — the parent POM says so itself. Under it
        // altDeploymentRepository is a parameter of a plugin that never runs, and the step
        // would instead offer Central a second bundle for coordinates it already holds.
        // `gio-release` carries the same enforcer, GPG signing, sources and javadoc, and
        // nothing else.
        //
        // Fatal, like the deploy to the staging repository above. A swallowed failure here
        // would leave the feed silently short of a release while the build stayed green,
        // and nothing would surface it until Artifactory is switched off. This step goes
        // with Artifactory.
        command: `mvn deploy --activate-profiles gio-release --batch-mode -T 4 -DskipTests -Dskip.validation=true --settings ${config.maven.settingsFile} --update-snapshots -DaltDeploymentRepository=azure-artifacts-gravitee::${config.maven.azureFeedUrl}`,
      }),
      new reusable.ReusedCommand(saveMavenCacheCmd, { jobName: NexusStagingJob.jobName }),
    ];

    return new Job(NexusStagingJob.jobName, OpenJdkExecutor.create('xlarge'), steps);
  }
}
