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

export class RestoreMavenJobCacheCommand {
  private static commandName = 'cmd-restore-maven-job-cache';

  private static customParametersList = new parameters.CustomParametersList([
    new parameters.CustomParameter('jobName', 'string', '', 'The job name'),
  ]);

  public static get(): reusable.ReusableCommand {
    return new reusable.ReusableCommand(
      RestoreMavenJobCacheCommand.commandName,
      [
        new commands.cache.Restore({
          keys: [
            `${config.cache.prefix}-<< parameters.jobName >>-{{ .Branch }}-{{ checksum "pom.xml" }}`,
            `${config.cache.prefix}-<< parameters.jobName >>-{{ .Branch }}`,
          ],
        }),
      ],
      RestoreMavenJobCacheCommand.customParametersList,
      'Restore Maven cache for a dedicated job',
    );
  }
}
