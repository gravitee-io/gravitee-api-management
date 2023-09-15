/*
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
