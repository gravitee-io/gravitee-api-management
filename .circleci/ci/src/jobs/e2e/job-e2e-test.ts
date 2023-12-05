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
import { commands, Config, Job, parameters, reusable } from '@circleci/circleci-config-sdk';
import { DockerAzureLoginCommand, DockerAzureLogoutCommand, NotifyOnFailureCommand } from '../../commands';
import { Command } from '@circleci/circleci-config-sdk/dist/src/lib/Components/Commands/exports/Command';
import { computeImagesTag } from '../../utils';
import { CircleCIEnvironment } from '../../pipelines';
import { orbs } from '../../orbs';
import { UbuntuExecutor } from '../../executors';

export class E2ETestJob {
  private static jobName = `job-e2e-test`;
  private static customParametersList = new parameters.CustomParametersList([
    new parameters.CustomParameter('apim_client_tag', 'string', ''),
    new parameters.CustomParameter('execution_mode', 'string', ''),
    new parameters.CustomParameter('database', 'string', ''),
  ]);
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
      new commands.Run({
        name: `Running API & E2E tests in << parameters.execution_mode >> mode with << parameters.database >> database`,
        command: `cd gravitee-apim-e2e
if [ "<< parameters.execution_mode >>" = "jupiter" ]; then
  echo "Enabling jupiter mode on APIM Gateway and Rest API"
  export JUPITER_MODE_ENABLED=true
fi
if [ -z "<< parameters.apim_client_tag >>" ]; then
  APIM_REGISTRY=graviteeio.azurecr.io APIM_TAG=${dockerImageTag} APIM_CLIENT_TAG=${dockerImageTag} npm run test:api:<< parameters.database >>
else 
  if [[ "<< parameters.apim_client_tag >>" == *"@"* ]]; then
    echo "Using custom registry for client"
    CLIENT_REGISTRY=$(echo "<< parameters.apim_client_tag >>" | cut -f1 -d@)
    CLIENT_TAG=$(echo "<< parameters.apim_client_tag >>" | cut -f2 -d@)
    APIM_REGISTRY=graviteeio.azurecr.io APIM_TAG=${dockerImageTag} APIM_CLIENT_REGISTRY=\${CLIENT_REGISTRY} APIM_CLIENT_TAG=\${CLIENT_TAG} npm run test:api:<< parameters.database >>
  else
    echo "Using ACR registry for client"
    APIM_REGISTRY=graviteeio.azurecr.io APIM_TAG=${dockerImageTag} APIM_CLIENT_REGISTRY=graviteeio.azurecr.io APIM_CLIENT_TAG=<< parameters.apim_client_tag >> npm run test:api:<< parameters.database >>
  fi
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
    return new reusable.ParameterizedJob(E2ETestJob.jobName, UbuntuExecutor.create(), E2ETestJob.customParametersList, steps);
  }
}
