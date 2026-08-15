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
import { mavenParallelism } from '../../utils';

export class TestRestApiJob extends AbstractTestJob {
  public static create(dynamicConfig: Config, environment: CircleCIEnvironment) {
    return super.create(
      dynamicConfig,
      environment,
      'job-test-rest-api',
      [
        new commands.Run({
          // Goals, not a lifecycle phase. `Build backend` has already compiled these modules and
          // generated their models, and the workspace carries the result: running `test` here would
          // rewrite the generated sources and recompile all of it for nothing.
          //
          // surefire needs two of them first — dependency:properties resolves the mockito agent path
          // and jacoco:prepare-agent fills argLine — or it hands `@{argLine}` to the JVM verbatim.
          //
          // standalone-container depends on two Gamma artifacts this reactor no longer builds, so they
          // come from the local repository. -nsu keeps a published snapshot from taking the place of the
          // one `Build backend` just installed.
          //
          // -pl '!.' drops the aggregator from the reactor. It is the one pom with no gravitee-apim-parent
          // above it, so the jacoco prefix does not resolve there and Maven would either fail outright or
          // reach for the latest release instead of the version the build pins.
          name: `Run Rest API tests`,
          command: `mvn --fail-fast -s ${config.maven.settingsFile} dependency:properties jacoco:prepare-agent surefire:test --no-transfer-progress -Drest-api-modules -pl '!.' -nsu ${mavenParallelism('large')}`,
        }),
        new commands.Run({
          // report-aggregate is bound to the test phase, which no longer runs.
          name: `Aggregate coverage`,
          command: `mvn -s ${config.maven.settingsFile} -pl gravitee-apim-rest-api/gravitee-apim-rest-api-coverage org.jacoco:jacoco-maven-plugin:report-aggregate --no-transfer-progress -Drest-api-modules -nsu`,
        }),
      ],
      UbuntuExecutor.create('large'),
      ['gravitee-apim-rest-api/gravitee-apim-rest-api-coverage/target/site/jacoco-aggregate/'],
    );
  }
}
