import { commands, Config, reusable } from '@circleci/circleci-config-sdk';
import { ReusableCommand } from '@circleci/circleci-config-sdk/dist/src/lib/Components/Commands/exports/Reusable';
import { orbs } from '../orbs';
import { config } from '../config';

export class DockerAzureLoginCommand {
  private static commandName = 'cmd-docker-azure-login';

  public static get(dynamicConfig: Config): ReusableCommand {
    dynamicConfig.importOrb(orbs.keeper);

    return new reusable.ReusableCommand(
      DockerAzureLoginCommand.commandName,
      [
        new reusable.ReusedCommand(orbs.keeper.commands['env-export'], {
          'secret-url': config.secrets.azureRegistryUsername,
          'var-name': 'AZURE_DOCKER_REGISTRY_USERNAME',
        }),
        new reusable.ReusedCommand(orbs.keeper.commands['env-export'], {
          'secret-url': config.secrets.azureRegistryPassword,
          'var-name': 'AZURE_DOCKER_REGISTRY_PASSWORD',
        }),
        new commands.Run({
          name: 'Login to Azure Container Registry',
          command:
            'echo $AZURE_DOCKER_REGISTRY_PASSWORD | docker login --username $AZURE_DOCKER_REGISTRY_USERNAME --password-stdin graviteeio.azurecr.io',
        }),
      ],
      undefined,
      'Login to Azure Container Registry',
    );
  }
}
