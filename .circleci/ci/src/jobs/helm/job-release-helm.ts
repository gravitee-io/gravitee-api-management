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
import { InstallYarnCommand } from '../../commands';

export class ReleaseHelmJob {
  private static jobName = 'job-release-helm';

  public static create(dynamicConfig: Config, environment: CircleCIEnvironment): Job {
    const apimVersion = environment.graviteeioVersion;

    dynamicConfig.importOrb(orbs.keeper);
    dynamicConfig.importOrb(orbs.helm);
    dynamicConfig.importOrb(orbs.github);

    const installYarnCmd = InstallYarnCommand.get();
    dynamicConfig.addReusableCommand(installYarnCmd);

    const steps: Command[] = [
      new commands.Checkout(),
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
      new reusable.ReusedCommand(orbs.helm.commands['install_helm_client'], { version: config.helm.defaultVersion }),
    ];

    if (!environment.isDryRun) {
      steps.push(
        new commands.Run({
          name: `Checkout tag ${apimVersion}`,
          command: `git checkout ${apimVersion}`,
        }),
      );
    }

    steps.push(
      new commands.Run({
        name: 'build the Charts',
        working_directory: './helm',
        command: `helm dependency update
cp -f "Chart.yaml" "../Chart.yaml.bak"
cp -f "values.yaml" "../values.yaml.bak"

${ReleaseHelmJob.prepareCharts('/docker/io')}

${environment.isDryRun ? ReleaseHelmJob.prepareChartsForAws('graviteedev', '/aws/dev') : ReleaseHelmJob.prepareChartsForAws('graviteeio', '/aws/io')}`,
      }),
      new reusable.ReusedCommand(installYarnCmd),
      new commands.Run({
        name: 'Install dependencies',
        command: 'yarn',
        working_directory: './release',
      }),
      new commands.Run({
        name: 'Install AWS CLI',
        command: `curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"
            unzip awscliv2.zip
            sudo ./aws/install
            aws --version`,
      }),
    );

    if (!environment.isDryRun) {
      steps.push(
        new commands.Run({
          name: 'Open a PR to publish helm chart release into helm-charts repository',
          working_directory: './release',
          command: `yarn zx ci-steps/release-helm.mjs --version=${apimVersion}`,
        }),
      );
      ReleaseHelmJob.prepareReleaseCommandForAWS(steps, environment, 'graviteeio', '/aws/io');
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
helm push charts/docker/io/apim-*.tgz oci://graviteeio.azurecr.io/helm/
helm push charts/docker/io/apim3-*.tgz oci://graviteeio.azurecr.io/helm/`,
        }),
      );
      ReleaseHelmJob.prepareReleaseCommandForAWS(steps, environment, 'graviteedev', '/aws/dev');
    }

    return new Job(ReleaseHelmJob.jobName, NodeLtsExecutor.create(), steps);
  }

  private static prepareCharts(localChartFolder: string): string {
    return `mkdir -p charts${localChartFolder}

helm package -d charts .
mv charts/apim-*.tgz charts${localChartFolder}/

sed "s/^name:.*/name: apim3/" -i Chart.yaml
helm package -d charts .
mv charts/apim3-*.tgz charts${localChartFolder}/`;
  }

  private static prepareChartsForAws(awsEcrName: string, localChartFolder: string): string {
    return `mkdir -p charts${localChartFolder}
cp -f "../Chart.yaml.bak" "Chart.yaml"
cp -f "../values.yaml.bak" "values.yaml"
sed -i -E "s|(repository: )graviteeio/apim-management-api|\\1${config.awsECRUrl}/${awsEcrName}/saas-apim-management-api|g" values.yaml
sed -i -E "s|(repository: )graviteeio/apim-gateway|\\1${config.awsECRUrl}/${awsEcrName}/saas-apim-gateway|g" values.yaml
sed -i -E "s|(repository: )graviteeio/apim-portal-ui|\\1${config.awsECRUrl}/${awsEcrName}/saas-apim-portal-ui|g" values.yaml
sed -i -E "s|(repository: )graviteeio/apim-management-ui|\\1${config.awsECRUrl}/${awsEcrName}/saas-apim-management-ui|g" values.yaml
helm package -d charts .
mv charts/apim-*.tgz charts${localChartFolder}/`;
  }

  private static prepareReleaseCommandForAWS(
    steps: Command[],
    environment: CircleCIEnvironment,
    awsEcrName: string,
    localChartFolder: string,
  ): void {
    steps.push(
      new reusable.ReusedCommand(orbs.keeper.commands['env-export'], {
        'secret-url': config.secrets.awsHelmAccessKeyId,
        'var-name': 'AWS_ACCESS_KEY_ID',
      }),
      new reusable.ReusedCommand(orbs.keeper.commands['env-export'], {
        'secret-url': config.secrets.awsHelmSecretAccessKey,
        'var-name': 'AWS_SECRET_ACCESS_KEY',
      }),
      new reusable.ReusedCommand(orbs.keeper.commands['env-export'], {
        'secret-url': config.secrets.awsHelmRegion,
        'var-name': 'AWS_REGION',
      }),
      new commands.Run({
        name: 'Login to AWS ECR for Helm OCI',
        command: `aws ecr get-login-password --region $AWS_REGION | helm registry login --username AWS --password-stdin ${config.awsECRUrl}`,
      }),
      new commands.Run({
        name: `Publish helm chart release in AWS ECR${environment.isDryRun ? ' DRY-RUN mode' : ''}`,
        working_directory: './helm',
        command: `helm push charts${localChartFolder}/apim-*.tgz oci://${config.awsECRUrl}/${awsEcrName}/helm/`,
      }),
    );
  }
}
