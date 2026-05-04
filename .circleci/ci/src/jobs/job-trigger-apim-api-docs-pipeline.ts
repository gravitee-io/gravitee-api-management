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
import { commands, Job } from '@circleci/circleci-config-sdk';
import { CircleCIEnvironment } from '../pipelines';
import { Command } from '@circleci/circleci-config-sdk/dist/src/lib/Components/Commands/exports/Command';
import { BaseExecutor } from '../executors';

/**
 * Notifies the gravitee-apim-api-docs repository of a new APIM release so it
 * regenerates its OpenAPI documentation site. Fire-and-forget: the docs
 * pipeline absorbs Sonatype → Maven Central propagation delay on its side, so
 * we do not wait for completion here.
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
--data '{"parameters":{"version":"${environment.graviteeioVersion}", "dry_run":${environment.isDryRun}}}'
echo "Docs ingestion pipeline triggered for APIM ${environment.graviteeioVersion}."`,
      }),
    ];
    return new Job(TriggerApimApiDocsPipelineJob.jobName, BaseExecutor.create(), steps);
  }
}
