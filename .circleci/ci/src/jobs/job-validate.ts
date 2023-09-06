import { commands, Config, Job, reusable } from '@circleci/circleci-config-sdk';
import { OpenJdkExecutor } from '../executors';
import { NotifyOnFailureCommand, RestoreMavenJobCacheCommand, SaveMavenJobCacheCommand } from '../commands';
import { Command } from '@circleci/circleci-config-sdk/dist/src/lib/Components/Commands/exports/Command';
import { config } from '../config';

export class ValidateJob {
  private static jobName = 'job-validate';
  public static create(dynamicConfig: Config): Job {
    const restoreMavenJobCacheCmd = RestoreMavenJobCacheCommand.get();
    const saveMavenJobCacheCmd = SaveMavenJobCacheCommand.get();
    const notifyOnFailureCmd = NotifyOnFailureCommand.get(dynamicConfig);
    dynamicConfig.addReusableCommand(restoreMavenJobCacheCmd);
    dynamicConfig.addReusableCommand(saveMavenJobCacheCmd);
    dynamicConfig.addReusableCommand(notifyOnFailureCmd);

    const steps: Command[] = [
      new commands.Checkout(),
      new commands.workspace.Attach({ at: '.' }),
      new reusable.ReusedCommand(restoreMavenJobCacheCmd, { jobName: ValidateJob.jobName }),
      new commands.Run({
        name: 'Validate project',
        command: `mvn -s ${config.maven.settingsFile} validate --no-transfer-progress -Pall-modules,integration-tests-modules -T 2C`,
      }),
      new reusable.ReusedCommand(notifyOnFailureCmd),
      new reusable.ReusedCommand(saveMavenJobCacheCmd, { jobName: ValidateJob.jobName }),
    ];
    return new Job(ValidateJob.jobName, OpenJdkExecutor.create('small'), steps);
  }
}
