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
import { OpenJdkNodeExecutor } from '../../executors';
import { CircleCIEnvironment } from '../../pipelines';
import { AzureArtifactsTokenCommand, PrepareGpgCmd, RestoreMavenJobCacheCommand, SaveMavenJobCacheCommand } from '../../commands';
import { keeper } from '../../orbs/keeper';

export class NexusStagingJob {
  private static jobName: string = 'job-nexus-staging';
  /**
   * @param checkoutRef the tag holding the tree to publish. Defaults to the bare version, which is
   * what the product's own lanes tag; the core lane prefixes its tags, so it passes its own.
   */
  public static create(dynamicConfig: Config, environment: CircleCIEnvironment, checkoutRef: string = environment.graviteeioVersion): Job {
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
        name: `Checkout tag ${checkoutRef}`,
        command: `git checkout ${checkoutRef}`,
      }),
      new reusable.ReusedCommand(restoreMavenJobCacheCmd, { jobName: NexusStagingJob.jobName }),
      new commands.workspace.Attach({ at: '.' }),
      new reusable.ReusedCommand(prepareGpgCmd),
      new reusable.ReusedCommand(azureArtifactsTokenCmd),
      new commands.Run({
        name: 'Release on Nexus',
        command: `mvn clean deploy --activate-profiles gravitee-release --batch-mode -T 4 -DskipTests -Dskip.validation=true -Dgravitee.archrules.skip=true --settings ${config.maven.settingsFile} --update-snapshots`,
      }),
      new reusable.ReusedCommand(saveMavenCacheCmd, { jobName: NexusStagingJob.jobName }),
    ];

    return new Job(NexusStagingJob.jobName, OpenJdkNodeExecutor.create('xlarge'), steps);
  }
}
