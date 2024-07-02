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
import { PrepareGpgCmd, RestoreMavenJobCacheCommand, SaveMavenJobCacheCommand } from '../../commands';
import { keeper } from '../../orbs/keeper';

export class NexusStagingJob {
  private static jobName: string = 'job-nexus-staging';
  public static create(dynamicConfig: Config, environment: CircleCIEnvironment): Job {
    dynamicConfig.importOrb(keeper);

    const restoreMavenJobCacheCmd = RestoreMavenJobCacheCommand.get(environment);
    dynamicConfig.addReusableCommand(restoreMavenJobCacheCmd);

    const prepareGpgCmd = PrepareGpgCmd.get(dynamicConfig);
    dynamicConfig.addReusableCommand(prepareGpgCmd);

    const saveMavenCacheCmd = SaveMavenJobCacheCommand.get();
    dynamicConfig.addReusableCommand(saveMavenCacheCmd);

    const steps: Command[] = [
      new commands.Checkout(),
      new commands.Run({
        name: `Checkout tag ${environment.graviteeioVersion}`,
        command: `git checkout ${environment.graviteeioVersion}`,
      }),
      new reusable.ReusedCommand(restoreMavenJobCacheCmd, { jobName: NexusStagingJob.jobName }),
      new commands.workspace.Attach({ at: '.' }),
      new reusable.ReusedCommand(prepareGpgCmd),
      new commands.Run({
        name: 'Release on Nexus',
        command: `mvn clean deploy --activate-profiles gravitee-release --batch-mode -Dmaven.test.skip=true -DskipTests -Dskip.validation=true --settings ${config.maven.settingsFile} --update-snapshots`,
      }),
      new reusable.ReusedCommand(saveMavenCacheCmd, { jobName: NexusStagingJob.jobName }),
    ];

    return new Job(NexusStagingJob.jobName, OpenJdkExecutor.create('xlarge'), steps);
  }
}
