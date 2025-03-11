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
import { NodeLtsExecutor } from '../executors';
import { orbs } from '../orbs';
import { config } from '../config';
import { NotifyOnFailureCommand } from '../commands';

export class PublishPrEnvUrlsJob {
  private static jobName = 'job-publish-pr-env-urls';

  public static create(dynamicConfig: Config): Job {
    dynamicConfig.importOrb(orbs.keeper);
    dynamicConfig.importOrb(orbs.github);

    const notifyOnFailureCommand = NotifyOnFailureCommand.get(dynamicConfig);
    dynamicConfig.addReusableCommand(notifyOnFailureCommand);

    return new Job(PublishPrEnvUrlsJob.jobName, NodeLtsExecutor.create('small'), [
      new commands.Checkout(),
      new commands.workspace.Attach({ at: '.' }),
      new reusable.ReusedCommand(orbs.keeper.commands['env-export'], {
        'secret-url': config.secrets.githubApiToken,
        'var-name': 'GITHUB_TOKEN',
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
export PR_NUMBER=$(gh pr view --json number --jq .number)
export PR_BODY_ENV_SECTION="
<!-- Environment placeholder -->

🏗️ Your changes can be tested here and will be available soon:
      Console: [https://pr.team-apim.gravitee.dev/$PR_NUMBER/console](https://pr.team-apim.gravitee.dev/$PR_NUMBER/console)
      Portal: [https://pr.team-apim.gravitee.dev/$PR_NUMBER/portal](https://pr.team-apim.gravitee.dev/$PR_NUMBER/portal)
      Management-api: [https://pr.team-apim.gravitee.dev/$PR_NUMBER/api/management](https://pr.team-apim.gravitee.dev/$PR_NUMBER/api/management)
      Gateway v4: [https://pr.team-apim.gravitee.dev/$PR_NUMBER](https://pr.team-apim.gravitee.dev/$PR_NUMBER)
      Gateway v3: [https://pr.gateway-v3.team-apim.gravitee.dev/$PR_NUMBER](https://pr.gateway-v3.team-apim.gravitee.dev/$PR_NUMBER)

<!-- Environment placeholder end -->
"

export CLEAN_BODY=$(gh pr view --json body --jq .body | sed '/Environment placeholder -->/,/Environment placeholder end -->/d')

gh pr edit --body "$CLEAN_BODY$PR_BODY_ENV_SECTION"`,
      }),
      new reusable.ReusedCommand(notifyOnFailureCommand),
    ]);
  }
}
