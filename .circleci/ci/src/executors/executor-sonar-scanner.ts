import { executors } from '@circleci/circleci-config-sdk';
import { config } from '../config';
import { Executor } from '@circleci/circleci-config-sdk/dist/src/lib/Components/Executors';
import { DockerResourceClass } from '@circleci/circleci-config-sdk/dist/src/lib/Components/Executors/types/DockerExecutor.types';

export class SonarScannerExecutor {
  public static create(resource: DockerResourceClass = 'medium'): Executor {
    const image = `${config.executor.sonar.image}:${config.executor.sonar.version}`;
    return new executors.DockerExecutor(image, resource);
  }
}
