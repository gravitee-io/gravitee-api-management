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
import { commands, reusable } from '@circleci/circleci-config-sdk';
import { CircleCIEnvironment } from '../pipelines';

export class DockerAzureLogoutCommand {
  private static commandName = 'cmd-docker-azure-logout';
  public static get(environment: CircleCIEnvironment, isProd: boolean): ReusableCommand {
    const steps = [];

    if (isProd && !environment.isDryRun) {
      steps.push(
        new commands.Run({
          name: `Logout from Docker Hub`,
          command: `docker logout`,
        }),
      );
    } else if (!isProd) {
      steps.push(
        new commands.Run({
          name: `Logout from Azure Container Registry`,
          command: `docker logout graviteeio.azurecr.io`,
        }),
      );
    }

    return new reusable.ReusableCommand(DockerAzureLogoutCommand.commandName, steps, undefined, 'Logout from Azure Container Registry');
  }
}
