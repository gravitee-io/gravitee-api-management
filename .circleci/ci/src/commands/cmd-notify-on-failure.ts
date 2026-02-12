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
import { Config, reusable } from '@circleci/circleci-config-sdk';
import { ReusableCommand } from '@circleci/circleci-config-sdk/dist/src/lib/Components/Commands/exports/Reusable';
import { config } from '../config';
import { keeper } from '../orbs/keeper';
import { slack } from '../orbs/slack';
import { CircleCIEnvironment } from '../pipelines';
import { computeSlackChannelToUse } from '../utils/slack';

export class NotifyOnFailureCommand {
  private static commandName = 'cmd-notify-on-failure';

  public static get(dynamicConfig: Config, environment: CircleCIEnvironment): ReusableCommand {
    dynamicConfig.importOrb(keeper);
    dynamicConfig.importOrb(slack);

    const slackChannelToUse = computeSlackChannelToUse(environment.action);

    return new reusable.ReusableCommand(NotifyOnFailureCommand.commandName, [
      new reusable.ReusedCommand(keeper.commands['env-export'], {
        'secret-url': config.secrets.slackAccessToken,
        'var-name': 'SLACK_ACCESS_TOKEN',
      }),
      new reusable.ReusedCommand(slack.commands.notify, {
        channel: slackChannelToUse,
        branch_pattern: 'master,[0-9]+\\.[0-9]+\\.x,alpha-vertx5', // Slack orb only supports POSIX regex. So we must use [0-9] instead of \d.
        event: 'fail',
        template: 'basic_fail_1',
      }),
    ]);
  }
}
