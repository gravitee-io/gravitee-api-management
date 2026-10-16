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
import { OpenJdkExecutor } from '../../executors';
import { AbstractTestJob } from './abstract-job-test';
import { CircleCIEnvironment } from '../../pipelines';
import { mavenParallelism } from '../../utils';

export const GATEWAY_INTEGRATION_TESTS_MODULE = 'gravitee-apim-gateway/gravitee-apim-gateway-integration-tests';

/**
 * Integration tests that need nothing but the engine, the tests SDK and the plugins of this repository.
 *
 * They live in the core reactor, so a change and the test that proves it are built together. The ones that
 * exercise a plugin released from another repository stay in the distribution (see TestIntegrationJob).
 */
export class TestGatewayIntegrationJob extends AbstractTestJob {
  public static create(dynamicConfig: Config, environment: CircleCIEnvironment) {
    return super.create(
      dynamicConfig,
      environment,
      'job-test-gateway-integration',
      [
        new commands.Run({
          name: 'Run gateway integration tests',
          // -nsu: the engine and the SDK come from the build workspace; a remote timestamped snapshot must not replace them.
          command: `mvn --fail-fast -s ${config.maven.settingsFile} test --no-transfer-progress -nsu -pl ${GATEWAY_INTEGRATION_TESTS_MODULE} -Dskip.validation=true -Dgravitee.archrules.skip=true -Dsurefire.rerunFailingTestsCount=3 -Dsurefire.exitTimeout=300 ${mavenParallelism('large')}`,
        }),
      ],
      OpenJdkExecutor.create('large'),
      [],
    );
  }
}
