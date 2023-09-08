import { executors } from '@circleci/circleci-config-sdk';
import { config } from '../config';
import { Executor } from '@circleci/circleci-config-sdk/dist/src/lib/Components/Executors';
import { DockerResourceClass } from '@circleci/circleci-config-sdk/dist/src/lib/Components/Executors/types/DockerExecutor.types';

export class AzureCliExecutor {
  public static create(resource: DockerResourceClass = 'medium'): Executor {
    const image = `${config.executor.azure.image}:${config.executor.azure.version}`;
    return new executors.DockerExecutor(image, resource);
  }
}
