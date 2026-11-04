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

export class PublishReleaseJob {
  private static jobName: string = 'job-publish-release';
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
      new reusable.ReusedCommand(restoreMavenJobCacheCmd, { jobName: PublishReleaseJob.jobName }),
      new commands.workspace.Attach({ at: '.' }),
      new reusable.ReusedCommand(prepareGpgCmd),
      new reusable.ReusedCommand(azureArtifactsTokenCmd),
      new commands.Run({
        name: 'Maven deploy to the Azure feed (releases)',
        // `gio-release`, not `gravitee-release`: the latter takes the deploy phase away from
        // maven-deploy-plugin, and altDeploymentRepository would then be a parameter of a
        // plugin that never runs. Same enforcer, GPG signing, sources and javadoc.
        command: `mvn clean deploy --activate-profiles gio-release --batch-mode -T 4 -DskipTests -Dskip.validation=true --settings ${config.maven.settingsFile} --update-snapshots -DaltDeploymentRepository=azure-artifacts-gravitee::${config.maven.azureFeedUrl}`,
      }),
      new reusable.ReusedCommand(saveMavenCacheCmd, { jobName: PublishReleaseJob.jobName }),
      // Customers read the feed through the `public` view only: it holds what used to go to Maven
      // Central. A fresh token, because the deploy above may outlive the first one (one hour).
      new reusable.ReusedCommand(azureArtifactsTokenCmd),
      new reusable.ReusedCommand(keeper.commands['env-export'], {
        'secret-url': config.secrets.githubApiToken,
        'var-name': 'GITHUB_TOKEN',
      }),
      new commands.Run({
        name: 'Promote the release to the @public view of the Azure feed',
        environment: {
          MAVEN_SETTINGS: config.maven.settingsFile,
          MAVEN_PROFILES: 'gio-release',
          PROMOTE_DRY_RUN: `${environment.isDryRun}`,
        },
        command: 'bash .circleci/scripts/promote-to-view.sh',
      }),
      new commands.StoreArtifacts({ path: '/tmp/promote-to-view', destination: 'promote-to-view' }),
    ];

    return new Job(PublishReleaseJob.jobName, OpenJdkExecutor.create('xlarge'), steps);
  }
}
