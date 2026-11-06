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
  public static create(environment: CircleCIEnvironment, distribution: 'dev' | 'prod', osDistribution: string = 'debian'): Job {
    // Distinct job name per os-distribution so several SaaS triggers can coexist in a workflow.
    const jobName =
      osDistribution === 'debian' ? TriggerSaasDockerImagesJob.jobName : `${TriggerSaasDockerImagesJob.jobName}-${osDistribution}`;
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
        }, "os-distribution":"${osDistribution}"}}' | jq -r '.id')
if [[ -z "$PIPELINE_ID" || "$PIPELINE_ID" == "null" ]]; then
    echo "Failed to trigger the SaaS Docker images pipeline."
    exit 1
fi
echo "Pipeline triggered with ID: $PIPELINE_ID"
# Right after the trigger the pipeline is not readable yet: the API answers 404 "Pipeline run not found." until it is.
waitForPipelineCompletion() {
    local pendingSince=$SECONDS
    while true; do
        STATUS=$(curl --silent --request GET --url "https://circleci.com/api/v2/pipeline/$PIPELINE_ID/workflow" --header "Circle-Token: \${CIRCLE_TOKEN}" | jq -r 'if .items then (if any(.items[]; .status == "failed" or .status == "error" or .status == "canceled" or .status == "unauthorized" or .status == "not_run") then "failed" elif (.items | length) > 0 and all(.items[]; .status == "success") then "success" else "running" end) elif .message == "Pipeline run not found." then "pending" else "error: " + (.message // "unreadable response") end') || STATUS="pending"
        echo "Current status: $STATUS"
        case "$STATUS" in
            success)
                echo "Pipeline completed successfully."
                return 0
                ;;
            failed)
                echo "Pipeline failed."
                exit 1
                ;;
            error:*)
                exit 1
                ;;
            pending)
                if (( SECONDS - pendingSince > 300 )); then
                    echo "Pipeline $PIPELINE_ID is still not readable after 5 minutes."
                    exit 1
                fi
                ;;
            *)
                pendingSince=$SECONDS
                ;;
        esac
        sleep 30
    done
}
waitForPipelineCompletion`,
      }),
    ];
    return new Job(jobName, BaseExecutor.create(), steps);
  }
}
