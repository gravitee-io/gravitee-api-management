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
import { OpenJdkExecutor } from '../executors';
import { AzureArtifactsTokenCommand } from '../commands';

export class SetupJob {
  public static create(dynamicConfig: Config): Job {
    dynamicConfig.importOrb(orbs.keeper);

    const azureArtifactsTokenCmd = AzureArtifactsTokenCommand.get(dynamicConfig);
    dynamicConfig.addReusableCommand(azureArtifactsTokenCmd);

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
      new reusable.ReusedCommand(azureArtifactsTokenCmd),
      new commands.Run({
        name: 'Check Maven resolves from the feed',
        // Once per pipeline, on the settings this job just wrote, rather than in every job that
        // runs Maven: the wiring is the same for all of them, since they all attach this one file.
        //
        // dependency:get runs project-less, so its remote repositories come from the active
        // profile in the settings — the part a raw HTTPS request never exercises: the <server> id
        // matching the repository id, and ${env.AZURE_ARTIFACTS_PAT} actually being interpolated.
        // A settings with a mismatched server id passes a curl check and falls back to Artifactory
        // for good.
        //
        // The plugin GAV is pinned: an unpinned one resolves maven-metadata.xml first, a round
        // trip that proves nothing.
        //
        // Scaffolding, to be removed with Artifactory. This check and the HTTPS one in
        // cmd-azure-artifacts-token both exist because the settings declare Artifactory behind
        // the feed, so a feed that answers 401 leaves the build green. Once Artifactory is off
        // there is no fallback left to hide behind, a broken feed fails on its own, and the two
        // checks — along with io.gravitee.canary:feed-canary itself — can go.
        command: `if ! mvn -B -q -s ${config.maven.settingsFile} \\
  org.apache.maven.plugins:maven-dependency-plugin:${config.maven.dependencyPluginVersion}:get \\
  -Dartifact=io.gravitee.canary:feed-canary:1.0.0:pom; then
  echo "Maven could not resolve the canary from the feed." >&2
  echo "The token itself was accepted over HTTPS a step earlier, so what is left is the" >&2
  echo "settings wiring: a <server> id that does not match the repository id, or" >&2
  # Single quotes: the shell would try to expand this one, and a dot is not a valid
  # variable name — the message would come out as a bad substitution instead.
  echo '\${env.AZURE_ARTIFACTS_PAT} not being interpolated.' >&2
  exit 1
fi

echo "Maven resolved io.gravitee.canary:feed-canary from the feed."`,
      }),
      new commands.workspace.Persist({
        root: '.',
        paths: [config.maven.settingsFile],
      }),
    ];
    return new Job('job-setup', OpenJdkExecutor.create('small'), steps);
  }
}
