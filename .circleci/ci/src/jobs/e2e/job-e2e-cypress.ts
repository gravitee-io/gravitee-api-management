import { commands, Config, Job, reusable } from '@circleci/circleci-config-sdk';
import { DockerAzureLoginCommand, DockerAzureLogoutCommand, NotifyOnFailureCommand } from '../../commands';
import { Command } from '@circleci/circleci-config-sdk/dist/src/lib/Components/Commands/exports/Command';
import { computeImagesTag } from '../../utils';
import { CircleCIEnvironment } from '../../pipelines';
import { UbuntuExecutor } from '../../executors';

export class E2ECypressJob {
  private static jobName = `job-e2e-cypress`;
  public static create(dynamicConfig: Config, environment: CircleCIEnvironment): Job {
    const dockerAzureLoginCmd = DockerAzureLoginCommand.get(dynamicConfig);
    const dockerAzureLogoutCmd = DockerAzureLogoutCommand.get();
    const notifyOnFailureCmd = NotifyOnFailureCommand.get(dynamicConfig);
    dynamicConfig.addReusableCommand(dockerAzureLoginCmd);
    dynamicConfig.addReusableCommand(dockerAzureLogoutCmd);
    dynamicConfig.addReusableCommand(notifyOnFailureCmd);

    const dockerImageTag = computeImagesTag(environment.branch);

    const steps: Command[] = [
      new commands.Checkout(),
      new commands.workspace.Attach({ at: '.' }),
      new reusable.ReusedCommand(dockerAzureLoginCmd),
      new commands.Run({
        name: `Running UI tests`,
        command: `cd gravitee-apim-e2e
APIM_REGISTRY=graviteeio.azurecr.io APIM_TAG=${dockerImageTag} npm run test:ui`,
      }),
      new reusable.ReusedCommand(dockerAzureLogoutCmd),
      new reusable.ReusedCommand(notifyOnFailureCmd),
      new commands.StoreArtifacts({
        path: './gravitee-apim-e2e/.tmp/screenshots',
      }),
      new commands.StoreArtifacts({
        path: './gravitee-apim-e2e/.tmp/videos',
      }),
      new commands.StoreArtifacts({
        path: './gravitee-apim-e2e/.logs',
      }),
    ];
    return new Job(E2ECypressJob.jobName, UbuntuExecutor.create('large'), steps);
  }
}
