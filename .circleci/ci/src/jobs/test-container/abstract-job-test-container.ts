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
import { commands, Config, parameters, reusable } from '@circleci/circleci-config-sdk';
import { NotifyOnFailureCommand, RestoreMavenJobCacheCommand, SaveMavenJobCacheCommand } from '../../commands';
import { UbuntuExecutor } from '../../executors';
import { Executor } from '@circleci/circleci-config-sdk/dist/src/lib/Components/Executors';
import { AnyParameterLiteral } from '@circleci/circleci-config-sdk/dist/src/lib/Components/Parameters/types/CustomParameterLiterals.types';
import { CircleCIEnvironment } from '../../pipelines';

export abstract class AbstractTestContainerJob {
  protected static create(
    dynamicConfig: Config,
    environment: CircleCIEnvironment,
    jobName: string,
    parameters: parameters.CustomParametersList<AnyParameterLiteral>,
    testStep: commands.Run,
    executor: Executor = UbuntuExecutor.create(),
  ) {
    const restoreMavenJobCacheCmd = RestoreMavenJobCacheCommand.get(environment);
    const saveMavenJobCacheCmd = SaveMavenJobCacheCommand.get();
    const notifyOnFailureCmd = NotifyOnFailureCommand.get(dynamicConfig);
    dynamicConfig.addReusableCommand(restoreMavenJobCacheCmd);
    dynamicConfig.addReusableCommand(saveMavenJobCacheCmd);
    dynamicConfig.addReusableCommand(notifyOnFailureCmd);

    return new reusable.ParameterizedJob(jobName, executor, parameters, [
      new commands.Checkout(),
      new commands.workspace.Attach({ at: '.' }),
      new reusable.ReusedCommand(restoreMavenJobCacheCmd, { jobName }),
      testStep,
      new commands.Run({
        name: 'Save test results',
        command: `mkdir -p ~/test-results/junit/
find . -type f -regex ".*/target/surefire-reports/.*xml" -exec cp {} ~/test-results/junit/ \\;`,
        when: 'always',
      }),
      new reusable.ReusedCommand(saveMavenJobCacheCmd, { jobName }),
      new commands.StoreTestResults({
        path: '~/test-results',
      }),
      new reusable.ReusedCommand(notifyOnFailureCmd),
    ]);
  }
}
