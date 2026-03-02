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
import { Command } from '@circleci/circleci-config-sdk/dist/src/lib/Components/Commands/exports/Command';
import { NodeLtsExecutor } from '../../executors';
import { InstallYarnCommand, NotifyOnFailureCommand, WorkspaceInstallCommand } from '../../commands';
import { orbs } from '../../orbs';
import { config } from '../../config';
import { CircleCIEnvironment } from '../../pipelines';

export class ChromaticConsoleJob {
  private static jobName = 'job-console-webui-chromatic-deployment';

  public static create(dynamicConfig: Config, environment: CircleCIEnvironment): Job {
    dynamicConfig.importOrb(orbs.keeper);
    dynamicConfig.importOrb(orbs.github);

    const installYarnCommand = InstallYarnCommand.get();
    dynamicConfig.addReusableCommand(installYarnCommand);

    const workspaceInstallCommand = WorkspaceInstallCommand.get();
    dynamicConfig.addReusableCommand(workspaceInstallCommand);

    const notifyOnFailureCommand = NotifyOnFailureCommand.get(dynamicConfig, environment);
    dynamicConfig.addReusableCommand(notifyOnFailureCommand);

    const steps: Command[] = [
      new commands.Checkout(),
      new commands.workspace.Attach({ at: '.' }),
      new reusable.ReusedCommand(installYarnCommand),
      new reusable.ReusedCommand(workspaceInstallCommand),
      new reusable.ReusedCommand(orbs.keeper.commands['env-export'], {
        'secret-url': config.secrets.githubApiToken,
        'var-name': 'GITHUB_TOKEN',
      }),
      new reusable.ReusedCommand(orbs.keeper.commands['env-export'], {
        'secret-url': config.secrets.chromaticProjectToken,
        'var-name': 'CHROMATIC_PROJECT_TOKEN',
      }),
      // TODO:
      //  - Handle npx chromatic command failure, make the job fails
      //  - Create a new project in Chromatic and update the token
      new commands.Run({
        name: 'Running Chromatic',
        command: `SB_URL=$(npx chromatic --project-token=$CHROMATIC_PROJECT_TOKEN --exit-once-uploaded -d=storybook-all/storybook-static | grep -o "View your Storybook at https:\\/\\/[0-9a-z-]*\\.chromatic\\.com" | grep -o "https:.*")
echo "export SB_URL=$SB_URL" >> $BASH_ENV`,
      }),
      new reusable.ReusedCommand(orbs.github.commands['setup']),
      new commands.Run({
        name: 'Edit Pull Request Description',
        command: `# First check there is an associated pull request, otherwise just stop the job here
if ! gh pr view --json title;
then
  echo "No PR found for this branch, stopping the job here."
  exit 0
fi

# If PR state is different from OPEN
if [ "$(gh pr view --json state --jq .state)" != "OPEN" ];
then
  echo "PR is not opened, stopping the job here."
  exit 0
fi

export PR_BODY_STORYBOOK_SECTION="
<!-- Storybook placeholder -->
---

📚&nbsp;&nbsp;View the storybook of this branch [here](\${SB_URL})
<!-- Storybook placeholder end -->
"

export CLEAN_BODY=$(gh pr view --json body --jq .body | sed '/Storybook placeholder -->/,/Storybook placeholder end -->/d')

gh pr edit --body "$CLEAN_BODY$PR_BODY_STORYBOOK_SECTION"`,
      }),
      new reusable.ReusedCommand(notifyOnFailureCommand),
    ];

    return new Job(ChromaticConsoleJob.jobName, NodeLtsExecutor.create('small'), steps);
  }
}
