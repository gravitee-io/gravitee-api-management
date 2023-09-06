import { Config, reusable } from '@circleci/circleci-config-sdk';
import { ReusableCommand } from '@circleci/circleci-config-sdk/dist/src/lib/Components/Commands/exports/Reusable';
import { config } from '../config';
import { keeper } from '../orbs/keeper';
import { slack } from '../orbs/slack';
import { supportBranchPattern } from '../utils';

export class NotifyOnFailureCommand {
  private static commandName = 'cmd-notify-on-failure';

  public static get(dynamicConfig: Config): ReusableCommand {
    dynamicConfig.importOrb(keeper);
    dynamicConfig.importOrb(slack);

    return new reusable.ReusableCommand(NotifyOnFailureCommand.commandName, [
      new reusable.ReusedCommand(keeper.commands['env-export'], {
        'secret-url': config.secrets.slackAccessToken,
        'var-name': 'SLACK_ACCESS_TOKEN',
      }),
      new reusable.ReusedCommand(slack.commands.notify, {
        channel: config.slack.channels.apiManagementTeamNotifications,
        branch_pattern: `master,${supportBranchPattern}`,
        event: 'fail',
        template: 'basic_fail_1',
      }),
    ]);
  }
}
