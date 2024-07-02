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
import { NotifyOnFailureCommand, RestoreMavenJobCacheCommand, SaveMavenJobCacheCommand } from '../../commands';
import { OpenJdkExecutor } from '../../executors';
import { CircleCIEnvironment } from '../../pipelines';

export class PublishJob {
  public static create(dynamicConfig: Config, environment: CircleCIEnvironment, target: 'nexus' | 'artifactory'): Job {
    const jobName = `job-publish-on-${target}`;

    const restoreMavenJobCacheCmd = RestoreMavenJobCacheCommand.get(environment);
    const saveMavenJobCacheCmd = SaveMavenJobCacheCommand.get();
    const notifyOnFailureCmd = NotifyOnFailureCommand.get(dynamicConfig);
    dynamicConfig.addReusableCommand(restoreMavenJobCacheCmd);
    dynamicConfig.addReusableCommand(saveMavenJobCacheCmd);
    dynamicConfig.addReusableCommand(notifyOnFailureCmd);

    const steps: Command[] = [
      new commands.Checkout(),
      new commands.workspace.Attach({ at: '.' }),
      new reusable.ReusedCommand(restoreMavenJobCacheCmd, { jobName }),
      target === 'nexus'
        ? new commands.Run({
            name: 'Maven Package and deploy to Nexus Snapshots',
            command: `mvn deploy --no-transfer-progress -DskipTests -Dskip.validation=true -T 2C -s ${config.maven.settingsFile} -U`,
          })
        : new commands.Run({
            name: 'Maven Package and deploy to Artifactory ([gravitee-snapshots] repository)',
            command: `mvn deploy --no-transfer-progress -DskipTests -Dskip.validation=true -T 2C -s ${config.maven.settingsFile} -U -P gio-artifactory-snapshot`,
          }),
      new reusable.ReusedCommand(notifyOnFailureCmd),
      new reusable.ReusedCommand(saveMavenJobCacheCmd, { jobName }),
    ];
    return new Job(jobName, OpenJdkExecutor.create('large'), steps);
  }
}
