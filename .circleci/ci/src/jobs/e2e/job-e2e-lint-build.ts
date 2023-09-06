import { commands, Config, Job, reusable } from '@circleci/circleci-config-sdk';
import { NotifyOnFailureCommand, WebuiInstallCommand } from '../../commands';
import { Command } from '@circleci/circleci-config-sdk/dist/src/lib/Components/Commands/exports/Command';
import { NodeLtsExecutor } from '../../executors';

export class E2ELintBuildJob {
  private static jobName = 'job-e2e-lint-build';
  public static create(dynamicConfig: Config): Job {
    const webuiInstallCmd = WebuiInstallCommand.get();
    const notifyOnFailureCmd = NotifyOnFailureCommand.get(dynamicConfig);
    dynamicConfig.addReusableCommand(webuiInstallCmd);
    dynamicConfig.addReusableCommand(notifyOnFailureCmd);

    const steps: Command[] = [
      new commands.Checkout(),
      new reusable.ReusedCommand(webuiInstallCmd, { 'apim-ui-project': 'gravitee-apim-e2e' }),
      new commands.workspace.Attach({ at: '.' }),
      new commands.Run({
        name: 'Check License',
        command: `npm run lint:license`,
        working_directory: 'gravitee-apim-e2e',
      }),
      new commands.Run({
        name: 'Run Prettier and ESLint',
        command: `npm run lint`,
        working_directory: 'gravitee-apim-e2e',
      }),
      new commands.Run({
        name: 'Build',
        command: `npm run build`,
        working_directory: 'gravitee-apim-e2e',
      }),
      new reusable.ReusedCommand(notifyOnFailureCmd),
      new commands.workspace.Persist({
        root: '.',
        paths: ['gravitee-apim-e2e/dist', 'gravitee-apim-e2e/node_modules'],
      }),
    ];
    return new Job(E2ELintBuildJob.jobName, NodeLtsExecutor.create('small'), steps);
  }
}
