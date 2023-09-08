import { commands, Config, Job, reusable } from '@circleci/circleci-config-sdk';
import { Command } from '@circleci/circleci-config-sdk/dist/src/lib/Components/Commands/exports/Command';
import { orbs } from '../orbs';
import { config } from '../config';
import { NodeLtsExecutor } from '../executors';

export class DangerJsJob {
  private static jobName = 'job-danger-js';
  public static create(dynamicConfig: Config): Job {
    dynamicConfig.importOrb(orbs.keeper);

    const steps: Command[] = [
      new commands.Checkout(),
      new reusable.ReusedCommand(orbs.keeper.commands['env-export'], {
        'secret-url': config.secrets.githubApiToken,
        'var-name': 'DANGER_GITHUB_API_TOKEN',
      }),
      new commands.Run({
        name: 'Run Danger JS',
        command: 'cd .circleci/danger && yarn run danger',
      }),
    ];
    return new Job(DangerJsJob.jobName, NodeLtsExecutor.create('small'), steps);
  }
}
