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
import { executors } from '@circleci/circleci-config-sdk';
import { config } from '../config';
import { Executor } from '@circleci/circleci-config-sdk/dist/src/lib/Components/Executors';
import { MachineResourceClass } from '@circleci/circleci-config-sdk/dist/src/lib/Components/Executors/types/MachineExecutor.types';

export class UbuntuExecutor {
  public static create(
    resource: MachineResourceClass = 'medium',
    useDockerLayerCaching: boolean = false,
    imageTag: string = config.executor.ubuntu.tag,
  ): Executor {
    const image = `ubuntu-${config.executor.ubuntu.version}:${imageTag}`;
    return new executors.MachineExecutor(resource, image, useDockerLayerCaching);
  }
}
