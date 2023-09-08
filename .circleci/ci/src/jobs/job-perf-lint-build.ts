import { commands, Config, Job, reusable } from '@circleci/circleci-config-sdk';
import { Command } from '@circleci/circleci-config-sdk/dist/src/lib/Components/Commands/exports/Command';
import { NodeLtsExecutor } from '../executors';
import { NotifyOnFailureCommand, WebuiInstallCommand } from '../commands';

export class PerfLintBuildJob {
  private static jobName = 'job-perf-lint-build';

  public static create(dynamicConfig: Config): Job {
    const webUiInstallCommand = WebuiInstallCommand.get();
    dynamicConfig.addReusableCommand(webUiInstallCommand);

    const notifyOnFailureCommand = NotifyOnFailureCommand.get(dynamicConfig);
    dynamicConfig.addReusableCommand(notifyOnFailureCommand);

    const steps: Command[] = [
      new commands.Checkout(),
      new reusable.ReusedCommand(webUiInstallCommand, { 'apim-ui-project': 'gravitee-apim-perf' }),
      new commands.workspace.Attach({ at: '.' }),
      new commands.Run({
        name: 'Check License',
        command: 'npm run lint:license',
        working_directory: 'gravitee-apim-perf',
      }),
      new commands.Run({
        name: 'Run Prettier and ESLint',
        command: 'npm run lint',
        working_directory: 'gravitee-apim-perf',
      }),
      new commands.Run({
        name: 'Build',
        command: 'npm run build',
        working_directory: 'gravitee-apim-perf',
      }),
      new reusable.ReusedCommand(notifyOnFailureCommand),
    ];

    return new Job(PerfLintBuildJob.jobName, NodeLtsExecutor.create('small'), steps);
  }
}
