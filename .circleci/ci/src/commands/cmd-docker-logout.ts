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

export class DockerLogoutCommand {
  private static commandName = 'cmd-docker-logout';
  public static get(environment: CircleCIEnvironment, isProd: boolean): ReusableCommand {
    const dockerRegistryName = isProd ? 'Docker Hub' : 'Azure Container Registry';
    const dockerRegistry = isProd ? '' : ' graviteeio.azurecr.io';

    const steps = [];

    let name = '';
    if (isProd && environment.isDryRun) {
      name = `No logout from ${dockerRegistryName} - Dry-Run`;
      steps.push(
        new commands.Run({
          name,
          command: `echo "DRY RUN Mode. Build only"`,
        }),
      );
    } else {
      name = `Logout from ${dockerRegistryName}`;
      steps.push(
        new commands.Run({
          name,
          command: `docker logout${dockerRegistry}`,
        }),
      );
    }
    return new reusable.ReusableCommand(DockerLogoutCommand.commandName, steps, undefined, name);
  }
}
