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
import { commands, Config, parameters } from '@circleci/circleci-config-sdk';
import { config } from '../../config';
import { AbstractTestContainerJob } from './abstract-job-test-container';
import { CircleCIEnvironment } from '../../pipelines';

export class RedisTestContainerJob extends AbstractTestContainerJob {
  public static create(dynamicConfig: Config, environment: CircleCIEnvironment) {
    return super.create(
      dynamicConfig,
      environment,
      'job-redis-test-container',
      new parameters.CustomParametersList([new parameters.CustomParameter('redisVersion', 'string', '', 'Version of Redis to test')]),
      new commands.Run({
        name: 'Run Rate-limit repository tests with Redis << parameters.redisVersion >>',
        command: `cd gravitee-apim-repository
mvn -pl 'gravitee-apim-repository-redis' -am -s ../${config.maven.settingsFile} clean package --no-transfer-progress -Dskip.validation=true -DredisVersion=<< parameters.redisVersion >>`,
      }),
    );
  }
}
