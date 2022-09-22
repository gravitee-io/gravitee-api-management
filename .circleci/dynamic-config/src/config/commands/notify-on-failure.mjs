import { ReusableCommand, ReusedCommand, Run } from '../../sdk/index.mjs';
import { keeperOrb } from '../orbs/keeper.mjs';
import { slackOrb } from '../orbs/slack.mjs';

export function createNotifyOnFailureCommand(slackChannel) {
  return new ReusableCommand(`notify-on-failure`, [
    new ReusedCommand(keeperOrb.commands['env-export'], {
      'secret-url': 'keeper://ZOz4db245GNaETVwmPBk8w/field/password',
      'var-name': 'SLACK_ACCESS_TOKEN',
    }),
    new ReusedCommand(slackOrb.commands['notify'], {
      channel: slackChannel,
      branch_pattern: 'master,[0-9]+.[0-9]+.x',
      event: 'fail',
      template: 'basic_fail_1',
    }),
  ]);
}
