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
import { Config, Job, reusable } from '@circleci/circleci-config-sdk';
import { CircleCIEnvironment } from '../pipelines';
import { Command } from '@circleci/circleci-config-sdk/dist/src/lib/Components/Commands/exports/Command';
import { config } from '../config';
import { keeper } from '../orbs/keeper';
import { AddDockerImageInSnykCommand } from '../commands';
import { computeImagesTag } from '../utils';
import { BaseExecutor } from '../executors';

export class AddDockerImagesInSnykJob {
  private static jobName = 'job-add-docker-images-in-snyk';
  public static create(dynamicConfig: Config, environment: CircleCIEnvironment): Job {
    dynamicConfig.importOrb(keeper);

    const addDockerImageInSnykCommand = AddDockerImageInSnykCommand.get();
    dynamicConfig.addReusableCommand(addDockerImageInSnykCommand);

    const tag = computeImagesTag(environment.branch);

    const steps: Command[] = [
      new reusable.ReusedCommand(keeper.commands['env-export'], {
        'secret-url': config.secrets.snykApiToken,
        'var-name': 'SNYK_API_TOKEN',
      }),
      new reusable.ReusedCommand(keeper.commands['env-export'], {
        'secret-url': config.secrets.snykOrgId,
        'var-name': 'SNYK_ORG_ID',
      }),
      new reusable.ReusedCommand(keeper.commands['env-export'], {
        'secret-url': config.secrets.snykIntegrationId,
        'var-name': 'SNYK_INTEGRATION_ID',
      }),
      new reusable.ReusedCommand(addDockerImageInSnykCommand, {
        'docker-image-name': config.dockerImages.gateway.image,
        version: tag,
      }),
      new reusable.ReusedCommand(addDockerImageInSnykCommand, {
        'docker-image-name': config.dockerImages.managementApi.image,
        version: tag,
      }),
      new reusable.ReusedCommand(addDockerImageInSnykCommand, {
        'docker-image-name': config.dockerImages.console.image,
        version: tag,
      }),
      new reusable.ReusedCommand(addDockerImageInSnykCommand, {
        'docker-image-name': config.dockerImages.portal.image,
        version: tag,
      }),
    ];
    return new Job(AddDockerImagesInSnykJob.jobName, BaseExecutor.create('small'), steps);
  }
}
