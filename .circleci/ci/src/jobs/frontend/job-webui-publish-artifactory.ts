import { commands, Config, Job, parameters, reusable } from '@circleci/circleci-config-sdk';
import { CommandParameterLiteral } from '@circleci/circleci-config-sdk/dist/src/lib/Components/Parameters/types/CustomParameterLiterals.types';
import { NodeLtsExecutor } from '../../executors';
import { Command } from '@circleci/circleci-config-sdk/dist/src/lib/Components/Commands/exports/Command';
import { computeApimVersion } from '../../utils';
import { CircleCIEnvironment } from '../../pipelines';
import { orbs } from '../../orbs';
import { config } from '../../config';
import { NotifyOnFailureCommand } from '../../commands';

export class WebuiPublishArtifactoryJob {
  private static jobName = 'job-webui-publish-artifactory';

  private static customParametersList = new parameters.CustomParametersList<CommandParameterLiteral>([
    new parameters.CustomParameter('apim-ui-project', 'string', '', 'the name of the UI project to publish'),
  ]);

  public static create(dynamicConfig: Config, environment: CircleCIEnvironment): Job {
    let apimVersion = computeApimVersion(environment);
    const suffix = '-SNAPSHOT';
    if (apimVersion.endsWith(suffix)) {
      apimVersion = apimVersion.slice(0, -suffix.length);
    }

    dynamicConfig.importOrb(orbs.artifactory);

    const notifyOnFailureCmd = NotifyOnFailureCommand.get(dynamicConfig);
    dynamicConfig.addReusableCommand(notifyOnFailureCmd);

    const steps: Command[] = [
      new commands.workspace.Attach({ at: '.' }),
      new reusable.ReusedCommand(orbs.keeper.commands['env-export'], {
        'secret-url': config.secrets.artifactoryUser,
        'var-name': 'ARTIFACTORY_USER',
      }),
      new reusable.ReusedCommand(orbs.keeper.commands['env-export'], {
        'secret-url': config.secrets.artifactoryApiKey,
        'var-name': 'ARTIFACTORY_API_KEY',
      }),
      new reusable.ReusedCommand(orbs.artifactory.commands['install']),
      new reusable.ReusedCommand(orbs.artifactory.commands['configure']),
      new commands.Run({
        name: 'Update Build version',
        command: 'sed -i "s/-SNAPSHOT//" dist/build.json',
        working_directory: '<< parameters.apim-ui-project >>',
      }),
      new commands.Run({
        name: 'Rename and zip dist folder',
        command: `mv dist << parameters.apim-ui-project >>-${apimVersion} && zip -r dist.zip << parameters.apim-ui-project >>-${apimVersion}`,
        working_directory: '<< parameters.apim-ui-project >>',
      }),
      new reusable.ReusedCommand(orbs.artifactory.commands['upload'], {
        source: '<< parameters.apim-ui-project >>/dist.zip',
        target: `${
          environment.isDryRun ? 'dry-run-releases' : 'gravitee-releases'
        }/io/gravitee/apim/ui/<< parameters.apim-ui-project >>/${apimVersion}/<< parameters.apim-ui-project >>-${apimVersion}.zip`,
      }),
      new reusable.ReusedCommand(notifyOnFailureCmd),
    ];

    return new reusable.ParameterizedJob(
      WebuiPublishArtifactoryJob.jobName,
      NodeLtsExecutor.create('small'),
      WebuiPublishArtifactoryJob.customParametersList,
      steps,
      {
        environment: {
          ARTIFACTORY_URL: config.artifactoryUrl,
        },
      },
    );
  }
}
