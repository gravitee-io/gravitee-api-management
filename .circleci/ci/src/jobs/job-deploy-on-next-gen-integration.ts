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
import { config } from '../config';
import { NotifyOnFailureCommand } from '../commands';
import { orbs } from '../orbs';
import { CircleCIEnvironment } from '../pipelines';
import { computeImagesTag } from '../utils';
import { BaseExecutor } from '../executors';

export class DeployOnNextGenIntegrationJob {
  private static jobName = 'job-deploy-on-next-gen-integration';
  public static create(dynamicConfig: Config, environment: CircleCIEnvironment): Job {
    dynamicConfig.importOrb(orbs.keeper);

    const notifyOnFailureCmd = NotifyOnFailureCommand.get(dynamicConfig, environment);
    dynamicConfig.addReusableCommand(notifyOnFailureCmd);

    const gitBranch = 'qa';
    const apimVersion = computeImagesTag(environment.branch, environment.sha1);
    const integrationControlPlaneId = '5fa78f';
    const integrationDataPlaneIds = ['5fa78f-894c1d', '5fa78f-558eb0'];

    const commandUpdatingDataplanes = (apimVersion: string, dataplaneIds: string[]) =>
      dataplaneIds
        .map(
          (dataplaneId) => `
/tmp/jq ".appVersion = \\"${apimVersion}\\"" dataplane-${dataplaneId}.json > tmp.$$.json && \\
mv tmp.$$.json dataplane-${dataplaneId}.json
git add dataplane-${dataplaneId}.json
`,
        )
        .join('\n');

    const steps: Command[] = [
      new commands.workspace.Attach({ at: '.' }),
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
        name: 'Install jq',
        command: 'wget https://github.com/jqlang/jq/releases/download/jq-1.7.1/jq-linux-amd64 -O /tmp/jq && chmod +x /tmp/jq',
      }),
      new commands.AddSSHKeys({ fingerprints: config.ssh.fingerprints }),
      new commands.Run({
        name: 'Clone cloud-deployments-configuration repo',
        command: `# For details see https://circleci.com/docs/2.0/gh-bb-integration/#establishing-the-authenticity-of-an-ssh-host
ssh-keyscan github.com >> ~/.ssh/known_hosts
git clone --branch ${gitBranch} git@github.com:gravitee-io/cloud-deployments-configuration.git cloud-deployments-configuration
`,
      }),
      new commands.Run({
        name: 'Deploy next-gen 🚀',
        command: `echo "# ---------------------------------------------------------------------- #"
echo "# - Deploying APIM ${apimVersion} in Integration env                            #"
echo "# ---------------------------------------------------------------------- #"

cd ./cloud-deployments-configuration/europe/westeurope/giocloud-worker1-europe-${gitBranch}

/tmp/jq ".appVersion = \\"${apimVersion}\\"" controlplane-${integrationControlPlaneId}.json > tmp.$$.json && \\
mv tmp.$$.json controlplane-${integrationControlPlaneId}.json
git add controlplane-${integrationControlPlaneId}.json

${commandUpdatingDataplanes(apimVersion, integrationDataPlaneIds)}

git commit -m "feat(${gitBranch}): deploying APIM ${apimVersion}"
git push
`,
      }),
      new reusable.ReusedCommand(notifyOnFailureCmd),
    ];
    return new Job(DeployOnNextGenIntegrationJob.jobName, BaseExecutor.create('small'), steps);
  }
}
