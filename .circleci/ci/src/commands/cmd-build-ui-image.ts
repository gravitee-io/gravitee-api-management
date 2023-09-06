import { commands, Config, parameters, reusable } from '@circleci/circleci-config-sdk';
import { computeImagesTag, isSupportBranchOrMaster } from '../utils';
import { CircleCIEnvironment } from '../pipelines';
import { CreateDockerContextCommand } from './cmd-create-docker-context';
import { DockerAzureLoginCommand } from './cmd-docker-azure-login';
import { DockerAzureLogoutCommand } from './cmd-docker-azure-logout';
import { Command } from '@circleci/circleci-config-sdk/dist/src/lib/Components/Commands/exports/Command';
import { orbs } from '../orbs';
import { config } from '../config';
import { AddDockerImageInSnykCommand } from './cmd-add-docker-image-in-snyk';

export class BuildUiImageCommand {
  private static commandName = 'cmd-build-ui-image';

  private static customParametersList = new parameters.CustomParametersList([
    new parameters.CustomParameter('docker-image-name', 'string', '', 'the name of the image'),
    new parameters.CustomParameter('apim-ui-project', 'string', '', 'the name of the UI project to build'),
  ]);

  public static get(dynamicConfig: Config, environment: CircleCIEnvironment): reusable.ReusableCommand {
    const tag = computeImagesTag(environment.branch);

    const createDockerContextCommand = CreateDockerContextCommand.get();
    dynamicConfig.addReusableCommand(createDockerContextCommand);

    const dockerAzureLoginCommand = DockerAzureLoginCommand.get(dynamicConfig);
    dynamicConfig.addReusableCommand(dockerAzureLoginCommand);

    const dockerAzureLogoutCommand = DockerAzureLogoutCommand.get();
    dynamicConfig.addReusableCommand(dockerAzureLogoutCommand);

    const steps: Command[] = [
      new reusable.ReusedCommand(createDockerContextCommand),
      new reusable.ReusedCommand(dockerAzureLoginCommand),
      new commands.Run({
        name: 'Build UI docker image',
        command: `docker buildx build --push --platform=linux/arm64,linux/amd64 -f docker/Dockerfile \\
-t graviteeio.azurecr.io/<< parameters.docker-image-name >>:${tag} \\
.`,
        working_directory: '<< parameters.apim-ui-project >>',
      }),
    ];

    if (isSupportBranchOrMaster(environment.branch)) {
      dynamicConfig.importOrb(orbs.keeper);

      steps.push(
        new reusable.ReusedCommand(orbs.keeper.commands['env-export'], {
          'secret-url': config.secrets.snykApiToken,
          'var-name': 'SNYK_API_TOKEN',
        }),
        new reusable.ReusedCommand(orbs.keeper.commands['env-export'], {
          'secret-url': config.secrets.snykOrgId,
          'var-name': 'SNYK_ORG_ID',
        }),
        new reusable.ReusedCommand(orbs.keeper.commands['env-export'], {
          'secret-url': config.secrets.snykIntegrationId,
          'var-name': 'SNYK_INTEGRATION_ID',
        }),
        new reusable.ReusedCommand(AddDockerImageInSnykCommand.get(), {
          'docker-image-name': '<< parameters.docker-image-name >>',
          version: tag,
        }),
      );
    }

    steps.push(new reusable.ReusedCommand(DockerAzureLogoutCommand.get()));

    return new reusable.ReusableCommand(BuildUiImageCommand.commandName, steps, BuildUiImageCommand.customParametersList);
  }
}
