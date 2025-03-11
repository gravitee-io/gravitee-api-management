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
import { Command } from '@circleci/circleci-config-sdk/dist/src/lib/Components/Commands/exports/Command';
import { orbs } from '../../orbs';
import { BaseExecutor } from '../../executors';
import { AnyParameterLiteral } from '@circleci/circleci-config-sdk/dist/src/lib/Components/Parameters/types/CustomParameterLiterals.types';
import { config } from '../../config';

export class TestApimChartsJob {
  private static jobName = 'job-test-apim-charts';

  public static create(dynamicConfig: Config): Job {
    dynamicConfig.importOrb(orbs.helm);

    const params: parameters.CustomParametersList<AnyParameterLiteral> = new parameters.CustomParametersList([
      new parameters.CustomParameter('helmClientVersion', 'string', config.helm.defaultVersion, 'Version of helm to test'),
    ]);

    const steps: Command[] = [
      new commands.Checkout(),
      new reusable.ReusedCommand(orbs.helm.commands['install_helm_client'], { version: '<< parameters.helmClientVersion >>' }),
      new commands.Run({
        name: 'Install helm-unittest plugin',
        command: `helm plugin install https://github.com/helm-unittest/helm-unittest.git --version ${config.helm.helmUnitVersion}`,
      }),
      new commands.Run({
        name: 'Lint the helm charts available in helm/',
        command: `helm lint helm/`,
      }),
      new commands.Run({
        name: 'Execute the units tests in helm/',
        command: "helm unittest -f 'tests/**/*.yaml' helm/ -t JUnit -o apim-result.xml",
      }),
      new commands.StoreTestResults({
        path: 'apim-result.xml',
      }),
    ];
    return new reusable.ParameterizedJob(TestApimChartsJob.jobName, BaseExecutor.create('small'), params, steps);
  }
}
