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
import { commands, Config, Job, parameters, reusable } from '@circleci/circleci-config-sdk';
import { SonarScannerExecutor } from '../executors';
import { NotifyOnFailureCommand, RestoreMavenJobCacheCommand, SaveMavenJobCacheCommand } from '../commands';
import { Command } from '@circleci/circleci-config-sdk/dist/src/lib/Components/Commands/exports/Command';
import { config } from '../config';
import { orbs } from '../orbs';
import { computeApimVersion } from '../utils';
import { CircleCIEnvironment } from '../pipelines';

export class SonarCloudAnalysisJob {
  private static jobName = 'job-sonarcloud-analysis';

  private static customParametersList = new parameters.CustomParametersList([
    new parameters.CustomParameter(
      'working_directory',
      'string',
      'gravitee-apim-rest-api',
      'Directory where the Sonarcloud analysis will be run',
    ),
  ]);

  public static create(dynamicConfig: Config, environment: CircleCIEnvironment): Job {
    return new reusable.ParameterizedJob(
      SonarCloudAnalysisJob.jobName,
      SonarScannerExecutor.create('large'),
      SonarCloudAnalysisJob.customParametersList,
      this.getSonarSteps(dynamicConfig, environment, '<< parameters.working_directory >>'),
    );
  }

  public static getSonarSteps(dynamicConfig: Config, environment: CircleCIEnvironment, workingDirectory: string = '.') {
    const apimVersion = computeApimVersion(environment);
    dynamicConfig.importOrb(orbs.keeper);

    const restoreMavenJobCacheCmd = RestoreMavenJobCacheCommand.get();
    const saveMavenJobCacheCmd = SaveMavenJobCacheCommand.get();
    const notifyOnFailureCmd = NotifyOnFailureCommand.get(dynamicConfig);
    dynamicConfig.addReusableCommand(restoreMavenJobCacheCmd);
    dynamicConfig.addReusableCommand(saveMavenJobCacheCmd);
    dynamicConfig.addReusableCommand(notifyOnFailureCmd);

    const steps: Command[] = [
      new commands.Run({
        name: 'Add SSH tool',
        command: 'apk add --no-cache openssh',
      }),
      new commands.Checkout(),
      new commands.workspace.Attach({ at: '.' }),
      new commands.cache.Restore({
        keys: [
          `${config.cache.prefix}-sonarcloud-analysis-<< parameters.working_directory >>-{{ .Branch }}-{{ checksum "pom.xml" }}`,
          `${config.cache.prefix}-sonarcloud-analysis-<< parameters.working_directory >>-{{ .Branch }}`,
          `${config.cache.prefix}-sonarcloud-analysis-<< parameters.working_directory >>`,
        ],
      }),
      new reusable.ReusedCommand(orbs.keeper.commands['env-export'], {
        'secret-url': config.secrets.sonarToken,
        'var-name': 'SONAR_TOKEN',
      }),
      new commands.Run({
        name: 'Run Sonarcloud Analysis',
        command: `sonar-scanner -Dsonar.projectVersion=${apimVersion}`,
        working_directory: workingDirectory,
      }),
      new reusable.ReusedCommand(notifyOnFailureCmd),
      new commands.cache.Save({
        paths: ['/opt/sonar-scanner/.sonar/cache'],
        key: `${config.cache.prefix}-sonarcloud-analysis-<< parameters.working_directory >>-{{ .Branch }}-{{ checksum "pom.xml" }}`,
        when: 'always',
      }),
    ];

    return steps;
  }
}
