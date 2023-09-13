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
import { OpenJdkExecutor } from '../../executors';
import { AbstractTestJob } from './abstract-job-test';

export class TestRestApiJob extends AbstractTestJob {
  public static create(dynamicConfig: Config) {
    return super.create(
      dynamicConfig,
      'job-test-rest-api',
      new commands.Run({
        name: 'Run tests',
        command: `cd gravitee-apim-rest-api
# List all tests
circleci tests glob "**/src/test/java/**/*Test.java" | sed -e 's#^.*/src/test/java/\\(.*\\)\\.java#\\1#' | tr "/" "." > all-tests

# List all tests to run on this executor
cat all-tests | circleci tests split --split-by=timings --timings-type=classname --time-default=10s > tests-to-run

# Compute exclusion list (use grep to invert the include list to an exclude list)
cat all-tests | grep -xvf tests-to-run > /tmp/ignore_list

# Add * add the end of each line of ignore_list to also exclude all inner classes
sed -i 's/$/*/' /tmp/ignore_list

# Display tests to run on this executor
echo "Following test files will run on this executor:"
cat tests-to-run

# Run tests
mvn --fail-fast -s ../${config.maven.settingsFile} test --no-transfer-progress -Dskip.validation=true -Dsurefire.excludesFile=/tmp/ignore_list`,
      }),
      OpenJdkExecutor.create('small'),
      ['gravitee-apim-rest-api/gravitee-apim-rest-api-coverage/target/site/jacoco-aggregate/'],
      {
        parallelism: 4,
      },
    );
  }
}
