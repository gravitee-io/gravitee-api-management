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

export class TestGammaJob extends AbstractTestJob {
  public static create(dynamicConfig: Config, environment: CircleCIEnvironment) {
    return super.create(
      dynamicConfig,
      environment,
      'job-test-gamma',
      [
        new commands.Run({
          // The Gamma modules depend on the rest-api ones, which this reactor does not build: they come
          // from the local repository, restored from the cache that `Build backend` fills. -nsu keeps a
          // published snapshot from taking the place of the one this pipeline just built.
          //
          // -Dskip.ui.build skips the workspace install and the Nx builds. Their output is only read by
          // the plugin assembly, bound to `package`, and this job stops at `test`; `Build backend` runs
          // them for real. The UI is covered by the Gamma UI test job instead.
          name: `Run Gamma tests`,
          command: `mvn --fail-fast -s ${config.maven.settingsFile} test --no-transfer-progress -Dgamma-modules -Dskip.validation=true -Dgravitee.archrules.skip=true -Dskip.ui.build=true -nsu ${mavenParallelism('medium')}`,
        }),
      ],
      OpenJdkExecutor.create('medium'),
      [],
    );
  }
}
