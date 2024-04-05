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

export class InstallYarnCommand {
  private static commandName = 'cmd-install-yarn';

  public static get(): reusable.ReusableCommand {
    return new reusable.ReusableCommand(InstallYarnCommand.commandName, [
      new commands.Run({
        name: 'Yarn Set Version Stable',
        command: `yarn set version ${config.yarn.version}`,
      }),
    ]);
  }
}
