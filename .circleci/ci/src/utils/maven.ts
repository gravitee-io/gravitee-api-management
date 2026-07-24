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
import { DockerResourceClass } from '@circleci/circleci-config-sdk/dist/src/lib/Components/Executors/types/DockerExecutor.types';
import { MachineResourceClass } from '@circleci/circleci-config-sdk/dist/src/lib/Components/Executors/types/MachineExecutor.types';

const RESOURCE_CLASS_CPUS: Record<string, number> = {
  small: 1,
  medium: 2,
  'medium+': 3,
  large: 4,
  xlarge: 8,
  '2xlarge': 16,
};

/**
 * Maven `-T` flag with a fixed thread count of 2 threads per vCPU of the given resource class.
 *
 * Must be used instead of `-T 2C` on docker executors: the JVM computes `C` from the CPUs of the
 * underlying host, not from the resource class, so the parallelism (and the memory consumed by the
 * build) changes whenever CircleCI changes its fleet. The resource class must be the one given to
 * the executor of the job running the command.
 */
export function mavenParallelism(resourceClass: DockerResourceClass | MachineResourceClass): string {
  const cpus = RESOURCE_CLASS_CPUS[resourceClass];
  if (!cpus) {
    throw new Error(`Unsupported resource class for maven parallelism: ${resourceClass}`);
  }
  return `-T ${2 * cpus}`;
}
