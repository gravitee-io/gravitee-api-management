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
        'docker-image-name': config.dockerImages.gateway,
        version: tag,
      }),
      new reusable.ReusedCommand(addDockerImageInSnykCommand, {
        'docker-image-name': config.dockerImages.managementApi,
        version: tag,
      }),
      new reusable.ReusedCommand(addDockerImageInSnykCommand, {
        'docker-image-name': config.dockerImages.console,
        version: tag,
      }),
      new reusable.ReusedCommand(addDockerImageInSnykCommand, {
        'docker-image-name': config.dockerImages.portal,
        version: tag,
      }),
    ];
    return new Job(AddDockerImagesInSnykJob.jobName, BaseExecutor.create('small'), steps);
  }
}
