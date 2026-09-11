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
import { BaseExecutor } from '../executors';
import { config } from '../config';
import { orbs } from '../orbs';
import { nextDevelopmentVersion } from '../utils';
import { CircleCIEnvironment } from '../pipelines';

/** The UIs the distribution ships, each carrying the product version in its own `build.json`. */
const WEBUI_BUILD_FILES = [
  'gravitee-apim-console-webui/build.json',
  'gravitee-apim-portal-webui/build.json',
  'gravitee-apim-portal-webui-next/build.json',
  'gravitee-gamma/gravitee-gamma-control-plane-webui/build.json',
];

/**
 * Commits the distribution's release version, tags it, and reopens the branch on the next
 * development version. Publishing is not here: pushing the tag starts the lane that does it.
 *
 * The tag is bare — `4.13.0` — because the distribution carries the version users know. Only the
 * core lane prefixes its tags.
 */
export class PrepareDistributionReleaseJob {
  private static jobName = 'job-prepare-distribution-release';

  public static create(dynamicConfig: Config, environment: CircleCIEnvironment): Job {
    dynamicConfig.importOrb(orbs.keeper);

    const { version: nextVersion, qualifier: nextQualifier } = nextDevelopmentVersion(environment.graviteeioVersion);
    const tag = environment.graviteeioVersion;

    const steps: Command[] = [
      new commands.Checkout(),
      new commands.AddSSHKeys({ fingerprints: config.ssh.fingerprints }),
      new reusable.ReusedCommand(orbs.keeper.commands['env-export'], {
        'secret-url': config.secrets.gitUserName,
        'var-name': 'GIT_USER_NAME',
      }),
      new reusable.ReusedCommand(orbs.keeper.commands['env-export'], {
        'secret-url': config.secrets.gitUserEmail,
        'var-name': 'GIT_USER_EMAIL',
      }),
      new commands.Run({
        name: 'Git config',
        command: `git config --global user.name "\${GIT_USER_NAME}"
git config --global user.email "\${GIT_USER_EMAIL}"`,
      }),
      new commands.Run({
        name: `Tag ${tag} ${environment.isDryRun ? '- Dry Run' : ''}`,
        command: `# The distribution's own pom, never the root's: the core releases under its own tag, and what
# this assembles is the pin, which a reviewed pull request advances.
sed -i "s#<changelist>.*</changelist>#<changelist></changelist>#" gravitee-apim-distribution/pom.xml

${WEBUI_BUILD_FILES.map((file) => `sed -i 's/"version": ".*"/"version": "${tag}"/' ${file}`).join('\n')}

git add --update
git commit -m "${tag}"
git tag ${tag}

sed -i "s#<revision>.*</revision>#<revision>${nextVersion}</revision>#" gravitee-apim-distribution/pom.xml
sed -i "s#<changelist>.*</changelist>#<changelist>-SNAPSHOT</changelist>#" gravitee-apim-distribution/pom.xml
# <sha1 /> is self-closing when the qualifier is empty, which the plain <sha1>.*</sha1> pattern
# never matches — the qualifier was silently kept on any pom already holding the empty form.
sed -i -E "s#<sha1( */>|>[^<]*</sha1>)#<sha1>${nextQualifier}</sha1>#" gravitee-apim-distribution/pom.xml

${WEBUI_BUILD_FILES.map((file) => `sed -i 's#"version": ".*"#"version": "${nextVersion}${nextQualifier}-SNAPSHOT"#' ${file}`).join('\n')}

# The chart ships with the distribution and carries the version to come, not a SNAPSHOT.
${PrepareDistributionReleaseJob.bumpChart(nextVersion, nextQualifier)}

git add --update
git commit -m 'chore: prepare next distribution version [skip ci]'

# The branch first: the reverse order publishes a version that sits on no branch, should the
# branch push then fail.
git push ${environment.isDryRun ? '--dry-run' : ''} origin ${environment.branch}
git push ${environment.isDryRun ? '--dry-run' : ''} origin ${tag}
`,
      }),
    ];
    return new Job(PrepareDistributionReleaseJob.jobName, BaseExecutor.create(), steps);
  }

  /**
   * Only the first line-anchored `version:` and `appVersion:` — both names occur again under every
   * dependency. The `artifacthub.io/changes` annotation is emptied for a final release only: a
   * pre-release accumulates its entries until the release that ships them.
   */
  private static bumpChart(nextVersion: string, nextQualifier: string): string {
    const sed = [
      `-e "0,/^version:/{s/version:.*/version: ${nextVersion}${nextQualifier}/}"`,
      `-e "0,/^appVersion:/{ s/appVersion.*/appVersion: ${nextVersion}${nextQualifier}/ }"`,
    ];
    if (!nextQualifier) {
      sed.push(`-e '/artifacthub.io\\/changes/,\${ s/|// }'`, `-e '/artifacthub.io\\/changes:/q0'`);
    }
    return `sed ${sed.join(' \\\n    ')} \\\n    -i helm/Chart.yaml`;
  }
}
