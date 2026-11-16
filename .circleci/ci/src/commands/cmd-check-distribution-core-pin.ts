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
import { ReusableCommand, commands, reusable } from '../circleci-config';
import { config } from '../config';
import { CircleCIEnvironment } from '../pipelines';
import { parse } from '../utils';

/**
 * Refuses a release whose distribution pins a core it cannot assemble: a SNAPSHOT, or a core from
 * another line than the tag. Every release job that builds or publishes from the distribution runs it
 * first, so none of them gets ahead of a refusal; the feed keeps a published version for good.
 *
 * What replaces `Check both reactors carry the same version`. A SNAPSHOT is mutable, so a
 * distribution assembled on one is not reproducible: the same tag rebuilt tomorrow would carry a
 * different core. Read through Maven rather than parsed out of the pom — a parsing slip here would
 * let a release through silently, which is the failure this exists to stop. Reading the pin needs no
 * reactor and no installed artifact: the pom parents to the organisation pom with <relativePath/>,
 * and the `versions:set -DremoveSnapshot` of the release only touches the project version, never this
 * property. It does need the Azure Artifacts token.
 */
export class CheckDistributionCorePinCommand {
  private static commandName = 'cmd-check-distribution-core-pin';

  public static get(environment: CircleCIEnvironment): ReusableCommand {
    // Which line this release belongs to, computed here rather than in shell: the version is parsed
    // for the artefact paths anyway, and one parser is enough.
    const { version: releasedVersion } = parse(environment.graviteeioVersion);
    const releasedLine = `${releasedVersion.major}.${releasedVersion.minor}`;

    return new reusable.ReusableCommand(
      CheckDistributionCorePinCommand.commandName,
      [
        new commands.Run({
          name: 'Refuse a core pin this release cannot assemble',
          command: `PIN=$(mvn --settings ${config.maven.settingsFile} -q -N -f gravitee-apim-distribution/pom.xml help:evaluate -Dexpression=apim.core.version -DforceStdout)
echo "Releasing ${environment.graviteeioVersion}, which pins core $PIN"

case "$PIN" in
*-SNAPSHOT)
  echo
  echo "A release cannot assemble a SNAPSHOT core: it is mutable, so this tag would not rebuild to"
  echo "the same artefacts. Release the core first, then merge the pull request that pins it."
  exit 1
  ;;
esac

# The pin may trail the release by a few patches — it moves only when someone decides a core is
# ready — but never by a minor. A pin from another line is a hand-edit, or a branch whose code
# freeze never moved it off the previous line, and either ships a core nobody meant to ship.
PIN_BASE=\${PIN%%-*}
if [ "\${PIN_BASE%.*}" != "${releasedLine}" ]; then
  echo
  echo "core $PIN is not on the ${releasedLine} line, which ${environment.graviteeioVersion} releases."
  echo "Release a ${releasedLine} core and merge the pull request that pins it, or fix the pin by hand."
  exit 1
fi`,
        }),
      ],
      undefined,
      'Refuse a distribution core pin this release cannot assemble',
    );
  }
}
