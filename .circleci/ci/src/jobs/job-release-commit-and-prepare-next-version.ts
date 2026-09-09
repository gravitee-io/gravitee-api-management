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

export class ReleaseCommitAndPrepareNextVersionJob {
  private static jobName = 'job-release-commit-and-prepare-next-version';

  private static buildHelmCommand(nextVersion: string, nextQualifier: string) {
    let command = `sed -e "0,/^version:/{s/version:.*/version: ${nextVersion}${nextQualifier}/}" \\
    -e "0,/^appVersion:/{ s/appVersion.*/appVersion: ${nextVersion}${nextQualifier}/ }" \\`;
    // Do not clean the helm changelog when building pre-release
    if (!nextQualifier) {
      command += `
    -e '/artifacthub.io\\/changes/,\${ s/|// }' \\
    -e '/artifacthub.io\\/changes:/q0' \\`;
    }
    command += `
    -i helm/Chart.yaml`;
    return command;
  }

  public static create(dynamicConfig: Config, environment: CircleCIEnvironment): Job {
    dynamicConfig.importOrb(orbs.keeper);
    dynamicConfig.importOrb(orbs.github);

    const { version: nextVersion, qualifier: nextQualifier } = nextDevelopmentVersion(environment.graviteeioVersion);

    const steps: Command[] = [
      new commands.Checkout(),
      new commands.workspace.Attach({ at: '.' }),
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
        // The bot token does, which is what makes the tag a trigger rather than a label. The remote
        // has to move to HTTPS for the credential helper to be consulted at all: \`checkout\` leaves
        // it on SSH. \`gh auth setup-git\` reads the token from the helper, so it stays out of
        // .git/config and out of any command that prints the remote.
        name: 'Push over HTTPS, so that pushing a tag triggers a pipeline',
        command: `gh auth setup-git
git remote set-url origin "https://github.com/\${CIRCLE_PROJECT_USERNAME}/\${CIRCLE_PROJECT_REPONAME}.git"`,
      }),
      new commands.Run({
        name: `Git release ${environment.isDryRun ? '- Dry Run' : ''}`,
        command: `# Remove \`-SNAPSHOT\` from source
# Backend. Both poms: this releases the two reactors at once, so both carry the version being
# released. The core lane moves the root alone, the distribution lane will move the other.
POMS="pom.xml gravitee-apim-distribution/pom.xml"
for POM in \${POMS}; do
  sed -i "s#<changelist>.*</changelist>#<changelist></changelist>#" "\${POM}"
done

# UI
sed -i 's/"version": ".*"/"version": "${environment.graviteeioVersion}"/' gravitee-apim-console-webui/build.json
sed -i 's/"version": ".*"/"version": "${environment.graviteeioVersion}"/' gravitee-apim-portal-webui/build.json
sed -i 's/"version": ".*"/"version": "${environment.graviteeioVersion}"/' gravitee-apim-portal-webui-next/build.json
sed -i 's/"version": ".*"/"version": "${environment.graviteeioVersion}"/' gravitee-gamma/gravitee-gamma-control-plane-webui/build.json

git add --update
git commit -m "${environment.graviteeioVersion}"
git tag ${environment.graviteeioVersion}

# Set the version to the next version (bump patch version + '-SNAPSHOT')
for POM in \${POMS}; do
  sed -i "s#<revision>.*</revision>#<revision>${nextVersion}</revision>#" "\${POM}"
  sed -i "s#<changelist>.*</changelist>#<changelist>-SNAPSHOT</changelist>#" "\${POM}"
  # <sha1 /> is self-closing when the qualifier is empty, which the plain <sha1>.*</sha1> pattern
  # never matches — the qualifier was silently kept on any pom already holding the empty form.
  sed -i -E "s#<sha1( */>|>[^<]*</sha1>)#<sha1>${nextQualifier}</sha1>#" "\${POM}"
done

sed -i 's#version: ".*"#version: "${nextVersion}${nextQualifier}-SNAPSHOT"#' gravitee-apim-rest-api/gravitee-apim-rest-api-portal/gravitee-apim-rest-api-portal-rest/src/main/resources/portal-openapi.yaml
sed -i 's#"version": ".*"#"version": "${nextVersion}${nextQualifier}-SNAPSHOT"#' gravitee-apim-console-webui/build.json
sed -i 's#"version": ".*"#"version": "${nextVersion}${nextQualifier}-SNAPSHOT"#' gravitee-apim-portal-webui/build.json
sed -i 's#"version": ".*"#"version": "${nextVersion}${nextQualifier}-SNAPSHOT"#' gravitee-apim-portal-webui-next/build.json
sed -i 's#"version": ".*"#"version": "${nextVersion}${nextQualifier}-SNAPSHOT"#' gravitee-gamma/gravitee-gamma-control-plane-webui/build.json

# Helm chart increase version, appVersion and clean the artifacthub.io/changes annotation
${this.buildHelmCommand(nextVersion, nextQualifier)}

git add --update
git commit -m 'chore: prepare next version [skip ci]'

git push -u ${environment.isDryRun ? '--dry-run' : ''} origin ${environment.branch}
git push --tags ${environment.isDryRun ? '--dry-run' : ''} origin ${environment.branch}
`,
      }),
    ];
    return new Job(ReleaseCommitAndPrepareNextVersionJob.jobName, BaseExecutor.create(), steps);
  }
}
