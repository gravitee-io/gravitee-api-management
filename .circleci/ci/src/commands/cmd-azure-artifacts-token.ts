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
import { Command, Config, ReusableCommand, commands, reusable } from '../circleci-config';
import { orbs } from '../orbs';
import { config } from '../config';

/**
 * Exposes an Azure Artifacts token in AZURE_ARTIFACTS_PAT, which the Maven settings read as
 * ${env.AZURE_ARTIFACTS_PAT}.
 *
 * Gravitee.io Bot is an Entra service principal, and a service principal cannot hold a personal
 * access token: it authenticates with a token obtained from its client secret, valid for one hour.
 *
 * Every job running Maven therefore asks for its own, rather than one being fetched once and
 * carried through the workspace. A "rerun from failed" does not replay the job that fetched it —
 * it reuses the workspace, and would start again with a dead token. Same for a workflow left
 * waiting on a manual approval. Neither is exotic, and the symptom would be a 401 pointing at
 * permissions when the token has merely expired.
 *
 * The credentials are the ones already used to push images to the container registry.
 */
export class AzureArtifactsTokenCommand {
  private static commandName = 'cmd-azure-artifacts-token';

  public static get(dynamicConfig: Config): ReusableCommand {
    dynamicConfig.importOrb(orbs.keeper);

    const steps: Command[] = [
      new reusable.ReusedCommand(orbs.keeper.commands['env-export'], {
        'secret-url': config.secrets.azureApplicationId,
        'var-name': 'AZURE_SP_CLIENT_ID',
      }),
      new reusable.ReusedCommand(orbs.keeper.commands['env-export'], {
        'secret-url': config.secrets.azureApplicationSecret,
        'var-name': 'AZURE_SP_CLIENT_SECRET',
      }),
      new reusable.ReusedCommand(orbs.keeper.commands['env-export'], {
        'secret-url': config.secrets.azureTenant,
        'var-name': 'AZURE_SP_TENANT',
      }),
      new commands.Run({
        name: 'Get an Azure Artifacts token',
        // 499b84ac-1321-427f-aa17-267ca6975798 is the Azure DevOps application ID; the token is
        // requested for that audience, not for the feed URL.
        command: `TOKEN=$(curl -sf -X POST \\
  "https://login.microsoftonline.com/\${AZURE_SP_TENANT}/oauth2/v2.0/token" \\
  -d "client_id=\${AZURE_SP_CLIENT_ID}" \\
  -d "client_secret=\${AZURE_SP_CLIENT_SECRET}" \\
  -d "scope=499b84ac-1321-427f-aa17-267ca6975798/.default" \\
  -d "grant_type=client_credentials" \\
  | sed -n 's/.*"access_token":"\\([^"]*\\)".*/\\1/p')

if [ -z "$TOKEN" ]; then
  echo "Could not obtain an Entra token for the service principal." >&2
  echo "The client secret is the one used to push images: if it expired, those are failing too." >&2
  exit 1
fi

# Through BASH_ENV so the value survives to the following steps, and never appears on a
# command line or in a process listing.
echo "export AZURE_ARTIFACTS_PAT='\${TOKEN}'" >> "$BASH_ENV"`,
      }),
      new commands.Run({
        name: 'Check the feed answers',
        // Without this, a feed that rejects the token is invisible: the settings declare
        // Artifactory after the feed, so Maven falls back to it with a warning and the build
        // stays green. The fallback is there for artifacts not yet on the feed, not to paper
        // over an authentication failure.
        //
        // io.gravitee.canary:feed-canary:1.0.0 exists on the feed and nowhere else, so
        // resolving it proves the feed answered, and answered to this token.
        command: `CODE=$(curl -s -o /dev/null -w '%{http_code}' -m 30 \\
  -u "bot:\${AZURE_ARTIFACTS_PAT}" \\
  "https://pkgs.dev.azure.com/graviteeio/packages/_packaging/gravitee/maven/v1/io/gravitee/canary/feed-canary/1.0.0/feed-canary-1.0.0.pom")

case "$CODE" in
  200) echo "The feed resolves the canary." ;;
  401) echo "401 — the feed rejected the token." >&2; exit 1 ;;
  403) echo "403 — Gravitee.io Bot has no role on the feed." >&2; exit 1 ;;
  404) echo "404 — token accepted but the canary is gone; publish it again." >&2; exit 1 ;;
  *)   echo "HTTP $CODE from the feed." >&2; exit 1 ;;
esac`,
      }),
    ];

    return new reusable.ReusableCommand(
      AzureArtifactsTokenCommand.commandName,
      steps,
      undefined,
      'Get a short-lived Azure Artifacts token',
    );
  }
}
