import { commands, Config, parameters, reusable } from '@circleci/circleci-config-sdk';
import { NotifyOnFailureCommand, RestoreMavenJobCacheCommand, SaveMavenJobCacheCommand } from '../../commands';
import { UbuntuExecutor } from '../../executors';
import { Executor } from '@circleci/circleci-config-sdk/dist/src/lib/Components/Executors';
import { AnyParameterLiteral } from '@circleci/circleci-config-sdk/dist/src/lib/Components/Parameters/types/CustomParameterLiterals.types';

export abstract class AbstractTestContainerJob {
  protected static create(
    dynamicConfig: Config,
    jobName: string,
    parameters: parameters.CustomParametersList<AnyParameterLiteral>,
    testStep: commands.Run,
    executor: Executor = UbuntuExecutor.create(),
  ) {
    const restoreMavenJobCacheCmd = RestoreMavenJobCacheCommand.get();
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
