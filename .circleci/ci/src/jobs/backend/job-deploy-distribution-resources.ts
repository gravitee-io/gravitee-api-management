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
import { Config, Job, commands, reusable } from '../../circleci-config';
import {
  AzureArtifactsTokenCommand,
  CheckDistributionCorePinCommand,
  PrepareGpgCmd,
  RestoreMavenJobCacheCommand,
  SaveMavenJobCacheCommand,
} from '../../commands';
import { config } from '../../config';
import { OpenJdkNodeExecutor } from '../../executors';
import { CircleCIEnvironment } from '../../pipelines';
import { DISTRIBUTION_DEPLOY_ARGS } from '../../utils';

/**
 * Deploys the distribution resource modules to the Azure feed on release. A job of its own so that
 * the images, the bundle and the download website, which do not consume these artifacts, are not held
 * back by a feed failure. Only the release announcement waits on it.
 */
export class DeployDistributionResourcesJob {
  private static jobName = 'job-deploy-distribution-resources';

  public static create(dynamicConfig: Config, environment: CircleCIEnvironment): Job {
    const restoreMavenJobCacheCmd = RestoreMavenJobCacheCommand.get(environment);
    dynamicConfig.addReusableCommand(restoreMavenJobCacheCmd);
    const saveMavenJobCacheCmd = SaveMavenJobCacheCommand.get();
    dynamicConfig.addReusableCommand(saveMavenJobCacheCmd);
    const azureArtifactsTokenCmd = AzureArtifactsTokenCommand.get(dynamicConfig);
    dynamicConfig.addReusableCommand(azureArtifactsTokenCmd);
    const checkDistributionCorePinCmd = CheckDistributionCorePinCommand.get(environment);
    dynamicConfig.addReusableCommand(checkDistributionCorePinCmd);
    const prepareGpgCmd = PrepareGpgCmd.get(dynamicConfig);
    dynamicConfig.addReusableCommand(prepareGpgCmd);

    return new Job(DeployDistributionResourcesJob.jobName, OpenJdkNodeExecutor.create(), [
      new commands.Checkout(),
      new commands.workspace.Attach({ at: '.' }),
      new reusable.ReusedCommand(restoreMavenJobCacheCmd, { jobName: DeployDistributionResourcesJob.jobName }),
      new reusable.ReusedCommand(azureArtifactsTokenCmd),
      // Runs in parallel with the backend job, so it cannot rely on that job's refusal.
      new reusable.ReusedCommand(checkDistributionCorePinCmd),
      new reusable.ReusedCommand(prepareGpgCmd),
      new commands.Run({
        name: 'Maven deploy the distribution resources to the Azure feed (releases)',
        command: `mvn deploy --settings ${config.maven.settingsFile} -B -P gio-release -DskipTests=true -Dskip.validation -Dgravitee.archrules.skip=true --no-transfer-progress ${DISTRIBUTION_DEPLOY_ARGS} -DaltDeploymentRepository=azure-artifacts-gravitee::${config.maven.azureFeedUrl}`,
        environment: {
          MAVEN_OPTS: '-Xmx2048m',
        },
      }),
      new reusable.ReusedCommand(saveMavenJobCacheCmd, { jobName: DeployDistributionResourcesJob.jobName }),
    ]);
  }
}
