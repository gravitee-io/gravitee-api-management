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
import { orbs } from '../../orbs';
import { NodeLtsExecutor } from '../../executors';
import { config } from '../../config';
import { CircleCIEnvironment } from '../../pipelines';

export class ReleaseHelmJob {
  private static jobName = 'job-release-helm';

  public static create(dynamicConfig: Config, environment: CircleCIEnvironment): Job {
    const apimVersion = environment.graviteeioVersion;

    dynamicConfig.importOrb(orbs.keeper);
    dynamicConfig.importOrb(orbs.helm);
    dynamicConfig.importOrb(orbs.github);

    const steps: Command[] = [
      new reusable.ReusedCommand(orbs.keeper.commands['env-export'], {
        'secret-url': config.secrets.gitUserName,
        'var-name': 'GIT_USER_NAME',
      }),
      new reusable.ReusedCommand(orbs.keeper.commands['env-export'], {
        'secret-url': config.secrets.gitUserEmail,
        'var-name': 'GIT_USER_EMAIL',
      }),
      new reusable.ReusedCommand(orbs.keeper.commands['env-export'], {
        'secret-url': config.secrets.githubApiToken,
        'var-name': 'GITHUB_TOKEN',
      }),
      new commands.AddSSHKeys({ fingerprints: config.ssh.fingerprints }),
      new commands.Run({
        name: 'Git config',
        command: `git config --global user.name "\${GIT_USER_NAME}"
git config --global user.email "\${GIT_USER_EMAIL}"`,
      }),
      new reusable.ReusedCommand(orbs.github.commands['setup']),
      new reusable.ReusedCommand(orbs.helm.commands['install_helm_client']),
    ];

    steps.push(new commands.Checkout());

    if (!environment.isDryRun) {
      steps.push(
        new commands.Run({
          name: `Checkout tag ${apimVersion}`,
          command: `git checkout ${apimVersion}`,
        }),
        new commands.Run({
          name: 'Update Chart and App versions',
          command: `sed "0,/appVersion.*/s/appVersion.*/appVersion: ${apimVersion}/" -i helm/Chart.yaml`,
        }),
      );
    }

    steps.push(
      new commands.Run({
        name: 'build the Charts',
        working_directory: './helm',
        command: `helm dependency update
helm package -d charts .

sed "s/name.*/name: apim3/" -i Chart.yaml
helm package -d charts .`,
      }),
      new commands.Run({
        name: 'Install dependencies',
        command: 'npm install',
        working_directory: './release',
      }),
    );

    if (!environment.isDryRun) {
      steps.push(
        new commands.Run({
          name: 'Open a PR to publish helm chart release into helm-charts repository',
          working_directory: './release',
          command: `npm run zx -- ci-steps/release-helm.mjs --version=${apimVersion}`,
        }),
      );
    } else {
      steps.push(
        new reusable.ReusedCommand(orbs.keeper.commands['env-export'], {
          'secret-url': config.secrets.azureRegistryUsername,
          'var-name': 'ACR_USER_NAME',
        }),
        new reusable.ReusedCommand(orbs.keeper.commands['env-export'], {
          'secret-url': config.secrets.azureRegistryPassword,
          'var-name': 'ACR_PASSWORD',
        }),
        new commands.Run({
          name: 'Publish helm chart release in azure repository DRY-RUN mode',
          working_directory: './helm',
          command: `helm registry login graviteeio.azurecr.io --username $ACR_USER_NAME --password $ACR_PASSWORD
helm push charts/apim-*.tgz oci://graviteeio.azurecr.io/helm/
helm push charts/apim3-*.tgz oci://graviteeio.azurecr.io/helm/`,
        }),
      );
    }

    return new Job(ReleaseHelmJob.jobName, NodeLtsExecutor.create(), steps);
  }
}
