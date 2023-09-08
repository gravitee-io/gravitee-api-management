import { executors } from '@circleci/circleci-config-sdk';
import { config } from '../config';
import { Executor } from '@circleci/circleci-config-sdk/dist/src/lib/Components/Executors';
import { DockerResourceClass } from '@circleci/circleci-config-sdk/dist/src/lib/Components/Executors/types/DockerExecutor.types';

export class BaseExecutor {
  public static create(resource: DockerResourceClass = 'medium'): Executor {
    const image = `${config.executor.base.image}:${config.executor.base.version}`;
    return new executors.DockerExecutor(image, resource);
  }
}
