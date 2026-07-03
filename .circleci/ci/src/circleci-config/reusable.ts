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
import { OrbRef } from './orb';
import { CustomParametersList } from './parameters';
import { Command, Generable, Schema } from './types';

/**
 * A named custom command (top-level `commands:` entry). Emits
 * `{ name: { parameters?, steps, description? } }`.
 */
export class ReusableCommand implements Generable {
  constructor(
    public readonly name: string,
    public readonly steps: Command[],
    public readonly parameters?: CustomParametersList,
    public readonly description?: string,
  ) {}

  generate(): Schema {
    const body: Schema = {};
    if (this.parameters && this.parameters.parameters.length > 0) {
      body.parameters = this.parameters.generate();
    }
    body.steps = this.steps.map((step) => step.generate());
    if (this.description !== undefined) {
      body.description = this.description;
    }
    return { [this.name]: body };
  }
}

/**
 * A step that reuses a custom command or an orb command. Emits the bare name
 * when it has no argument, otherwise `{ name: args }`.
 */
export class ReusedCommand implements Generable {
  constructor(
    private readonly command: ReusableCommand | OrbRef,
    private readonly parameters?: Record<string, unknown>,
  ) {}

  generate(): string | Schema {
    if (!this.parameters || Object.keys(this.parameters).length === 0) {
      return this.command.name;
    }
    return { [this.command.name]: this.parameters };
  }
}

/**
 * A job that declares parameters. Emits
 * `{ name: { parameters, <executor>, steps } }`.
 */
export class ParameterizedJob implements Generable {
  constructor(
    public readonly name: string,
    public readonly executor: Executor,
    public readonly parameters: CustomParametersList,
    public readonly steps: Command[],
  ) {}

  generate(): Schema {
    return {
      [this.name]: {
        parameters: this.parameters.generate(),
        ...this.executor.generate(),
        steps: this.steps.map((step) => step.generate()),
      },
    };
  }
}
