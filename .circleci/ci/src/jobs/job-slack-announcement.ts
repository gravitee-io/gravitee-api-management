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
import { Config, Job, parameters, reusable } from '@circleci/circleci-config-sdk';
import { config } from '../config';
import { keeper } from '../orbs/keeper';
import { Command } from '@circleci/circleci-config-sdk/dist/src/lib/Components/Commands/exports/Command';
import { slack } from '../orbs/slack';
import { BaseExecutor } from '../executors';

export class SlackAnnouncementJob {
  private static jobName = 'job-slack-announcement';
  private static customParametersList = new parameters.CustomParametersList([new parameters.CustomParameter('message', 'string', '')]);
  public static create(dynamicConfig: Config): Job {
    dynamicConfig.importOrb(keeper);
    dynamicConfig.importOrb(slack);

    const steps: Command[] = [
      new reusable.ReusedCommand(keeper.commands['env-export'], {
        'secret-url': config.secrets.slackAccessToken,
        'var-name': 'SLACK_ACCESS_TOKEN',
      }),
      new reusable.ReusedCommand(slack.commands['notify'], {
        channel: config.slack.channels.graviteeReleaseAlerts,
        event: 'always',
        custom: `{
  "blocks": [
    {
      "type": "section",
      "text": {
        "type": "mrkdwn",
        "text": "<< parameters.message >>"
      }
    }
  ]
}`,
      }),
    ];

    return new reusable.ParameterizedJob(
      SlackAnnouncementJob.jobName,
      BaseExecutor.create('small'),
      SlackAnnouncementJob.customParametersList,
      steps,
    );
  }
}
