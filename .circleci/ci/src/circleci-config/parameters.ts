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
import { Generable, ParameterType, Schema } from './types';

/**
 * A single pipeline / command / job parameter definition.
 * Emits `{ type, default?, description }` (with `default` omitted when undefined).
 */
export class CustomParameter implements Generable {
  constructor(
    public readonly name: string,
    public readonly type: ParameterType,
    public readonly defaultValue?: unknown,
    public readonly description: string = '',
  ) {}

  generate(): Schema {
    const body: Schema = { type: this.type };
    if (this.defaultValue !== undefined) {
      body.default = this.defaultValue;
    }
    body.description = this.description;
    return body;
  }
}

/**
 * An enum parameter. Emits `{ type: 'enum', default?, description, enum }`.
 */
export class CustomEnumParameter implements Generable {
  public readonly type: ParameterType = 'enum';

  constructor(
    public readonly name: string,
    public readonly enumValues: string[],
    public readonly defaultValue?: unknown,
    public readonly description: string = '',
  ) {}

  generate(): Schema {
    const body: Schema = { type: this.type };
    if (this.defaultValue !== undefined) {
      body.default = this.defaultValue;
    }
    body.description = this.description;
    body.enum = this.enumValues;
    return body;
  }
}

export type AnyParameter = CustomParameter | CustomEnumParameter;

/**
 * An ordered list of parameters. Emits a map keyed by parameter name, preserving
 * insertion order.
 */
export class CustomParametersList implements Generable {
  constructor(public readonly parameters: AnyParameter[] = []) {}

  generate(): Schema {
    const out: Schema = {};
    for (const parameter of this.parameters) {
      out[parameter.name] = parameter.generate();
    }
    return out;
  }
}
