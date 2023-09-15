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

export class WebuiInstallCommand {
  private static commandName = 'cmd-webui-install';

  private static customParametersList = new parameters.CustomParametersList([
    new parameters.CustomParameter('apim-ui-project', 'string', '', 'the name of the UI project to build'),
  ]);

  public static get(): reusable.ReusableCommand {
    return new reusable.ReusableCommand(
      WebuiInstallCommand.commandName,
      [
        new commands.cache.Restore({
          name: 'Restore NPM cache',
          keys: [
            '<< parameters.apim-ui-project >>-cache-v1-{{ .Branch }}-{{ checksum "<< parameters.apim-ui-project >>/package-lock.json" }}',
            '<< parameters.apim-ui-project >>-cache-v1-{{ .Branch }}',
          ],
        }),
        new commands.Run({
          name: 'Install dependencies',
          command: 'npm install',
          working_directory: '<< parameters.apim-ui-project >>',
        }),
        new commands.cache.Save({
          name: 'Save NPM cache',
          key: '<< parameters.apim-ui-project >>-cache-v1-{{ .Branch }}-{{ checksum "<< parameters.apim-ui-project >>/package-lock.json" }}',
          paths: ['./<< parameters.apim-ui-project >>/node_modules'],
        }),
      ],
      WebuiInstallCommand.customParametersList,
    );
  }
}
