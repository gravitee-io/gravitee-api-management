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
import { commands, Config, Job, reusable } from '@circleci/circleci-config-sdk';
import { Command } from '@circleci/circleci-config-sdk/dist/src/lib/Components/Commands/exports/Command';
import { BaseExecutor } from '../executors';
import { config } from '../config';
import { orbs } from '../orbs';
import { parse } from '../utils';
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
    const parsedVersion = parse(environment.graviteeioVersion);

    dynamicConfig.importOrb(orbs.keeper);

    let nextVersion = '';
    let nextQualifier = '';
    if (parsedVersion.qualifier.full === '') {
      nextVersion = `${parsedVersion.version.major}.${parsedVersion.version.minor}.${Number(parsedVersion.version.patch) + 1}`;
      nextQualifier = '';
    } else {
      nextVersion = `${parsedVersion.version.major}.${parsedVersion.version.minor}.${parsedVersion.version.patch}`;
      nextQualifier = `-${parsedVersion.qualifier.name}.${Number(parsedVersion.qualifier.version) + 1}`;
    }

    const steps: Command[] = [
      new commands.Checkout(),
      new commands.workspace.Attach({ at: '.' }),
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
        name: `Git release ${environment.isDryRun ? '- Dry Run' : ''}`,
        command: `# Remove \`-SNAPSHOT\` from source
# Backend
sed -i "s#<changelist>.*</changelist>#<changelist></changelist>#" pom.xml

# UI
sed -i 's/"version": ".*"/"version": "${environment.graviteeioVersion}"/' gravitee-apim-console-webui/build.json
sed -i 's/"version": ".*"/"version": "${environment.graviteeioVersion}"/' gravitee-apim-portal-webui/build.json
sed -i 's/"version": ".*"/"version": "${environment.graviteeioVersion}"/' gravitee-apim-portal-webui-next/build.json

git add --update
git commit -m "${environment.graviteeioVersion}"
git tag ${environment.graviteeioVersion}

# Set the version to the next version (bump patch version + '-SNAPSHOT')
sed -i "s#<revision>.*</revision>#<revision>${nextVersion}</revision>#" pom.xml                   
sed -i "s#<changelist>.*</changelist>#<changelist>-SNAPSHOT</changelist>#" pom.xml
sed -i "s#<sha1>.*</sha1>#<sha1>${nextQualifier}</sha1>#" pom.xml

sed -i 's#version: ".*"#version: "${nextVersion}${nextQualifier}-SNAPSHOT"#' gravitee-apim-rest-api/gravitee-apim-rest-api-portal/gravitee-apim-rest-api-portal-rest/src/main/resources/portal-openapi.yaml
sed -i 's#"version": ".*"#"version": "${nextVersion}${nextQualifier}-SNAPSHOT"#' gravitee-apim-console-webui/build.json
sed -i 's#"version": ".*"#"version": "${nextVersion}${nextQualifier}-SNAPSHOT"#' gravitee-apim-portal-webui/build.json
sed -i 's#"version": ".*"#"version": "${nextVersion}${nextQualifier}-SNAPSHOT"#' gravitee-apim-portal-webui-next/build.json

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
