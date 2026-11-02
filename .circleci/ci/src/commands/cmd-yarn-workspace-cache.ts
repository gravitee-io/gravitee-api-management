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
import { commands, reusable } from '../circleci-config';
import { config } from '../config';
import { CircleCIEnvironment } from '../pipelines';

// For the Maven builds that run the Gamma UI's `yarn install` (gravitee-gamma, generate-resources).
// Shared by every job on the OpenJDK image, so a workflow's first one fills it for the others; the
// image version is in the key because the native addons built at install time depend on its Node.
// Not the key of the UI build jobs, which run on another image.
const keyPrefix = `${config.cache.prefix}-yarn-workspace-openjdk-${config.executor.openjdk.version}`;
const key = `${keyPrefix}-{{ .Branch }}-{{ checksum "yarn.lock" }}`;

export class RestoreYarnWorkspaceCacheCommand {
  private static commandName = 'cmd-restore-yarn-workspace-cache';

  public static get(environment: CircleCIEnvironment): reusable.ReusableCommand {
    const keys = [key, `${keyPrefix}-{{ .Branch }}`];

    // Same fallback as the Maven cache: a new branch starts from its base branch's install.
    const baseBranch = environment.baseBranch.replace(/^origin\//, '');
    if (baseBranch !== environment.branch) {
      keys.push(`${keyPrefix}-${baseBranch}-`);
    }

    return new reusable.ReusableCommand(RestoreYarnWorkspaceCacheCommand.commandName, [
      new commands.cache.Restore({ name: 'Restore Yarn workspace cache', keys }),
    ]);
  }
}

export class SaveYarnWorkspaceCacheCommand {
  private static commandName = 'cmd-save-yarn-workspace-cache';

  public static get(): reusable.ReusableCommand {
    return new reusable.ReusableCommand(SaveYarnWorkspaceCacheCommand.commandName, [
      new commands.cache.Save({
        name: 'Save Yarn workspace cache',
        key,
        // node_modules alone is not enough: without install-state.gz yarn takes every package as
        // never built and runs all their install scripts again.
        paths: ['./node_modules', './.yarn/install-state.gz'],
      }),
    ]);
  }
}
