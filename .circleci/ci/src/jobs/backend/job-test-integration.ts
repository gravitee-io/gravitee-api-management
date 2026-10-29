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
import { UbuntuExecutor } from '../../executors';
import {
  AzureArtifactsTokenCommand,
  NotifyOnFailureCommand,
  RestoreMavenJobCacheCommand,
  SaveMavenJobCacheCommand,
  withJdk,
} from '../../commands';
import { CircleCIEnvironment } from '../../pipelines';
import { computeApimVersion } from '../../utils';
import { assemblesPinnedCore } from '../../workflows/groups/changed-files';

export class TestIntegrationJob {
  private static jobName = 'job-test-integration';

  public static create(dynamicConfig: Config, environment: CircleCIEnvironment) {
    const restoreMavenJobCacheCmd = RestoreMavenJobCacheCommand.get(environment);
    const saveMavenJobCacheCmd = SaveMavenJobCacheCommand.get();
    const notifyOnFailureCmd = NotifyOnFailureCommand.get(dynamicConfig, environment);
    const azureArtifactsTokenCmd = AzureArtifactsTokenCommand.get(dynamicConfig);
    dynamicConfig.addReusableCommand(restoreMavenJobCacheCmd);
    dynamicConfig.addReusableCommand(azureArtifactsTokenCmd);
    dynamicConfig.addReusableCommand(saveMavenJobCacheCmd);
    dynamicConfig.addReusableCommand(notifyOnFailureCmd);
    const executor = UbuntuExecutor.create();

    // Same rule as job-build-backend: a distribution-only change is tested against the core it pins.
    const pinnedCore = assemblesPinnedCore(environment.changedFiles);
    const coreVersion = pinnedCore ? '' : ` -Dapim.core.version=${computeApimVersion(environment)}`;

    const steps: Command[] = [
      new commands.Checkout(),
      ...withJdk(dynamicConfig, executor),
      new commands.workspace.Attach({ at: '.' }),
      new reusable.ReusedCommand(restoreMavenJobCacheCmd, { jobName: TestIntegrationJob.jobName }),
      new reusable.ReusedCommand(azureArtifactsTokenCmd),
      new commands.cache.Restore({
        keys: [`${config.cache.prefix}-build-apim-{{ .Environment.CIRCLE_WORKFLOW_WORKSPACE_ID }}`],
      }),
      ...(pinnedCore ? [TestIntegrationJob.alignTestsWithPinnedCore()] : []),
      new commands.Run({
        name: 'Run tests',
        command: `cd gravitee-apim-distribution/gravitee-apim-distribution-integration-tests
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

# Run tests with rerunFailingTestsCount=3 because some integration tests related to RabbitMQ or Websocket are randomly failing on the CI
mvn --fail-fast -s ../../.gravitee.settings.xml test --no-transfer-progress -nsu${coreVersion} -Dskip.validation=true -Dgravitee.archrules.skip=true -Dsurefire.excludesFile=/tmp/ignore_list -Dsurefire.rerunFailingTestsCount=3 -Dsurefire.exitTimeout=300`,
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

    return new Job(TestIntegrationJob.jobName, executor, steps, {
      parallelism: 3,
    });
  }

  /**
   * The integration tests live in the distribution reactor but follow the branch, while a pinned build
   * runs them against the released core. A test merged with a core fix after that release asserts a
   * fix the pinned core does not have, and fails on every distribution-only pull request until the
   * next core release moves the pin.
   *
   * Put those tests back as they were at the pinned core's tag. Only commits that also changed the
   * core count: a test that came with a distribution change (a plugin bump) is what this pull request
   * ships, so it keeps running. A pin with no tag (a SNAPSHOT on master) leaves the tests untouched.
   */
  private static alignTestsWithPinnedCore(): commands.Run {
    return new commands.Run({
      name: 'Align integration tests with the pinned core',
      command: `PIN=$(sed -n 's:.*<apim.core.version>\\(.*\\)</apim.core.version>.*:\\1:p' gravitee-apim-distribution/pom.xml)
TAG="core_\${PIN}"
if ! git fetch --no-tags origin "refs/tags/\${TAG}:refs/tags/\${TAG}"; then
  echo "No tag \${TAG}: the pinned core is not a release, integration tests run as they are on the branch."
  exit 0
fi

TESTS=gravitee-apim-distribution/gravitee-apim-distribution-integration-tests
for commit in $(git rev-list --no-merges "\${TAG}..HEAD" -- "\${TESTS}"); do
  if git show --name-only --format= "\${commit}" | grep -qv '^gravitee-apim-distribution/'; then
    git show --name-only --format= "\${commit}" -- "\${TESTS}"
  fi
done | sort -u > /tmp/core-coupled-tests

if [ ! -s /tmp/core-coupled-tests ]; then
  echo "No integration test changed with the core since \${TAG}."
  exit 0
fi

echo "Restored as of \${TAG}, because they came with core changes the pinned core does not have:"
while read -r file; do
  echo "  \${file}"
  if git cat-file -e "\${TAG}:\${file}" 2>/dev/null; then
    git checkout "\${TAG}" -- "\${file}"
  else
    rm -f "\${file}"
  fi
done < /tmp/core-coupled-tests`,
    });
  }
}
