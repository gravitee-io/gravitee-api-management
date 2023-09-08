import { commands, Config, Job, reusable } from '@circleci/circleci-config-sdk';
import { config } from '../../config';
import { OpenJdkExecutor } from '../../executors';
import { Command } from '@circleci/circleci-config-sdk/dist/src/lib/Components/Commands/exports/Command';
import { NotifyOnFailureCommand, RestoreMavenJobCacheCommand, SaveMavenJobCacheCommand } from '../../commands';

export class TestIntegrationJob {
  private static jobName = 'job-test-integration';

  public static create(dynamicConfig: Config) {
    const restoreMavenJobCacheCmd = RestoreMavenJobCacheCommand.get();
    const saveMavenJobCacheCmd = SaveMavenJobCacheCommand.get();
    const notifyOnFailureCmd = NotifyOnFailureCommand.get(dynamicConfig);
    dynamicConfig.addReusableCommand(restoreMavenJobCacheCmd);
    dynamicConfig.addReusableCommand(saveMavenJobCacheCmd);
    dynamicConfig.addReusableCommand(notifyOnFailureCmd);

    const steps: Command[] = [
      new commands.SetupRemoteDocker(),
      new commands.Checkout(),
      new commands.workspace.Attach({ at: '.' }),
      new reusable.ReusedCommand(restoreMavenJobCacheCmd, { jobName: TestIntegrationJob.jobName }),
      new commands.cache.Restore({
        keys: [`${config.cache.prefix}-build-apim-{{ .Environment.CIRCLE_WORKFLOW_WORKSPACE_ID }}`],
      }),
      new commands.Run({
        name: 'Testcontainers tunnel',
        command: '.circleci/autoforward.py',
        background: true,
        environment: {
          TESTCONTAINERS_HOST_OVERRIDE: 'localhost',
        },
      }),
      new commands.Run({
        name: 'Run tests',
        command: `cd gravitee-apim-integration-tests
# List all tests
circleci tests glob "src/test/java/**/*Test.java" | sed -e 's#^src/test/java/\\(.*\\)\\.java#\\1#' | tr "/" "." > all-tests

# List all tests to run on this executor
cat all-tests | circleci tests split --split-by=timings --timings-type=classname --time-default=10s > tests-to-run

# Compute exclusion list (use grep to invert the include list to an exclude list)
cat all-tests | grep -xvf tests-to-run > ignore_list

# Add * add the end of each line of ignore_list to also exclude all inner classes
sed -i 's/$/*/' ignore_list 

# Display tests to run on this executor
echo "Following test files will run on this executor:"
cat tests-to-run

# Run tests            
mvn --fail-fast -s ../.gravitee.settings.xml test --no-transfer-progress -Dskip.validation=true -Dsurefire.excludesFile=ignore_list`,
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

    return new Job(TestIntegrationJob.jobName, OpenJdkExecutor.create('medium+'), steps, {
      parallelism: 2,
    });
  }
}
