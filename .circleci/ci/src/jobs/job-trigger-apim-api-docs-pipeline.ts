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
import { corePin } from '../utils';

/**
 * Notifies the gravitee-apim-api-docs repository of a new APIM release so it
 * regenerates its OpenAPI documentation site. Fire-and-forget: the docs
 * pipeline resolves the jars from the Azure feed on its side, so we do not
 * wait for completion here.
 *
 * Two versions, because the site needs both: it polls `gravitee-apim-rest-api-management-rest-<v>.jar`
 * before ingesting, and that jar is a core artefact, while the specs are published under the number
 * users look for. Sent one, it would wait an hour for a jar that no longer exists under that number.
 */
export class TriggerApimApiDocsPipelineJob {
  private static jobName: string = 'job-trigger-apim-api-docs-pipeline';
  public static create(environment: CircleCIEnvironment): Job {
    const steps: Command[] = [
      new commands.Run({
        name: 'Trigger gravitee-apim-api-docs ingestion pipeline',
        command: `curl --fail --request POST \
--url https://circleci.com/api/v2/project/github/gravitee-io/gravitee-apim-api-docs/pipeline \
--header "Circle-Token: \${CIRCLE_TOKEN}" \
--header 'content-type: application/json' \
--data '{"parameters":{"version":"${environment.graviteeioVersion}", "core_version":"${corePin(environment)}", "dry_run":${environment.isDryRun}}}'
echo "Docs ingestion pipeline triggered for APIM ${environment.graviteeioVersion}."`,
      }),
    ];
    return new Job(TriggerApimApiDocsPipelineJob.jobName, BaseExecutor.create(), steps);
  }
}
