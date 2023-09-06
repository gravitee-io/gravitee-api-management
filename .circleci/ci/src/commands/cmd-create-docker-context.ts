import { commands, reusable } from '@circleci/circleci-config-sdk';
import { ReusableCommand } from '@circleci/circleci-config-sdk/dist/src/lib/Components/Commands/exports/Reusable';

export class CreateDockerContextCommand {
  private static commandName = 'cmd-create-docker-context';

  public static get(): ReusableCommand {
    return new reusable.ReusableCommand(CreateDockerContextCommand.commandName, [
      new commands.Run({
        name: 'Create docker context for buildx',
        command: `docker context create tls-env
docker buildx create tls-env --use`,
      }),
    ]);
  }
}
