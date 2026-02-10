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
import { config } from '../config';

export class WorkspaceInstallCommand {
  private static commandName = 'cmd-workspace-install';

  public static get(): reusable.ReusableCommand {
    return new reusable.ReusableCommand(WorkspaceInstallCommand.commandName, [
      new commands.cache.Restore({
        name: 'Restore Yarn workspace cache',
        keys: [
          `${config.cache.prefix}-workspace-{{ .Branch }}-{{ checksum "yarn.lock" }}`,
          `${config.cache.prefix}-workspace-{{ .Branch }}`,
        ],
      }),
      new commands.Run({
        name: 'Install workspace dependencies',
        command: 'yarn install --frozen-lockfile',
      }),
      new commands.cache.Save({
        name: 'Save Yarn workspace cache',
        key: `${config.cache.prefix}-workspace-{{ .Branch }}-{{ checksum "yarn.lock" }}`,
        paths: ['./node_modules'],
      }),
    ]);
  }
}
