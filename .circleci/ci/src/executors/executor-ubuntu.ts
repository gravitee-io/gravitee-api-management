import { executors } from '@circleci/circleci-config-sdk';
import { config } from '../config';
import { Executor } from '@circleci/circleci-config-sdk/dist/src/lib/Components/Executors';
import { MachineResourceClass } from '@circleci/circleci-config-sdk/dist/src/lib/Components/Executors/types/MachineExecutor.types';

export class UbuntuExecutor {
  public static create(resource: MachineResourceClass = 'medium', useDockerLayerCaching: boolean = false): Executor {
    const image = `ubuntu-${config.executor.ubuntu.version}:${config.executor.ubuntu.tag}`;
    return new executors.MachineExecutor(resource, image, useDockerLayerCaching);
  }
}
