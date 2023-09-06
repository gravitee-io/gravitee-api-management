import { commands, Config, Job, reusable } from '@circleci/circleci-config-sdk';
import { CircleCIEnvironment } from '../pipelines';
import { keeper } from '../orbs/keeper';
import { slack } from '../orbs/slack';
import { github } from '../orbs/github';
import { Command } from '@circleci/circleci-config-sdk/dist/src/lib/Components/Commands/exports/Command';
import { NodeLtsExecutor } from '../executors';
import { config } from '../config';

export class ReleaseNotesApimJob {
  private static jobName: string = 'job-release-notes-apim';
  public static create(dynamicConfig: Config, environment: CircleCIEnvironment): Job {
    dynamicConfig.importOrb(keeper);
    dynamicConfig.importOrb(github);

    const steps: Command[] = [
      new commands.Checkout(),
      new reusable.ReusedCommand(keeper.commands['env-export'], {
        'secret-url': config.secrets.gitUserName,
        'var-name': 'GIT_USER_NAME',
      }),
      new reusable.ReusedCommand(keeper.commands['env-export'], {
        'secret-url': config.secrets.gitUserEmail,
        'var-name': 'GIT_USER_EMAIL',
      }),
      new reusable.ReusedCommand(keeper.commands['env-export'], {
        'secret-url': config.secrets.githubApiToken,
        'var-name': 'GITHUB_TOKEN',
      }),
      new reusable.ReusedCommand(keeper.commands['env-export'], {
        'secret-url': config.secrets.slackAccessToken,
        'var-name': 'SLACK_ACCESS_TOKEN',
      }),
      new reusable.ReusedCommand(keeper.commands['env-export'], {
        'secret-url': config.secrets.jiraToken,
        'var-name': 'JIRA_TOKEN',
      }),
      new commands.AddSSHKeys({ fingerprints: config.ssh.fingerprints }),
      new commands.Run({
        name: 'Git config',
        command: `git config --global user.name "\${GIT_USER_NAME}"
git config --global user.email "\${GIT_USER_EMAIL}"`,
      }),
      new reusable.ReusedCommand(github.commands['setup']),
      new commands.Run({
        name: 'Install dependencies',
        command: 'npm install',
        working_directory: './release',
      }),
      new commands.Run({
        name: 'Open a PR to create release notes into docs repository',
        command: `npm run zx -- --quiet ci-steps/generate-changelog.mjs --version=${environment.graviteeioVersion}`,
        working_directory: './release',
      }),
    ];

    if (!environment.isDryRun) {
      dynamicConfig.importOrb(slack);

      steps.push(
        new commands.Run({
          name: 'Get RELEASE_NOTES_PR_URL',
          command: 'echo "export RELEASE_NOTES_PR_URL=$(cat /tmp/releaseNotesPrUrl.txt)" >> $BASH_ENV',
        }),
        new reusable.ReusedCommand(slack.commands['notify'], {
          channel: config.slack.channels.graviteeReleaseAlerts,
          event: 'pass',
          custom: `{
  "blocks": [
    {
      "type": "section",
      "text": {
        "type": "mrkdwn",
        "text": ":memo: APIM Changelog ${environment.graviteeioVersion} can be completed <\${RELEASE_NOTES_PR_URL}|here> @tech_writers_team :pray:"
      }
    }
  ]
}`,
        }),
      );
    }

    return new Job(ReleaseNotesApimJob.jobName, NodeLtsExecutor.create(), steps);
  }
}
