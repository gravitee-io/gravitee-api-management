import { commands, Config, Job, reusable } from '@circleci/circleci-config-sdk';
import { Command } from '@circleci/circleci-config-sdk/dist/src/lib/Components/Commands/exports/Command';
import { NodeLtsExecutor } from '../../executors';
import { NotifyOnFailureCommand, WebuiInstallCommand } from '../../commands';

export class StorybookConsoleJob {
  private static jobName = 'job-console-webui-build-storybook';

  public static create(dynamicConfig: Config): Job {
    const webUiInstallCommand = WebuiInstallCommand.get();
    dynamicConfig.addReusableCommand(webUiInstallCommand);

    const notifyOnFailureCommand = NotifyOnFailureCommand.get(dynamicConfig);
    dynamicConfig.addReusableCommand(notifyOnFailureCommand);

    const steps: Command[] = [
      new commands.Checkout(),
      new reusable.ReusedCommand(webUiInstallCommand, { 'apim-ui-project': 'gravitee-apim-console-webui' }),
      new commands.Run({
        name: 'Build',
        command: 'npm run build-storybook',
        working_directory: 'gravitee-apim-console-webui',
        environment: {
          NODE_OPTIONS: '--max_old_space_size=3072',
        },
      }),
      new reusable.ReusedCommand(notifyOnFailureCommand),
      new commands.workspace.Persist({
        root: '.',
        paths: ['gravitee-apim-console-webui/storybook-static'],
      }),
    ];

    return new Job(StorybookConsoleJob.jobName, NodeLtsExecutor.create('large'), steps);
  }
}
