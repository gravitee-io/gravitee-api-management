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
 * Opens the pull request that advances the distribution's pin onto the core just published.
 *
 * The pin never moves on its own: merging this is the moment someone decides a core is ready to
 * ship. Leaving it unmerged costs nothing — the branch stays releasable on the core it already
 * pins.
 *
 * There is no dry run here. This lane is reachable only from a pushed tag, and a tag forces
 * `isDryRun` off — a tag is never a rehearsal. `prepare_core_release --dry-run` therefore rehearses
 * the commit and the tag, and stops short of this.
 */
export class PinCoreJob {
  private static jobName = 'job-pin-core';

  public static create(dynamicConfig: Config, environment: CircleCIEnvironment): Job {
    dynamicConfig.importOrb(orbs.keeper);
    dynamicConfig.importOrb(orbs.github);

    const version = environment.graviteeioVersion;
    const parsed = parse(version);
    // The lane runs on a tag, where CIRCLE_BRANCH is empty, so the branch is derived from the
    // version the way every release command already derives it: 4.13.0-alpha.1 comes from 4.13.x.
    const branch = `${parsed.version.major}.${parsed.version.minor}.x`;
    // Named after the branch it targets, never after the version: it is the same pull request being
    // brought up to date, the way Renovate keeps one branch per dependency. Two support branches
    // releasing at once would otherwise fight over one name.
    const pinBranch = `chore/pin-core-${branch}`;

    const steps: Command[] = [
      new commands.Checkout(),
      new reusable.ReusedCommand(orbs.keeper.commands['env-export'], {
        'secret-url': config.secrets.githubApiToken,
        'var-name': 'GITHUB_TOKEN',
      }),
      new reusable.ReusedCommand(orbs.github.commands['setup'], { version: config.githubCli.version }),
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
git config --global user.email "\${GIT_USER_EMAIL}"
gh auth setup-git
git remote set-url origin "https://github.com/\${CIRCLE_PROJECT_USERNAME}/\${CIRCLE_PROJECT_REPONAME}.git"`,
      }),
      new commands.Run({
        name: `Pin core ${version} on ${branch}`,
        command: `# The lane checked out the tag, and the branch has moved on since — it carries the commit
# reopening the next development version. The pin has to be advanced there, not on the tag.
# --no-track: a remote starting point sets an upstream on its own, and this branch is pushed
# explicitly below.
git fetch origin ${branch}
git checkout -B ${pinBranch} --no-track origin/${branch}

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
git commit -m "chore(distribution): pin core ${version}"
# Rebuilt from the base branch every time, so the push replaces whatever was there. A release that
# lands before the previous pin was merged updates that pull request instead of opening a rival —
# only the newest core is worth reviewing. Anything pushed onto this branch by hand goes with it.
git push --force origin ${pinBranch}

TITLE="chore(distribution): pin core ${version}"
BODY="Core ${version} has been published. Merging this makes the distribution assemble it.

Its integration tests run against the pinned core, so a green build here is the evidence that this core is ready to ship. Until it is merged, ${branch} keeps releasing on the core it already pins.

This branch is rebuilt on every core release: a newer core replaces this one rather than opening a second pull request."

if [ "$(gh pr list --head ${pinBranch} --state open --json number --jq 'length')" = "0" ]; then
  gh pr create --base ${branch} --head ${pinBranch} --title "$TITLE" --body "$BODY"
else
  echo "A pin pull request is already open on ${pinBranch}, bringing it up to core ${version}."
  gh pr edit ${pinBranch} --title "$TITLE" --body "$BODY"
fi`,
      }),
    ];
    return new Job(PinCoreJob.jobName, BaseExecutor.create(), steps);
  }
}
