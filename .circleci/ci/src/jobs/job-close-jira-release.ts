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
import { Command, Config, Job, commands, reusable } from '../circleci-config';
import { CircleCIEnvironment } from '../pipelines';
import { keeper } from '../orbs/keeper';
import { NodeLtsExecutor } from '../executors';
import { config } from '../config';
import { InstallYarnCommand } from '../commands';

/**
 * Closes the released version in Jira, opens the next one so the fix versions set on merge have
 * somewhere to go, and reports the tickets of the version that are not closed.
 */
export class CloseJiraReleaseJob {
  private static jobName: string = 'job-close-jira-release';
  public static create(dynamicConfig: Config, environment: CircleCIEnvironment): Job {
    dynamicConfig.importOrb(keeper);

    const installYarnCmd = InstallYarnCommand.get();
    dynamicConfig.addReusableCommand(installYarnCmd);

    const steps: Command[] = [
      new commands.Checkout(),
      new reusable.ReusedCommand(keeper.commands['env-export'], {
        'secret-url': config.secrets.jiraToken,
        'var-name': 'JIRA_TOKEN',
      }),
      new reusable.ReusedCommand(keeper.commands['env-export'], {
        'secret-url': config.secrets.slackAccessToken,
        'var-name': 'SLACK_ACCESS_TOKEN',
      }),
      new reusable.ReusedCommand(installYarnCmd),
      new commands.Run({
        name: 'Install dependencies',
        command: 'yarn',
        working_directory: './release',
      }),
      new commands.Run({
        name: 'Close the release in Jira',
        command: `yarn zx --quiet ci-steps/close-jira-release.mjs --version=${environment.graviteeioVersion}${environment.isDryRun ? ' --dry-run' : ''}`,
        working_directory: './release',
        environment: { SLACK_CHANNEL: config.slack.channels.graviteeReleaseAlerts },
      }),
    ];

    return new Job(CloseJiraReleaseJob.jobName, NodeLtsExecutor.create('small'), steps);
  }
}
