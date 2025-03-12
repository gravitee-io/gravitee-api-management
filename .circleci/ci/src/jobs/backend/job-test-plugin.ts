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
import { commands, Config } from '@circleci/circleci-config-sdk';
import { config } from '../../config';
import { AbstractTestJob } from './abstract-job-test';
import { UbuntuExecutor } from '../../executors';
import { CircleCIEnvironment } from '../../pipelines';

export class TestPluginJob extends AbstractTestJob {
  public static create(dynamicConfig: Config, environment: CircleCIEnvironment) {
    return super.create(
      dynamicConfig,
      environment,
      'job-test-plugin',
      new commands.Run({
        name: `Run plugin tests`,
        command: `mvn --fail-fast -s ${config.maven.settingsFile} test --no-transfer-progress -Dplugin-modules -Dskip.validation=true -T 2C`,
      }),
      UbuntuExecutor.create(),
      ['gravitee-apim-plugin/gravitee-apim-plugin-coverage/target/site/jacoco-aggregate/'],
    );
  }
}
