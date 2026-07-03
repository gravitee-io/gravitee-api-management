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
import { DockerResourceClass, Generable, MachineResourceClass, Schema } from './types';

/**
 * Base class for executors. An executor emits the fragment merged into a job:
 * the runtime declaration (`docker`/`machine`) plus its `resource_class`.
 */
export abstract class Executor implements Generable {
  protected constructor(public readonly resourceClass: string) {}

  abstract generate(): Schema;
}

/** A Docker executor. Emits `{ docker: [{ image }], resource_class }`. */
export class DockerExecutor extends Executor {
  constructor(
    public readonly image: string,
    resourceClass: DockerResourceClass = 'medium',
  ) {
    super(resourceClass);
  }

  generate(): Schema {
    return {
      docker: [{ image: this.image }],
      resource_class: this.resourceClass,
    };
  }
}

/** A Machine executor. Emits `{ machine: { image, docker_layer_caching }, resource_class }`. */
export class MachineExecutor extends Executor {
  constructor(
    resourceClass: MachineResourceClass = 'medium',
    public readonly image: string = 'ubuntu-2004:current',
    public readonly dockerLayerCaching: boolean = false,
  ) {
    super(resourceClass);
  }

  generate(): Schema {
    return {
      machine: {
        image: this.image,
        docker_layer_caching: this.dockerLayerCaching,
      },
      resource_class: this.resourceClass,
    };
  }
}
