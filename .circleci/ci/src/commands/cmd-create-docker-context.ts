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
