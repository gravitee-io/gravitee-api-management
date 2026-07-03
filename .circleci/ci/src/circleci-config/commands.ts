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

/*
 * Native step commands. Each step emits `{ <step-key>: { ...parameters } }`,
 * spreading the caller's parameters so their insertion order is preserved
 * exactly (this is what the original SDK does, e.g. `save_cache` keys appear in
 * the order they were passed).
 */

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
    return { run: { ...this.parameters } };
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
    return { checkout: { ...this.parameters } };
  }
}

export interface AttachParameters {
  at: string;
}

/** An `attach_workspace` step. */
export class Attach implements Command {
  constructor(private readonly parameters: AttachParameters) {}

  generate(): Schema {
    return { attach_workspace: { ...this.parameters } };
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
    return { persist_to_workspace: { ...this.parameters } };
  }
}

export interface RestoreParameters {
  keys: string[];
  name?: string;
}

/** A `restore_cache` step. */
export class Restore implements Command {
  constructor(private readonly parameters: RestoreParameters) {}

  generate(): Schema {
    return { restore_cache: { ...this.parameters } };
  }
}

export interface SaveParameters {
  key: string;
  paths: string[];
  name?: string;
  when?: When;
}

/** A `save_cache` step. */
export class Save implements Command {
  constructor(private readonly parameters: SaveParameters) {}

  generate(): Schema {
    return { save_cache: { ...this.parameters } };
  }
}

export interface StoreTestResultsParameters {
  path: string;
}

/** A `store_test_results` step. */
export class StoreTestResults implements Command {
  constructor(private readonly parameters: StoreTestResultsParameters) {}

  generate(): Schema {
    return { store_test_results: { ...this.parameters } };
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
    return { store_artifacts: { ...this.parameters } };
  }
}

export interface SetupRemoteDockerParameters {
  version?: string;
}

/** A `setup_remote_docker` step. */
export class SetupRemoteDocker implements Command {
  constructor(private readonly parameters: SetupRemoteDockerParameters = {}) {}

  generate(): Schema {
    return { setup_remote_docker: { ...this.parameters } };
  }
}

export interface AddSSHKeysParameters {
  fingerprints: string[];
}

/** An `add_ssh_keys` step. */
export class AddSSHKeys implements Command {
  constructor(private readonly parameters: AddSSHKeysParameters) {}

  generate(): Schema {
    return { add_ssh_keys: { ...this.parameters } };
  }
}

/** `commands.workspace.*` namespace. */
export const workspace = { Attach, Persist };

/** `commands.cache.*` namespace. */
export const cache = { Restore, Save };
