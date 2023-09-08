import { commands, Config, Job, reusable } from '@circleci/circleci-config-sdk';
import { Command } from '@circleci/circleci-config-sdk/dist/src/lib/Components/Commands/exports/Command';
import { BaseExecutor } from '../executors';
import { config } from '../config';
import { orbs } from '../orbs';
import { parse } from '../utils';
import { CircleCIEnvironment } from '../pipelines';

export class ReleaseCommitAndPrepareNextVersionJob {
  private static jobName = 'job-release-commit-and-prepare-next-version';

  public static create(dynamicConfig: Config, environment: CircleCIEnvironment): Job {
    const parsedVersion = parse(environment.graviteeioVersion);

    dynamicConfig.importOrb(orbs.keeper);

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

# Helm chart
sed "0,/version.*/s/version.*/version: ${environment.graviteeioVersion}/" -i helm/Chart.yaml

git add --update
git commit -m "${environment.graviteeioVersion}"
git tag ${environment.graviteeioVersion}

# Set the version to the next version (bump patch version + '-SNAPSHOT')
${
  parsedVersion.qualifier.full === ''
    ? `export MVN_PRJ_NEXT_VERSION="${parsedVersion.version.major}.${parsedVersion.version.minor}.$((${parsedVersion.version.patch}+1))"
export MVN_PRJ_NEXT_QUALIFIER=""`
    : `export MVN_PRJ_NEXT_VERSION="${parsedVersion.version.major}.${parsedVersion.version.minor}.${parsedVersion.version.patch}"
export MVN_PRJ_NEXT_QUALIFIER="-${parsedVersion.qualifier.name}.$((${parsedVersion.qualifier.version}+1))"`
}

sed -i "s#<revision>.*</revision>#<revision>\${MVN_PRJ_NEXT_VERSION}</revision>#" pom.xml                   
sed -i "s#<changelist>.*</changelist>#<changelist>-SNAPSHOT</changelist>#" pom.xml
sed -i "s#<sha1>.*</sha1>#<sha1>\${MVN_PRJ_NEXT_QUALIFIER}</sha1>#" pom.xml

sed -i 's#version: ".*"#version: "\${MVN_PRJ_NEXT_VERSION}\${MVN_PRJ_NEXT_QUALIFIER}-SNAPSHOT"#' gravitee-apim-rest-api/gravitee-apim-rest-api-portal/gravitee-apim-rest-api-portal-rest/src/main/resources/portal-openapi.yaml
sed -i 's#"version": ".*"#"version": "\${MVN_PRJ_NEXT_VERSION}\${MVN_PRJ_NEXT_QUALIFIER}-SNAPSHOT"#' gravitee-apim-console-webui/build.json
sed -i 's#"version": ".*"#"version": "\${MVN_PRJ_NEXT_VERSION}\${MVN_PRJ_NEXT_QUALIFIER}-SNAPSHOT"#' gravitee-apim-portal-webui/build.json

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
