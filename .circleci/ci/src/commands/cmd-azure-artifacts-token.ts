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
 * The credentials are the service principal's own, the ones job-deploy-on-azure signs in with
 * to restart the AKS pods. They are not the container registry's — that one has its own login.
 */
export class AzureArtifactsTokenCommand {
  private static commandName = 'cmd-azure-artifacts-token';

  public static get(dynamicConfig: Config): ReusableCommand {
    dynamicConfig.importOrb(orbs.keeper);

    const steps: Command[] = [
      new reusable.ReusedCommand(orbs.keeper.commands['install']),
      new commands.Run({
        name: 'Get an Azure Artifacts token',
        // The credentials are read into shell variables rather than exported: they stay inside
        // this step instead of living in the environment of every step that follows.
        //
        // 499b84ac-1321-427f-aa17-267ca6975798 is the application ID of Azure DevOps as a whole —
        // Artifacts has none of its own, it is one of its services. A client_credentials flow can
        // only ask for .default anyway; what the token may do comes from the roles granted to the
        // service principal on the feed, not from the scope.
        command: `CLIENT_ID=$(ksm secret notation ${config.secrets.azureApplicationId})
CLIENT_SECRET=$(ksm secret notation ${config.secrets.azureApplicationSecret})
TENANT=$(ksm secret notation ${config.secrets.azureTenant})

# --data-urlencode rather than -d, which sends its argument verbatim: a client secret holding
# a +, & or = is mangled by the form decoder on the other side. Entra secrets routinely do.
#
# The status is captured instead of being left to --fail. Steps run under \`bash -eo pipefail\`,
# where a failing command substitution aborts the step on the spot — the diagnostics below would
# never run, and the job would die without printing anything at all.
RESPONSE=$(curl -sS --retry 3 --retry-all-errors --retry-delay 5 --max-time 30 \\
  -w '\\n%{http_code}' -X POST \\
  "https://login.microsoftonline.com/\${TENANT}/oauth2/v2.0/token" \\
  --data-urlencode "client_id=\${CLIENT_ID}" \\
  --data-urlencode "client_secret=\${CLIENT_SECRET}" \\
  --data-urlencode "scope=499b84ac-1321-427f-aa17-267ca6975798/.default" \\
  --data-urlencode "grant_type=client_credentials") || RESPONSE=""

CODE=$(printf '%s' "$RESPONSE" | tail -1)
BODY=$(printf '%s' "$RESPONSE" | sed '$d')

if [ "$CODE" != "200" ]; then
  echo "Entra refused to issue a token (HTTP \${CODE:-no answer})." >&2
  printf '%s' "$BODY" | sed -n 's/.*"error_description":"\\([^"]*\\)".*/  \\1/p' >&2
  echo "Same secret as job-deploy-on-azure: if it expired, that job is failing too." >&2
  exit 1
fi

TOKEN=$(printf '%s' "$BODY" | sed -n 's/.*"access_token":"\\([^"]*\\)".*/\\1/p')
if [ -z "$TOKEN" ]; then
  echo "Entra answered 200 with no access_token in the body." >&2
  exit 1
fi

# The one value that has to reach the next steps, so it goes through BASH_ENV. It is a JWT —
# base64url and dots, nothing the shell can expand — and it never came from Keeper.
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
        //
        // Scaffolding, to be removed with Artifactory: see the note on job-setup's Maven check.
        command: `CODE=$(curl -s -o /dev/null -w '%{http_code}' --retry 3 --retry-all-errors --retry-delay 5 --max-time 30 \\
  -u "bot:\${AZURE_ARTIFACTS_PAT}" \\
  "https://pkgs.dev.azure.com/graviteeio/packages/_packaging/gravitee/maven/v1/io/gravitee/canary/feed-canary/1.0.0/feed-canary-1.0.0.pom") || CODE=000

case "$CODE" in
  200) echo "The feed resolves the canary." ;;
  401) echo "401 — the feed rejected the token." >&2; exit 1 ;;
  403) echo "403 — Gravitee.io Bot has no role on the feed." >&2; exit 1 ;;
  404) echo "404 — token accepted but the canary is gone; publish it again." >&2; exit 1 ;;
  # Everything else is the feed being unavailable rather than misconfigured, and that is what
  # the Artifactory fallback exists to absorb. Failing here would remove the resilience this
  # design was built around.
  *)   echo "The feed answered HTTP \${CODE}; carrying on, Maven falls back to Artifactory." >&2 ;;
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
