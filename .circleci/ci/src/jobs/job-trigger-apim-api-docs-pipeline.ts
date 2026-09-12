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

/**
 * Notifies the gravitee-apim-api-docs repository of a new APIM release so it
 * regenerates its OpenAPI documentation site. Fire-and-forget: the docs
 * pipeline absorbs Sonatype → Maven Central propagation delay on its side, so
 * we do not wait for completion here.
 *
 * Two versions go over, because the docs site needs them for different things and they stop being
 * the same number once the reactors release apart. `version` is what the site displays — readers
 * look for "APIM 4.13.4", not for a reactor's internal number. `core_version` is what it fetches:
 * the pipeline polls `gravitee-apim-rest-api-management-rest-<v>.jar` on Maven Central, and that jar
 * only ever exists under the core's version.
 *
 * The docs repository reads `version` alone today and ignores the second parameter, which is
 * harmless. It breaks the day the two numbers first differ, and that is what the second parameter is
 * there to let it fix without a change on this side.
 */
export class TriggerApimApiDocsPipelineJob {
  private static jobName: string = 'job-trigger-apim-api-docs-pipeline';
  public static create(environment: CircleCIEnvironment): Job {
    const steps: Command[] = [
      new commands.Checkout(),
      new commands.Run({
        // The pin is a literal in the distribution pom, so a grep is enough and keeps this job off
        // a JDK image for one value.
        name: 'Read the core version the distribution pins',
        command: `CORE_VERSION=$(sed -n 's#.*<apim.core.version>\\(.*\\)</apim.core.version>.*#\\1#p' gravitee-apim-distribution/pom.xml)
if [ -z "\${CORE_VERSION}" ]; then
  echo "No apim.core.version in gravitee-apim-distribution/pom.xml." >&2
  exit 1
fi
echo "export CORE_VERSION=\${CORE_VERSION}" >> $BASH_ENV
echo "The distribution pins core \${CORE_VERSION}."`,
      }),
      new commands.Run({
        name: 'Trigger gravitee-apim-api-docs ingestion pipeline',
        command: `curl --fail --request POST \
--url https://circleci.com/api/v2/project/github/gravitee-io/gravitee-apim-api-docs/pipeline \
--header "Circle-Token: \${CIRCLE_TOKEN}" \
--header 'content-type: application/json' \
--data '{"parameters":{"version":"${environment.graviteeioVersion}", "core_version":"'"\${CORE_VERSION}"'", "dry_run":${environment.isDryRun}}}'
echo "Docs ingestion pipeline triggered for APIM ${environment.graviteeioVersion}."`,
      }),
    ];
    return new Job(TriggerApimApiDocsPipelineJob.jobName, BaseExecutor.create(), steps);
  }
}
