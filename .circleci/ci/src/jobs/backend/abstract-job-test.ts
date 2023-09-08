import { commands, Config, Job, reusable } from '@circleci/circleci-config-sdk';
import { NotifyOnFailureCommand, RestoreMavenJobCacheCommand, SaveMavenJobCacheCommand } from '../../commands';
import { Executor } from '@circleci/circleci-config-sdk/dist/src/lib/Components/Executors';
import { config } from '../../config';

export abstract class AbstractTestJob {
  protected static create(dynamicConfig: Config, jobName: string, testStep: commands.Run, executor: Executor, pathsToPersist: string[]) {
    const restoreMavenJobCacheCmd = RestoreMavenJobCacheCommand.get();
    const saveMavenJobCacheCmd = SaveMavenJobCacheCommand.get();
    const notifyOnFailureCmd = NotifyOnFailureCommand.get(dynamicConfig);
    dynamicConfig.addReusableCommand(restoreMavenJobCacheCmd);
    dynamicConfig.addReusableCommand(saveMavenJobCacheCmd);
    dynamicConfig.addReusableCommand(notifyOnFailureCmd);

    return new Job(jobName, executor, [
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
    ]);
  }
}
