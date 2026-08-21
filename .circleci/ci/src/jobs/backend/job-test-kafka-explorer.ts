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
import { commands, Config } from '../../circleci-config';
import { config } from '../../config';
import { UbuntuExecutor } from '../../executors';
import { AbstractTestJob } from './abstract-job-test';
import { CircleCIEnvironment } from '../../pipelines';

// The only module in the rest-api reactor that starts a container: its suite brings up a Kafka broker
// through docker compose. That is why it needs a machine executor, and why it is worth 89 s of the
// rest-api job's critical path when it runs alongside the rest.
export class TestKafkaExplorerJob extends AbstractTestJob {
  private static readonly module = 'gravitee-apim-rest-api/gravitee-apim-rest-api-kafka-explorer';

  public static create(dynamicConfig: Config, environment: CircleCIEnvironment) {
    return super.create(
      dynamicConfig,
      environment,
      'job-test-kafka-explorer',
      [
        new commands.Run({
          name: `Run Kafka Explorer tests`,
          command: `mvn --fail-fast -s ${config.maven.settingsFile} test --no-transfer-progress -Drest-api-modules -pl ${TestKafkaExplorerJob.module} -Dskip.validation=true -Dgravitee.archrules.skip=true -nsu`,
        }),
      ],
      UbuntuExecutor.create('large'),
      [],
    );
  }
}
