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
import { commands, Config, Job, reusable } from '@circleci/circleci-config-sdk';
import { OpenJdkExecutor } from '../../executors';
import { NotifyOnFailureCommand, RestoreMavenJobCacheCommand, SaveMavenJobCacheCommand } from '../../commands';
import { Command } from '@circleci/circleci-config-sdk/dist/src/lib/Components/Commands/exports/Command';
import { config } from '../../config';
import { CircleCIEnvironment } from '../../pipelines';

export class BuildBackendJob {
  public static create(dynamicConfig: Config, environment: CircleCIEnvironment): Job {
    const jobName = 'job-build';

    const restoreMavenJobCacheCmd = RestoreMavenJobCacheCommand.get(environment);
    const saveMavenJobCacheCmd = SaveMavenJobCacheCommand.get();
    const notifyOnFailureCmd = NotifyOnFailureCommand.get(dynamicConfig);
    dynamicConfig.addReusableCommand(restoreMavenJobCacheCmd);
    dynamicConfig.addReusableCommand(saveMavenJobCacheCmd);
    dynamicConfig.addReusableCommand(notifyOnFailureCmd);

    const steps: Command[] = [
      new commands.Checkout(),
      new commands.workspace.Attach({ at: '.' }),
      new reusable.ReusedCommand(restoreMavenJobCacheCmd, { jobName: jobName }),
      new commands.Run({
        name: 'Build project',
        command: `mvn -s ${config.maven.settingsFile} clean install --no-transfer-progress --update-snapshots -DskipTests -Dskip.validation=true -T 2C -Dbundle=dev
mkdir -p ./rest-api-docker-context/distribution && cp -r ./gravitee-apim-rest-api/gravitee-apim-rest-api-standalone/gravitee-apim-rest-api-standalone-distribution/target/distribution ./rest-api-docker-context/.
mkdir -p ./gateway-docker-context/distribution && cp -r ./gravitee-apim-gateway/gravitee-apim-gateway-standalone/gravitee-apim-gateway-standalone-distribution/target/distribution ./gateway-docker-context/.`,
        environment: {
          BUILD_ID: environment.buildId,
          BUILD_NUMBER: environment.buildNum,
          GIT_COMMIT: environment.sha1,
        },
      }),
      new reusable.ReusedCommand(notifyOnFailureCmd),
      new commands.cache.Save({
        paths: ['~/.m2/repository/io/gravitee/apim'],
        key: `${config.cache.prefix}-build-apim-{{ .Environment.CIRCLE_WORKFLOW_WORKSPACE_ID }}`,
        when: 'on_success',
      }),
      new reusable.ReusedCommand(saveMavenJobCacheCmd, { jobName: jobName }),
      new commands.workspace.Persist({
        root: './',
        paths: [
          './gravitee-apim-rest-api/gravitee-apim-rest-api-management/gravitee-apim-rest-api-management-rest/target/classes/console-openapi.*',
          './rest-api-docker-context',
          './gateway-docker-context',
        ],
      }),
    ];
    return new Job(jobName, OpenJdkExecutor.create('large'), steps);
  }
}
