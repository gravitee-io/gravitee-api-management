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
import { commands, Config, Job, reusable } from '../circleci-config';
import { NodeLtsExecutor } from '../executors';
import { orbs } from '../orbs';
import { config } from '../config';
import { CircleCIEnvironment } from '../pipelines';
import { computeImagesTag, computeLineImagesTag } from '../utils';

/**
 * Runs the platform end-to-end journeys a pull request names, on that pull request's own images.
 *
 * The description carries the list (`CUJ: CUJ-72, CUJ-161`) and, for a journey still in review
 * in gravitee-platform-e2e, the branch holding it (`CUJ-branch: <branch>`). The run builds a
 * cluster from the `<branch>-<sha>` images this workflow pushed, with the chart of the line the
 * pull request targets. Its result is the `e2e/CUJ` commit status, advisory: a failing journey
 * leaves this job green, so it never blocks the workflow.
 */
export class TriggerPlatformCujsJob {
  private static jobName = 'job-trigger-platform-cujs';
  // A run builds a full kind cluster before testing; the nightly takes about 40 minutes.
  private static pollMinutes = 120;

  public static create(dynamicConfig: Config, environment: CircleCIEnvironment): Job {
    dynamicConfig.importOrb(orbs.keeper);
    dynamicConfig.importOrb(orbs.github);

    const imageTag = computeImagesTag(environment.branch, environment.sha1);
    const lineTag = computeLineImagesTag(environment.baseBranch);

    return new Job(TriggerPlatformCujsJob.jobName, NodeLtsExecutor.create('small'), [
      new reusable.ReusedCommand(orbs.keeper.commands['env-export'], {
        'secret-url': config.secrets.githubApiToken,
        'var-name': 'GITHUB_TOKEN',
      }),
      new reusable.ReusedCommand(orbs.github.commands['setup']),
      new commands.Run({
        name: 'Run the CUJs the pull request names',
        command: `if ! body=$(gh pr view "\${CIRCLE_BRANCH}" --repo gravitee-io/gravitee-api-management --json body --jq .body); then
  echo "No pull request for this branch: no CUJ to run."
  exit 0
fi
line() { printf '%s\\n' "\${body}" | tr -d '\\r' | sed -n "s/^$1:[[:space:]]*//p" | head -n 1 | tr -d ' '; }
CUJS=$(line CUJ)
if [ -z "\${CUJS}" ]; then
  echo "No 'CUJ:' line in the pull request description: nothing to run."
  exit 0
fi
if ! [[ "\${CUJS}" =~ ^CUJ-[0-9]+(,CUJ-[0-9]+)*$ ]]; then
  echo "The 'CUJ:' line must look like 'CUJ: CUJ-72, CUJ-161', got: \${CUJS}"
  exit 1
fi
E2E_BRANCH=$(line CUJ-branch)
E2E_BRANCH=\${E2E_BRANCH:-main}
if ! [[ "\${E2E_BRANCH}" =~ ^[A-Za-z0-9._/-]+$ ]]; then
  echo "The 'CUJ-branch:' line must name a gravitee-platform-e2e branch, got: \${E2E_BRANCH}"
  exit 1
fi

status() {
  gh api "repos/gravitee-io/gravitee-api-management/statuses/\${CIRCLE_SHA1}" \\
    -f state="$1" -f context=e2e/CUJ -f target_url="$2" -f description="$3" > /dev/null
}

params=$(jq -n --arg c "\${CUJS}" --arg line "${lineTag}" --arg image "${imageTag}" \\
  '{trigger: "cuj", cujs: $c, "apim-tag": $line, "am-tag": $line, "apim-image-tag": $image}')
response=$(curl --fail --silent --show-error --request POST \\
  --url https://circleci.com/api/v2/project/gh/gravitee-io/gravitee-platform-e2e/pipeline \\
  --header "Circle-Token: \${CIRCLE_TOKEN}" \\
  --header 'content-type: application/json' \\
  --data "$(jq -n --argjson p "\${params}" --arg b "\${E2E_BRANCH}" '{branch: $b, parameters: $p}')")
pipeline_id=$(jq -r .id <<< "\${response}")
url="https://app.circleci.com/pipelines/github/gravitee-io/gravitee-platform-e2e/$(jq -r .number <<< "\${response}")"
echo "\${CUJS} on gravitee-platform-e2e@\${E2E_BRANCH}, images ${imageTag}, chart ${lineTag}: \${url}"
status pending "\${url}" "\${CUJS}: running"

for _ in $(seq 1 ${TriggerPlatformCujsJob.pollMinutes}); do
  sleep 60
  state=$(curl --silent --header "Circle-Token: \${CIRCLE_TOKEN}" \\
    "https://circleci.com/api/v2/pipeline/\${pipeline_id}/workflow" | jq -r '.items[0].status // "pending"') || state=pending
  case "\${state}" in
    success) status success "\${url}" "\${CUJS}: passed"; exit 0 ;;
    failed | error | canceled | unauthorized | not_run) status failure "\${url}" "\${CUJS}: \${state}"; exit 0 ;;
  esac
done
status error "\${url}" "\${CUJS}: no result after ${TriggerPlatformCujsJob.pollMinutes} minutes"`,
      }),
    ]);
  }
}
