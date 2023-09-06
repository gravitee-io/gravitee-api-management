import { commands, Config, Job, reusable } from '@circleci/circleci-config-sdk';
import { DockerAzureLoginCommand, DockerAzureLogoutCommand, NotifyOnFailureCommand } from '../../commands';
import { Command } from '@circleci/circleci-config-sdk/dist/src/lib/Components/Commands/exports/Command';
import { computeImagesTag } from '../../utils';
import { CircleCIEnvironment } from '../../pipelines';
import { orbs } from '../../orbs';
import { keeper } from '../../orbs/keeper';
import { config } from '../../config';
import { UbuntuExecutor } from '../../executors';

export class E2ETestJob {
  private static jobName = `job-e2e-test`;
  public static create(dynamicConfig: Config, environment: CircleCIEnvironment): Job {
    dynamicConfig.importOrb(orbs.keeper);

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
      new reusable.ReusedCommand(keeper.commands['env-export'], {
        'secret-url': config.secrets.graviteeLicense,
        'var-name': 'GRAVITEE_LICENSE',
      }),
      new commands.Run({
        name: `Running API & E2E tests in << parameters.execution_mode >> mode with << parameters.database >> database`,
        command: `cd gravitee-apim-e2e
if [ "<< parameters.execution_mode >>" = "v3" ]; then
  echo "Disable v4 emulation engine on APIM Gateway and Rest API"
  export V4_EMULATION_ENGINE_DEFAULT=no
fi
if [ -z "<< parameters.apim_client_tag >>" ]; then
  APIM_REGISTRY=graviteeio.azurecr.io APIM_TAG=${dockerImageTag} APIM_CLIENT_TAG=${dockerImageTag} npm run test:api:<< parameters.database >>
else 
  APIM_REGISTRY=graviteeio.azurecr.io APIM_TAG=${dockerImageTag} APIM_CLIENT_TAG=<< parameters.apim_client_tag >> npm run test:api:<< parameters.database >>
fi`,
      }),
      new reusable.ReusedCommand(dockerAzureLogoutCmd),

      new reusable.ReusedCommand(notifyOnFailureCmd),
      new commands.StoreTestResults({
        path: './gravitee-apim-e2e/.tmp/e2e-test-report.xml',
      }),
      new commands.StoreArtifacts({
        path: './gravitee-apim-e2e/.tmp/e2e-test-report.xml',
      }),
      new commands.StoreArtifacts({
        path: './gravitee-apim-e2e/.logs',
      }),
    ];
    return new Job(E2ETestJob.jobName, UbuntuExecutor.create(), steps);
  }
}
