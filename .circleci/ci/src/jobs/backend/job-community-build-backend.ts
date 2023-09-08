import { commands, Config, Job, reusable } from '@circleci/circleci-config-sdk';
import { OpenJdkExecutor } from '../../executors';
import { NotifyOnFailureCommand, RestoreMavenJobCacheCommand, SaveMavenJobCacheCommand } from '../../commands';
import { Command } from '@circleci/circleci-config-sdk/dist/src/lib/Components/Commands/exports/Command';

export class CommunityBuildBackendJob {
  public static create(dynamicConfig: Config): Job {
    const jobName = 'job-community-build';

    const restoreMavenJobCacheCmd = RestoreMavenJobCacheCommand.get();
    const saveMavenJobCacheCmd = SaveMavenJobCacheCommand.get();
    const notifyOnFailureCmd = NotifyOnFailureCommand.get(dynamicConfig);
    dynamicConfig.addReusableCommand(restoreMavenJobCacheCmd);
    dynamicConfig.addReusableCommand(saveMavenJobCacheCmd);
    dynamicConfig.addReusableCommand(notifyOnFailureCmd);

    const steps: Command[] = [
      new commands.Checkout(),
      new reusable.ReusedCommand(restoreMavenJobCacheCmd, { jobName: jobName }),
      new commands.Run({
        name: 'Build project',
        command: `mvn clean install --no-transfer-progress --update-snapshots -DskipTests -Dskip.validation=true -T 2C`,
      }),
      new reusable.ReusedCommand(notifyOnFailureCmd),
      new reusable.ReusedCommand(saveMavenJobCacheCmd, { jobName: jobName }),
    ];
    return new Job(jobName, OpenJdkExecutor.create(), steps);
  }
}
