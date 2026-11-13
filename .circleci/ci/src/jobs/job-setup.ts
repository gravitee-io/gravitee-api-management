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
import { Command, Config, Job, commands, reusable } from '../circleci-config';
import { orbs } from '../orbs';
import { config } from '../config';
import { BaseExecutor } from '../executors';

export class SetupJob {
  public static create(dynamicConfig: Config): Job {
    dynamicConfig.importOrb(orbs.keeper);

    const steps: Command[] = [
      new commands.Checkout(),
      new reusable.ReusedCommand(orbs.keeper.commands['install']),
      new commands.Run({
        name: 'Get the Maven settings',
        // Written straight to its file, never through the environment. `env-export` puts the
        // value in $BASH_ENV inside an unquoted heredoc, where the shell expands whatever looks
        // like a variable: a settings.xml holding ${env.SOMETHING} — the syntax Maven uses to
        // read a token — breaks the export and leaves the remaining lines to be run as commands,
        // which prints the file, credentials included, in the job output.
        command: `ksm secret notation ${config.secrets.mavenSettings} > ${config.maven.settingsFile}

# An empty or truncated file would only surface later, in another job, as an unreadable
# settings or a 401 blamed on the credentials.
if ! grep -q '</settings>' ${config.maven.settingsFile}; then
  echo "The Maven settings read from Keeper are empty or truncated." >&2
  exit 1
fi`,
      }),
      new commands.workspace.Persist({
        root: '.',
        paths: [config.maven.settingsFile],
      }),
    ];
    return new Job('job-setup', BaseExecutor.create('small'), steps);
  }
}
