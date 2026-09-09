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
import { OpenJdkNodeExecutor } from '../../executors';
import { InstallYarnCommand, NotifyOnFailureCommand, RestoreMavenJobCacheCommand, SaveMavenJobCacheCommand } from '../../commands';
import { Command } from '@circleci/circleci-config-sdk/dist/src/lib/Components/Commands/exports/Command';
import { CircleCIEnvironment } from '../../pipelines';
import { mavenParallelism } from '../../utils';

export class CommunityBuildBackendJob {
  public static create(dynamicConfig: Config, environment: CircleCIEnvironment): Job {
    const jobName = 'job-community-build';

    const restoreMavenJobCacheCmd = RestoreMavenJobCacheCommand.get(environment);
    const saveMavenJobCacheCmd = SaveMavenJobCacheCommand.get();
    const notifyOnFailureCmd = NotifyOnFailureCommand.get(dynamicConfig, environment);
    // The reactor installs the yarn workspace (gravitee-gamma runs `yarn install` in
    // generate-resources). Without corepack the image's yarn 1 cannot read the berry lockfile
    // and resolves the whole workspace from the registry instead.
    const installYarnCmd = InstallYarnCommand.get();
    dynamicConfig.addReusableCommand(restoreMavenJobCacheCmd);
    dynamicConfig.addReusableCommand(saveMavenJobCacheCmd);
    dynamicConfig.addReusableCommand(notifyOnFailureCmd);
    dynamicConfig.addReusableCommand(installYarnCmd);

    const steps: Command[] = [
      new commands.Checkout(),
      new reusable.ReusedCommand(restoreMavenJobCacheCmd, { jobName: jobName }),
      new reusable.ReusedCommand(installYarnCmd),
      new commands.Run({
        name: 'Build project',
        command: `mvn clean install --no-transfer-progress --update-snapshots -DskipTests -Dskip.validation=true -Dgravitee.archrules.skip=false ${mavenParallelism('large')}`,
        environment: {
          // Cap the maven JVM heap: its default is derived from the memory of the
          // underlying CI host, not from the resource class of the job.
          MAVEN_OPTS: '-Xmx2048m',
        },
      }),
      new reusable.ReusedCommand(notifyOnFailureCmd),
      new reusable.ReusedCommand(saveMavenJobCacheCmd, { jobName: jobName }),
    ];
    return new Job(jobName, OpenJdkNodeExecutor.create('large'), steps);
  }
}
