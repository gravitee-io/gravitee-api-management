import { ReusableCommand } from '@circleci/circleci-config-sdk/dist/src/lib/Components/Commands/exports/Reusable';
import { commands, parameters, reusable } from '@circleci/circleci-config-sdk';
import { CommandParameterLiteral } from '@circleci/circleci-config-sdk/dist/src/lib/Components/Parameters/types/CustomParameterLiterals.types';

export class AddDockerImageInSnykCommand {
  private static commandName = 'cmd-add-docker-image-in-snyk';

  private static customParametersList = new parameters.CustomParametersList<CommandParameterLiteral>([
    new parameters.CustomParameter('docker-image-name', 'string'),
    new parameters.CustomParameter('version', 'string'),
  ]);

  public static get(): ReusableCommand {
    return new reusable.ReusableCommand(
      AddDockerImageInSnykCommand.commandName,
      [
        new commands.Run({
          name: 'Add << parameters.docker-image-name >> << parameters.version >> to Snyk',
          command: `curl --fail \\
    --include \\
    --request POST \\
    --header "Content-Type: application/json; charset=utf-8" \\
    --header "Authorization: token \${SNYK_API_TOKEN}" \\
    --data-binary "{
  \\"target\\": {
    \\"name\\": \\"<< parameters.docker-image-name >>:<< parameters.version >>\\"
  }
}" \\
"https://api.snyk.io/api/v1/org/\${SNYK_ORG_ID}/integrations/\${SNYK_INTEGRATION_ID}/import"`,
        }),
      ],
      AddDockerImageInSnykCommand.customParametersList,
    );
  }
}
