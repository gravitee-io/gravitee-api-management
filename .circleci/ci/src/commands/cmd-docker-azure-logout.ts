import { ReusableCommand } from '@circleci/circleci-config-sdk/dist/src/lib/Components/Commands/exports/Reusable';
import { commands, reusable } from '@circleci/circleci-config-sdk';

export class DockerAzureLogoutCommand {
  private static commandName = 'cmd-docker-azure-logout';
  public static get(): ReusableCommand {
    return new reusable.ReusableCommand(
      DockerAzureLogoutCommand.commandName,
      [
        new commands.Run({
          name: 'Logout from Azure Container Registry',
          command: 'docker logout graviteeio.azurecr.io',
        }),
      ],
      undefined,
      'Logout from Azure Container Registry',
    );
  }
}
