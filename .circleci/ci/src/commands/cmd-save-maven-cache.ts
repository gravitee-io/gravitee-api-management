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
import { commands, parameters, reusable } from '@circleci/circleci-config-sdk';
import { config } from '../config';

export class SaveMavenJobCacheCommand {
  private static commandName = 'cmd-save-maven-job-cache';

  private static customParametersList = new parameters.CustomParametersList([
    new parameters.CustomParameter('jobName', 'string', '', 'The job name'),
  ]);

  public static get(): reusable.ReusableCommand {
    return new reusable.ReusableCommand(
      SaveMavenJobCacheCommand.commandName,
      [
        new commands.Run({
          name: 'Exclude all APIM artefacts from cache.',
          command: 'rm -rf ~/.m2/repository/io/gravitee/apim',
        }),
        new commands.cache.Save({
          key: `${config.cache.prefix}-<< parameters.jobName >>-{{ .Branch }}-{{ checksum "pom.xml" }}`,
          paths: ['~/.m2'],
          when: 'always',
        }),
      ],
      SaveMavenJobCacheCommand.customParametersList,
      'Save Maven cache for a dedicated job',
    );
  }
}
