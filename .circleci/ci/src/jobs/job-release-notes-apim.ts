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
import { commands, Config, Job, reusable } from '@circleci/circleci-config-sdk';
import { CircleCIEnvironment } from '../pipelines';
import { keeper } from '../orbs/keeper';
import { slack } from '../orbs/slack';
import { github } from '../orbs/github';
import { Command } from '@circleci/circleci-config-sdk/dist/src/lib/Components/Commands/exports/Command';
import { NodeLtsExecutor } from '../executors';
import { config } from '../config';
import { InstallYarnCommand } from '../commands';

export class ReleaseNotesApimJob {
  private static jobName: string = 'job-release-notes-apim';
  public static create(dynamicConfig: Config, environment: CircleCIEnvironment): Job {
    dynamicConfig.importOrb(keeper);
    dynamicConfig.importOrb(github);

    const installYarnCmd = InstallYarnCommand.get();
    dynamicConfig.addReusableCommand(installYarnCmd);

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
      new reusable.ReusedCommand(installYarnCmd),
      new commands.Run({
        name: 'Install dependencies',
        command: 'yarn',
        working_directory: './release',
      }),
      new commands.Run({
        name: 'Open a PR to create release notes into docs repository',
        command: `yarn zx --quiet ci-steps/generate-changelog.mjs --version=${environment.graviteeioVersion}`,
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
