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
import { commands, Config, reusable } from '@circleci/circleci-config-sdk';
import { ReusableCommand } from '@circleci/circleci-config-sdk/dist/src/lib/Components/Commands/exports/Reusable';
import { keeper } from '../orbs/keeper';
import { config } from '../config';

export class PrepareGpgCmd {
  private static commandName = 'cmd-prepare-gpg';

  public static get(dynamicConfig: Config): ReusableCommand {
    dynamicConfig.importOrb(keeper);

    return new reusable.ReusableCommand(
      PrepareGpgCmd.commandName,
      [
        new reusable.ReusedCommand(keeper.commands['install']),
        new commands.Run({
          command: `ksm secret notation ${config.secrets.gpgPublicKey} > pub.key
gpg --import pub.key

ksm secret notation ${config.secrets.gpgPrivateKey} > private.key
# Need --batch to be able to import private key
gpg --import --batch private.key`,
        }),
      ],
      undefined,
      'Prepare GPG command',
    );
  }
}
