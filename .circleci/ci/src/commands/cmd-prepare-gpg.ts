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
