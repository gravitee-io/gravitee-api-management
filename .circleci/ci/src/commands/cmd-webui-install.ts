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
import { commands, parameters, reusable } from '../circleci-config';

export class WebuiInstallCommand {
  private static commandName = 'cmd-webui-install';
  private static nodeVersionFile = '/tmp/node-version';

  private static customParametersList = new parameters.CustomParametersList([
    new parameters.CustomParameter('apim-ui-project', 'string', '', 'the name of the UI project to build'),
    new parameters.CustomParameter('apim-ui-project-workdir', 'string', '', 'the directory of the UI project to build'),
  ]);

  public static get(): reusable.ReusableCommand {
    return new reusable.ReusableCommand(
      WebuiInstallCommand.commandName,
      [
        // Jobs on different images share this cache, so the key carries the Node they run: the native
        // addons built at install time depend on it.
        new commands.Run({
          name: 'Record the Node version for the Yarn cache key',
          command: `node --version > ${WebuiInstallCommand.nodeVersionFile}`,
        }),
        new commands.cache.Restore({
          name: 'Restore Yarn cache',
          keys: [
            `<< parameters.apim-ui-project >>-cache-v1-{{ checksum "${WebuiInstallCommand.nodeVersionFile}" }}-{{ .Branch }}-{{ checksum "<< parameters.apim-ui-project-workdir >>/yarn.lock" }}`,
            `<< parameters.apim-ui-project >>-cache-v1-{{ checksum "${WebuiInstallCommand.nodeVersionFile}" }}-{{ .Branch }}`,
          ],
        }),
        new commands.Run({
          name: 'Install dependencies',
          command: 'yarn',
          working_directory: '<< parameters.apim-ui-project-workdir >>',
        }),
        new commands.cache.Save({
          name: 'Save Yarn cache',
          key: `<< parameters.apim-ui-project >>-cache-v1-{{ checksum "${WebuiInstallCommand.nodeVersionFile}" }}-{{ .Branch }}-{{ checksum "<< parameters.apim-ui-project-workdir >>/yarn.lock" }}`,
          // node_modules alone is not enough: without install-state.gz yarn takes every package as
          // never built and runs all their install scripts again.
          paths: [
            './<< parameters.apim-ui-project-workdir >>/node_modules',
            './<< parameters.apim-ui-project-workdir >>/.yarn/install-state.gz',
          ],
        }),
      ],
      WebuiInstallCommand.customParametersList,
    );
  }
}
