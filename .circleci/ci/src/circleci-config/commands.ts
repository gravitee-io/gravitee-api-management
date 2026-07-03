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
import { Command, Schema } from './types';

export type When = 'always' | 'on_success' | 'on_fail';

export interface RunParameters {
  command: string;
  name?: string;
  working_directory?: string;
  environment?: Record<string, string>;
  when?: When;
}

/** A `run` step. */
export class Run implements Command {
  constructor(private readonly parameters: RunParameters) {}

  generate(): Schema {
    const body: Schema = {};
    if (this.parameters.name !== undefined) {
      body.name = this.parameters.name;
    }
    body.command = this.parameters.command;
    if (this.parameters.working_directory !== undefined) {
      body.working_directory = this.parameters.working_directory;
    }
    if (this.parameters.environment !== undefined) {
      body.environment = this.parameters.environment;
    }
    if (this.parameters.when !== undefined) {
      body.when = this.parameters.when;
    }
    return { run: body };
  }
}

export interface CheckoutParameters {
  method?: string;
}

/** A `checkout` step. Emits the bare string when it has no parameter. */
export class Checkout implements Command {
  constructor(private readonly parameters?: CheckoutParameters) {}

  generate(): string | Schema {
    if (!this.parameters || this.parameters.method === undefined) {
      return 'checkout';
    }
    return { checkout: { method: this.parameters.method } };
  }
}

export interface AttachParameters {
  at: string;
}

/** An `attach_workspace` step. */
export class Attach implements Command {
  constructor(private readonly parameters: AttachParameters) {}

  generate(): Schema {
    return { attach_workspace: { at: this.parameters.at } };
  }
}

export interface PersistParameters {
  root: string;
  paths: string[];
}

/** A `persist_to_workspace` step. */
export class Persist implements Command {
  constructor(private readonly parameters: PersistParameters) {}

  generate(): Schema {
    return {
      persist_to_workspace: {
        root: this.parameters.root,
        paths: this.parameters.paths,
      },
    };
  }
}

export interface RestoreParameters {
  keys: string[];
}

/** A `restore_cache` step. */
export class Restore implements Command {
  constructor(private readonly parameters: RestoreParameters) {}

  generate(): Schema {
    return { restore_cache: { keys: this.parameters.keys } };
  }
}

export interface SaveParameters {
  key: string;
  paths: string[];
  when?: When;
}

/** A `save_cache` step. */
export class Save implements Command {
  constructor(private readonly parameters: SaveParameters) {}

  generate(): Schema {
    const body: Schema = {
      key: this.parameters.key,
      paths: this.parameters.paths,
    };
    if (this.parameters.when !== undefined) {
      body.when = this.parameters.when;
    }
    return { save_cache: body };
  }
}

export interface StoreTestResultsParameters {
  path: string;
}

/** A `store_test_results` step. */
export class StoreTestResults implements Command {
  constructor(private readonly parameters: StoreTestResultsParameters) {}

  generate(): Schema {
    return { store_test_results: { path: this.parameters.path } };
  }
}

export interface StoreArtifactsParameters {
  path: string;
  destination?: string;
}

/** A `store_artifacts` step. */
export class StoreArtifacts implements Command {
  constructor(private readonly parameters: StoreArtifactsParameters) {}

  generate(): Schema {
    const body: Schema = { path: this.parameters.path };
    if (this.parameters.destination !== undefined) {
      body.destination = this.parameters.destination;
    }
    return { store_artifacts: body };
  }
}

export interface SetupRemoteDockerParameters {
  version?: string;
}

/** A `setup_remote_docker` step. */
export class SetupRemoteDocker implements Command {
  constructor(private readonly parameters: SetupRemoteDockerParameters = {}) {}

  generate(): Schema {
    const body: Schema = {};
    if (this.parameters.version !== undefined) {
      body.version = this.parameters.version;
    }
    return { setup_remote_docker: body };
  }
}

export interface AddSSHKeysParameters {
  fingerprints: string[];
}

/** An `add_ssh_keys` step. */
export class AddSSHKeys implements Command {
  constructor(private readonly parameters: AddSSHKeysParameters) {}

  generate(): Schema {
    return { add_ssh_keys: { fingerprints: this.parameters.fingerprints } };
  }
}

/** `commands.workspace.*` namespace. */
export const workspace = { Attach, Persist };

/** `commands.cache.*` namespace. */
export const cache = { Restore, Save };
