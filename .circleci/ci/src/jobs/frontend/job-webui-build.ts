import { commands, Config, Job, parameters, reusable } from '@circleci/circleci-config-sdk';
import { Command } from '@circleci/circleci-config-sdk/dist/src/lib/Components/Commands/exports/Command';
import { CommandParameterLiteral } from '@circleci/circleci-config-sdk/dist/src/lib/Components/Parameters/types/CustomParameterLiterals.types';
import { NodeLtsExecutor } from '../../executors';
import { BuildUiImageCommand, NotifyOnFailureCommand, WebuiInstallCommand } from '../../commands';
import { CircleCIEnvironment } from '../../pipelines';
import { computeApimVersion } from '../../utils';

export class WebuiBuildJob {
  private static jobName = 'job-webui-build';

  private static customParametersList = new parameters.CustomParametersList<CommandParameterLiteral>([
    new parameters.CustomParameter('apim-ui-project', 'string', '', 'the name of the UI project to build'),
    new parameters.CustomParameter('docker-image-name', 'string', '', 'the name of the image'),
  ]);

  public static create(dynamicConfig: Config, environment: CircleCIEnvironment): Job {
    const webUiInstallCommand = WebuiInstallCommand.get();
    dynamicConfig.addReusableCommand(webUiInstallCommand);

    const notifyOnFailureCommand = NotifyOnFailureCommand.get(dynamicConfig);
    dynamicConfig.addReusableCommand(notifyOnFailureCommand);

    const buildUiImageCommand = BuildUiImageCommand.get(dynamicConfig, environment);
    dynamicConfig.addReusableCommand(buildUiImageCommand);

    const apimVersion = computeApimVersion(environment);

    const steps: Command[] = [
      new commands.Checkout(),
      new commands.workspace.Attach({ at: '.' }),
      new commands.SetupRemoteDocker(),
      new reusable.ReusedCommand(webUiInstallCommand, { 'apim-ui-project': '<< parameters.apim-ui-project >>' }),
      new commands.Run({
        name: 'Update Build version',
        command: `sed -i 's/"version": ".*"/"version": "${apimVersion}"/' << parameters.apim-ui-project >>/build.json`,
      }),
      new commands.Run({
        name: 'Build',
        command: 'npm run build:prod',
        environment: {
          NODE_OPTIONS: '--max_old_space_size=4086',
        },
        working_directory: '<< parameters.apim-ui-project >>',
      }),
      new reusable.ReusedCommand(buildUiImageCommand, {
        'docker-image-name': '<< parameters.docker-image-name >>',
        'apim-ui-project': '<< parameters.apim-ui-project >>',
      }),
      new reusable.ReusedCommand(notifyOnFailureCommand),
      new commands.workspace.Persist({
        root: '.',
        paths: ['<< parameters.apim-ui-project >>/dist'],
      }),
    ];

    return new reusable.ParameterizedJob(WebuiBuildJob.jobName, NodeLtsExecutor.create('large'), WebuiBuildJob.customParametersList, steps);
  }
}
