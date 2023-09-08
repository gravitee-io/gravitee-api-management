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
