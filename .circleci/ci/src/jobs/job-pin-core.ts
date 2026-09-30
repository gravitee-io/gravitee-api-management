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
import { parse } from '../utils';
import { CircleCIEnvironment } from '../pipelines';

/**
 * Advances the distribution's pin onto the core just published.
 *
 * Releasing the core is what decides it ships, so the pin lands as a commit on the support branch
 * rather than as a pull request someone has to merge. The commit carries `[skip ci]`: it moves one
 * property, and what builds against the new pin is the branch's own work — its nightly, its pull
 * requests, and the distribution release that ships it.
 *
 * There is no dry run here. This lane is reachable only from a pushed tag, and a tag forces
 * `isDryRun` off — a tag is never a rehearsal. `prepare_core_release --dry-run` therefore rehearses
 * the commit and the tag, and stops short of this.
 */
export class PinCoreJob {
  private static jobName = 'job-pin-core';

  public static create(dynamicConfig: Config, environment: CircleCIEnvironment): Job {
    dynamicConfig.importOrb(orbs.keeper);

    const version = environment.graviteeioVersion;
    const parsed = parse(version);
    // The lane runs on a tag, where CIRCLE_BRANCH is empty, so the branch is derived from the
    // version the way every release command already derives it: 4.13.0-alpha.1 comes from 4.13.x.
    const branch = `${parsed.version.major}.${parsed.version.minor}.x`;

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
        name: `Pin core ${version} on ${branch}`,
        command: `# The lane checked out the tag, and the branch has moved on since — it carries the commit
# reopening the next development version. The pin has to be advanced there, not on the tag.
# --no-track: a remote starting point sets an upstream on its own, and the branch is pushed
# explicitly below.
git fetch origin ${branch}
git checkout -B ${branch} --no-track origin/${branch}

sed -i "s#<apim.core.version>.*</apim.core.version>#<apim.core.version>${version}</apim.core.version>#" gravitee-apim-distribution/pom.xml

# The sed above no-ops if the property line ever changes shape, and an empty diff would then read
# as "already pinned" and exit 0 on a pin that never moved. Assert the outcome, not the absence of
# a change.
if ! grep -q "<apim.core.version>${version}</apim.core.version>" gravitee-apim-distribution/pom.xml; then
  echo "The pin was not set to ${version}. The property is not the shape this edit expects."
  exit 1
fi

if git diff --quiet; then
  echo "${branch} already pins ${version}, nothing to do."
  exit 0
fi

git add --update
git commit -m 'chore(distribution): pin core ${version} [skip ci]'
# No --force, unlike the pull request branch this replaced: a commit that landed on ${branch} since
# the fetch above fails this push instead of being overwritten by the pin.
git push origin ${branch}`,
      }),
    ];
    return new Job(PinCoreJob.jobName, BaseExecutor.create(), steps);
  }
}
