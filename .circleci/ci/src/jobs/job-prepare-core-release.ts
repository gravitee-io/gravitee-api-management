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

/**
 * Commits the core reactor's release version, tags it, and reopens the branch on the next
 * development version. Publishing is not here: pushing the tag starts the lane that does it.
 */
export class PrepareCoreReleaseJob {
  private static jobName = 'job-prepare-core-release';

  public static create(dynamicConfig: Config, environment: CircleCIEnvironment): Job {
    dynamicConfig.importOrb(orbs.keeper);
    dynamicConfig.importOrb(orbs.github);

    const { version: nextVersion, qualifier: nextQualifier } = nextDevelopmentVersion(environment.graviteeioVersion);
    const tag = `core_${environment.graviteeioVersion}`;

    const steps: Command[] = [
      new commands.Checkout(),
      new reusable.ReusedCommand(orbs.keeper.commands['env-export'], {
        'secret-url': config.secrets.githubApiToken,
        'var-name': 'GITHUB_TOKEN',
      }),
      new reusable.ReusedCommand(orbs.github.commands['setup']),
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
        // A push over the project SSH key produces no webhook, so the tag it carries starts nothing.
        name: 'Push over HTTPS, so that pushing the tag triggers the release',
        command: `gh auth setup-git
git remote set-url origin "https://github.com/\${CIRCLE_PROJECT_USERNAME}/\${CIRCLE_PROJECT_REPONAME}.git"`,
      }),
      new commands.Run({
        name: `Tag core ${environment.graviteeioVersion} ${environment.isDryRun ? '- Dry Run' : ''}`,
        command: `# Only the root pom. The distribution carries its own triplet and releases under its own tag;
# what it assembles is its pin, which a reviewed pull request advances after this has published.
sed -i "s#<changelist>.*</changelist>#<changelist></changelist>#" pom.xml

git add --update
git commit -m "${tag}"
git tag ${tag}

sed -i "s#<revision>.*</revision>#<revision>${nextVersion}</revision>#" pom.xml
sed -i "s#<changelist>.*</changelist>#<changelist>-SNAPSHOT</changelist>#" pom.xml
# <sha1 /> is self-closing when the qualifier is empty, which the plain <sha1>.*</sha1> pattern
# never matches — the qualifier was silently kept on any pom already holding the empty form.
sed -i -E "s#<sha1( */>|>[^<]*</sha1>)#<sha1>${nextQualifier}</sha1>#" pom.xml

git add --update
git commit -m 'chore: prepare next core version [skip ci]'

# The branch first: the reverse order publishes a version that sits on no branch, should the
# branch push then fail.
git push ${environment.isDryRun ? '--dry-run' : ''} origin ${environment.branch}
git push ${environment.isDryRun ? '--dry-run' : ''} origin ${tag}
`,
      }),
    ];
    return new Job(PrepareCoreReleaseJob.jobName, BaseExecutor.create(), steps);
  }
}
