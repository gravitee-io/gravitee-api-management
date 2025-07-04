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
import { CircleCIEnvironment } from '../pipelines';
import { computeImagesTag } from '../utils';

export class PublishPrDockerImagesJob {
  private static jobName = 'job-publish-pr-docker-images';

  public static create(dynamicConfig: Config, environment: CircleCIEnvironment): Job {
    dynamicConfig.importOrb(orbs.keeper);
    dynamicConfig.importOrb(orbs.github);

    const notifyOnFailureCommand = NotifyOnFailureCommand.get(dynamicConfig, environment);
    dynamicConfig.addReusableCommand(notifyOnFailureCommand);

    return new Job(PublishPrDockerImagesJob.jobName, NodeLtsExecutor.create('small'), [
      new commands.Checkout(),
      new commands.workspace.Attach({ at: '.' }),
      new reusable.ReusedCommand(orbs.keeper.commands['env-export'], {
        'secret-url': config.secrets.githubApiToken,
        'var-name': 'GITHUB_TOKEN',
      }),
      new reusable.ReusedCommand(orbs.github.commands['setup']),
      new commands.Run({
        name: 'Edit Pull Request Description',
        environment: {
          BRANCH_TAG: computeImagesTag(environment.branch),
        },
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
export PR_BODY_DOCKER_IMAGES_SECTION="
<!-- Docker Images placeholder -->

🏗️ Your changes can be tested using the Docker images listed below, which will be available shortly:

      Console: graviteeio.azurecr.io/apim-management-ui:$BRANCH_TAG
      Portal: graviteeio.azurecr.io/apim-portal-ui:$BRANCH_TAG
      Management-api: graviteeio.azurecr.io/apim-management-api:$BRANCH_TAG
      Gateway: graviteeio.azurecr.io/apim-gateway:$BRANCH_TAG
      
      You can also run any quick-setup docker compose via:
      1. az acr login -n graviteeio
      2. APIM_REGISTRY=graviteeio.azurecr.io APIM_VERSION=$BRANCH_TAG docker-compose -f ./docker/quick-setup/mongodb/docker-compose.yml up -d

<!-- Docker Images placeholder end -->
"

export CLEAN_BODY=$(gh pr view --json body --jq .body | sed '/Docker Images placeholder -->/,/Docker Images placeholder end -->/d')

gh pr edit --body "$CLEAN_BODY$PR_BODY_DOCKER_IMAGES_SECTION"`,
      }),
      new reusable.ReusedCommand(notifyOnFailureCommand),
    ]);
  }
}
