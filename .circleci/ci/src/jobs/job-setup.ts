import { commands, Config, executors, Job, reusable } from '@circleci/circleci-config-sdk';
import { Command } from '@circleci/circleci-config-sdk/dist/src/lib/Components/Commands/exports/Command';
import { orbs } from '../orbs';
import { config } from '../config';

export class SetupJob {
  public static create(dynamicConfig: Config): Job {
    dynamicConfig.importOrb(orbs.keeper);

    const steps: Command[] = [
      new commands.Checkout(),
      new reusable.ReusedCommand(orbs.keeper.commands['env-export'], {
        'secret-url': config.secrets.mavenSettings,
        'var-name': 'MAVEN_SETTINGS',
      }),
      new commands.Run({
        command: `echo $MAVEN_SETTINGS > ${config.maven.settingsFile} `,
      }),
      new commands.workspace.Persist({
        root: '.',
        paths: [config.maven.settingsFile],
      }),
    ];
    return new Job('job-setup', new executors.DockerExecutor(config.executor.base, 'small'), steps);
  }
}
