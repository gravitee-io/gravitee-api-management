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
import { CustomParametersList } from './parameters';
import { Generable, Schema } from './types';

/**
 * A reference to a command, job or executor exposed by an imported orb.
 * Its `name` is fully qualified (`alias/localName`) so it can key a step or a
 * workflow job.
 */
export class OrbRef implements Generable {
  public readonly name: string;

  constructor(
    localName: string,
    public readonly parameters: CustomParametersList,
    public readonly orb: OrbImport,
  ) {
    this.name = `${orb.alias}/${localName}`;
  }

  generate(): string {
    return this.name;
  }
}

/** Optional descriptor of the refs exposed by an imported orb. */
export interface OrbManifest {
  jobs?: Record<string, CustomParametersList>;
  executors?: Record<string, CustomParametersList>;
  commands?: Record<string, CustomParametersList>;
}

/**
 * An imported orb. Emits `{ alias: 'namespace/orb@version' }` in the `orbs`
 * section, and exposes `jobs` / `commands` / `executors` maps of `OrbRef`.
 */
export class OrbImport implements Generable {
  public readonly jobs: Record<string, OrbRef>;
  public readonly commands: Record<string, OrbRef>;
  public readonly executors: Record<string, OrbRef>;

  constructor(
    public readonly alias: string,
    public readonly namespace: string,
    public readonly orb: string,
    public readonly version: string,
    _logoUrl?: string,
    manifest?: OrbManifest,
  ) {
    this.jobs = {};
    this.commands = {};
    this.executors = {};

    for (const [name, parameters] of Object.entries(manifest?.jobs ?? {})) {
      this.jobs[name] = new OrbRef(name, parameters, this);
    }
    for (const [name, parameters] of Object.entries(manifest?.commands ?? {})) {
      this.commands[name] = new OrbRef(name, parameters, this);
    }
    for (const [name, parameters] of Object.entries(manifest?.executors ?? {})) {
      this.executors[name] = new OrbRef(name, parameters, this);
    }
  }

  generate(): Schema {
    return { [this.alias]: `${this.namespace}/${this.orb}@${this.version}` };
  }
}
