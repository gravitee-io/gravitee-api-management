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
import { NotifyOnFailureCommand, RestoreMavenJobCacheCommand, SaveMavenJobCacheCommand } from '../../commands';
import { Executor } from '@circleci/circleci-config-sdk/dist/src/lib/Components/Executors';
import { config } from '../../config';
import { JobOptionalProperties } from '@circleci/circleci-config-sdk/dist/src/lib/Components/Job/types/Job.types';
import { CircleCIEnvironment } from '../../pipelines';

export abstract class AbstractTestJob {
  protected static create(
    dynamicConfig: Config,
    environment: CircleCIEnvironment,
    jobName: string,
    testStep: commands.Run,
    executor: Executor,
    pathsToPersist: string[],
    properties?: JobOptionalProperties,
  ) {
    const restoreMavenJobCacheCmd = RestoreMavenJobCacheCommand.get(environment);
    const saveMavenJobCacheCmd = SaveMavenJobCacheCommand.get();
    const notifyOnFailureCmd = NotifyOnFailureCommand.get(dynamicConfig);
    dynamicConfig.addReusableCommand(restoreMavenJobCacheCmd);
    dynamicConfig.addReusableCommand(saveMavenJobCacheCmd);
    dynamicConfig.addReusableCommand(notifyOnFailureCmd);

    return new Job(
      jobName,
      executor,
      [
        new commands.Checkout(),
        new commands.workspace.Attach({ at: '.' }),
        new reusable.ReusedCommand(restoreMavenJobCacheCmd, { jobName }),
        new commands.cache.Restore({
          keys: [`${config.cache.prefix}-build-apim-{{ .Environment.CIRCLE_WORKFLOW_WORKSPACE_ID }}`],
        }),
        testStep,
        new commands.Run({
          name: 'Save test results',
          command: `mkdir -p ~/test-results/junit/
find . -type f -regex ".*/target/surefire-reports/.*xml" -exec cp {} ~/test-results/junit/ \\;`,
          when: 'always',
        }),
        new reusable.ReusedCommand(notifyOnFailureCmd),
        new reusable.ReusedCommand(saveMavenJobCacheCmd, { jobName }),
        new commands.StoreTestResults({
          path: '~/test-results',
        }),
        new commands.workspace.Persist({
          root: '.',
          paths: pathsToPersist,
        }),
      ],
      properties,
    );
  }
}
