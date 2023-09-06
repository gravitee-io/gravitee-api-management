import { commands, Config, Job, reusable } from '@circleci/circleci-config-sdk';
import { OpenJdkNodeExecutor } from '../../executors';
import { NotifyOnFailureCommand, WebuiInstallCommand } from '../../commands';
import { Command } from '@circleci/circleci-config-sdk/dist/src/lib/Components/Commands/exports/Command';

export class E2EGenerateSDKJob {
  private static jobName = 'job-e2e-generate-sdk';
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
        name: 'Generate e2e tests SDK',
        command: `npm run update:sdk:management
npm run update:sdk:management:v2
npm run update:sdk:portal`,
        working_directory: 'gravitee-apim-e2e',
      }),
      new reusable.ReusedCommand(notifyOnFailureCmd),
      new commands.workspace.Persist({
        root: '.',
        paths: [
          'gravitee-apim-e2e/lib/management-webclient-sdk',
          'gravitee-apim-e2e/lib/management-v2-webclient-sdk',
          'gravitee-apim-e2e/lib/portal-webclient-sdk',
        ],
      }),
    ];
    return new Job(E2EGenerateSDKJob.jobName, OpenJdkNodeExecutor.create('small'), steps);
  }
}
