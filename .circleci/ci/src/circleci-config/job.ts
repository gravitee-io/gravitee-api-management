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
import { Executor } from './executors';
import { Command, Generable, Schema } from './types';

/** Optional job-level properties emitted after the steps (e.g. parallelism). */
export interface JobOptionalProperties {
  parallelism?: number;
  [key: string]: unknown;
}

/**
 * A job (top-level `jobs:` entry). Emits `{ name: { <executor>, steps, <properties> } }`.
 */
export class Job implements Generable {
  constructor(
    public readonly name: string,
    public readonly executor: Executor,
    public readonly steps: Command[],
    public readonly properties?: JobOptionalProperties,
  ) {}

  generate(): Schema {
    const body: Schema = {
      ...this.executor.generate(),
      steps: this.steps.map((step) => step.generate()),
    };
    if (this.properties) {
      Object.assign(body, this.properties);
    }
    return { [this.name]: body };
  }
}
