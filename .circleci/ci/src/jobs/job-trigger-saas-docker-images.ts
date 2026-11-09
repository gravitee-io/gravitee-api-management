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
import { Command, Job, commands } from '../circleci-config';
import { CircleCIEnvironment } from '../pipelines';
import { BaseExecutor } from '../executors';
import { computeDockerTagSuffix } from '../utils';

export class TriggerSaasDockerImagesJob {
  private static jobName: string = 'job-trigger-saas-docker-images';
  /** The SaaS runs the chainguard images alone, so it is the one flavor its pipeline is asked to build. */
  private static osDistribution: string = 'chainguard';

  public static create(environment: CircleCIEnvironment, distribution: 'dev' | 'prod'): Job {
    const steps: Command[] = [
      new commands.Run({
        name: 'Trigger SaaS Docker images pipeline',
        command: `PIPELINE_ID=$(curl --request POST \
--url https://circleci.com/api/v2/project/github/gravitee-io/cloud-distributions/pipeline \
--header "Circle-Token: \${CIRCLE_TOKEN}" \
--header 'content-type: application/json' \
--data '{"parameters":{"project":"apim", "distribution":"${distribution}", "branch-version":"${
          environment.branch
        }", "sha1":"${computeDockerTagSuffix(environment.sha1)}", "release-version":"${environment.graviteeioVersion}", "dry-run":${
          environment.isDryRun
        }, "os-distribution":"${TriggerSaasDockerImagesJob.osDistribution}"}}' | jq -r '.id')
if [[ -z "$PIPELINE_ID" || "$PIPELINE_ID" == "null" ]]; then
    echo "Failed to trigger the SaaS Docker images pipeline."
    exit 1
fi
echo "Pipeline triggered with ID: $PIPELINE_ID"
waitForPipelineCompletion() {
    local notFound=0
    while true; do
        # The statuses of the pipeline's workflows, comma-separated, or the error message when there are none
        STATUS=$(curl --silent --request GET --url "https://circleci.com/api/v2/pipeline/$PIPELINE_ID/workflow" --header "Circle-Token: \${CIRCLE_TOKEN}" | jq -r 'if .items then [.items[].status] | unique | join(",") else .message end') || STATUS=""
        echo "Current status: $STATUS"
        case "$STATUS" in
            success)
                echo "Pipeline completed successfully."
                return 0
                ;;
            *failed* | *error* | *canceled* | *unauthorized* | *not_run*)
                echo "Pipeline failed."
                exit 1
                ;;
            *running* | *on_hold* | *failing*)
                notFound=0
                ;;
            # Right after the trigger the pipeline is not readable yet: wait for it, at most 5 minutes
            "Pipeline run not found." | "")
                notFound=$((notFound + 1))
                if ((notFound > 10)); then
                    echo "Pipeline $PIPELINE_ID is still not readable after 5 minutes."
                    exit 1
                fi
                ;;
            *)
                echo "Unexpected answer from the CircleCI API."
                exit 1
                ;;
        esac
        sleep 30
    done
}
waitForPipelineCompletion`,
      }),
    ];
    return new Job(TriggerSaasDockerImagesJob.jobName, BaseExecutor.create(), steps);
  }
}
