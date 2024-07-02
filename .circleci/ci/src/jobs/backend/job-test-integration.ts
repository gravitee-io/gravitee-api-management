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
import { UbuntuExecutor } from '../../executors';
import { Command } from '@circleci/circleci-config-sdk/dist/src/lib/Components/Commands/exports/Command';
import { NotifyOnFailureCommand, RestoreMavenJobCacheCommand, SaveMavenJobCacheCommand } from '../../commands';
import { CircleCIEnvironment } from '../../pipelines';

export class TestIntegrationJob {
  private static jobName = 'job-test-integration';

  public static create(dynamicConfig: Config, environment: CircleCIEnvironment) {
    const restoreMavenJobCacheCmd = RestoreMavenJobCacheCommand.get(environment);
    const saveMavenJobCacheCmd = SaveMavenJobCacheCommand.get();
    const notifyOnFailureCmd = NotifyOnFailureCommand.get(dynamicConfig);
    dynamicConfig.addReusableCommand(restoreMavenJobCacheCmd);
    dynamicConfig.addReusableCommand(saveMavenJobCacheCmd);
    dynamicConfig.addReusableCommand(notifyOnFailureCmd);

    const steps: Command[] = [
      new commands.Checkout(),
      new commands.workspace.Attach({ at: '.' }),
      new reusable.ReusedCommand(restoreMavenJobCacheCmd, { jobName: TestIntegrationJob.jobName }),
      new commands.cache.Restore({
        keys: [`${config.cache.prefix}-build-apim-{{ .Environment.CIRCLE_WORKFLOW_WORKSPACE_ID }}`],
      }),
      new commands.Run({
        name: 'Run tests',
        command: `cd gravitee-apim-integration-tests
# List all tests
circleci tests glob "src/test/java/**/*Test.java" | sed -e 's#^src/test/java/\\(.*\\)\\.java#\\1#' | tr "/" "." > all-tests

# List all tests to run on this executor
cat all-tests | circleci tests split --split-by=timings --timings-type=classname --time-default=10s > tests-to-run

# Compute exclusion list (use grep to invert the include list to an exclude list)
cat all-tests | grep -xvf tests-to-run > /tmp/ignore_list

# Add * add the end of each line of ignore_list to also exclude all inner classes
sed -i 's/$/*/' /tmp/ignore_list

# Display tests to run on this executor
echo "Following test files will run on this executor:"
cat tests-to-run

# Run tests with rerunFailingTestsCount=2 because some integration tests related to RabbitMQ or Websocket are randomly failing on the CI
mvn --fail-fast -s ../.gravitee.settings.xml test --no-transfer-progress -Dskip.validation=true -Dsurefire.excludesFile=/tmp/ignore_list -Dsurefire.rerunFailingTestsCount=2`,
      }),
      new commands.Run({
        name: 'Save test results',
        command: `mkdir -p ~/test-results/junit/
find . -type f -regex ".*/target/surefire-reports/.*xml" -exec cp {} ~/test-results/junit/ \\;`,
        when: 'always',
      }),
      new reusable.ReusedCommand(notifyOnFailureCmd),
      new reusable.ReusedCommand(saveMavenJobCacheCmd, { jobName: TestIntegrationJob.jobName }),
      new commands.StoreTestResults({
        path: '~/test-results',
      }),
    ];

    return new Job(TestIntegrationJob.jobName, UbuntuExecutor.create(), steps, {
      parallelism: 4,
    });
  }
}
