/**
 * CircleCI command factories — all built-in step types.
 */

export interface CommandStep {
  serialize(): Record<string, unknown>;
}

// ─── Run ─────────────────────────────────────────────────────────────────────

export interface RunOptions {
  command: string;
  name?: string;
  shell?: string;
  environment?: Record<string, string | number | boolean>;
  background?: boolean;
  workingDirectory?: string;
  noOutputTimeout?: string;
  when?: 'always' | 'on_success' | 'on_fail';
}

class RunCommand implements CommandStep {
  constructor(private opts: RunOptions) {}
  serialize(): Record<string, unknown> {
    const run: Record<string, unknown> = { command: this.opts.command };
    if (this.opts.name) run.name = this.opts.name;
    if (this.opts.shell) run.shell = this.opts.shell;
    if (this.opts.environment) run.environment = this.opts.environment;
    if (this.opts.background) run.background = this.opts.background;
    if (this.opts.workingDirectory) run.working_directory = this.opts.workingDirectory;
    if (this.opts.noOutputTimeout) run.no_output_timeout = this.opts.noOutputTimeout;
    if (this.opts.when) run.when = this.opts.when;
    return { run };
  }
}

/** Create a run step. Shorthand: `run('Name', 'command')`, `run('Name', { command, ... })` or `run({ command, name, ... })` */
export function run(nameOrOpts: string | RunOptions, commandOrOpts?: string | Omit<RunOptions, 'name'>): CommandStep {
  if (typeof nameOrOpts === 'string') {
    if (typeof commandOrOpts === 'string') {
      return new RunCommand({ name: nameOrOpts, command: commandOrOpts });
    }
    return new RunCommand({ name: nameOrOpts, ...commandOrOpts! });
  }
  return new RunCommand(nameOrOpts);
}

// ─── Checkout ────────────────────────────────────────────────────────────────

class CheckoutCommand implements CommandStep {
  constructor(private path?: string) {}
  serialize(): Record<string, unknown> {
    if (this.path) return { checkout: { path: this.path } };
    return { checkout: { path: '.' } };
  }
}

export function checkout(path?: string): CommandStep {
  return new CheckoutCommand(path);
}

// ─── Cache ───────────────────────────────────────────────────────────────────

class CacheRestoreCommand implements CommandStep {
  constructor(private opts: { keys: string[]; name?: string }) {}
  serialize(): Record<string, unknown> {
    const restore: Record<string, unknown> = { keys: this.opts.keys };
    if (this.opts.name) restore.name = this.opts.name;
    return { restore_cache: restore };
  }
}

class CacheSaveCommand implements CommandStep {
  constructor(private opts: { key: string; paths: string[]; name?: string; when?: 'always' | 'on_success' | 'on_fail' }) {}
  serialize(): Record<string, unknown> {
    const save: Record<string, unknown> = { key: this.opts.key, paths: this.opts.paths };
    if (this.opts.name) save.name = this.opts.name;
    if (this.opts.when) save.when = this.opts.when;
    return { save_cache: save };
  }
}

export const cache = {
  restore(opts: { keys: string[]; name?: string }): CommandStep {
    return new CacheRestoreCommand(opts);
  },
  save(opts: { key: string; paths: string[]; name?: string; when?: 'always' | 'on_success' | 'on_fail' }): CommandStep {
    return new CacheSaveCommand(opts);
  },
};

// ─── Workspace ───────────────────────────────────────────────────────────────

class WorkspacePersistCommand implements CommandStep {
  constructor(private opts: { root: string; paths: string[] }) {}
  serialize(): Record<string, unknown> {
    return { persist_to_workspace: { root: this.opts.root, paths: this.opts.paths } };
  }
}

class WorkspaceAttachCommand implements CommandStep {
  constructor(private opts: { at: string }) {}
  serialize(): Record<string, unknown> {
    return { attach_workspace: { at: this.opts.at } };
  }
}

export const workspace = {
  persist(opts: { root: string; paths: string[] }): CommandStep {
    return new WorkspacePersistCommand(opts);
  },
  attach(opts: { at: string }): CommandStep {
    return new WorkspaceAttachCommand(opts);
  },
};

// ─── Store Artifacts / Test Results ──────────────────────────────────────────

class StoreArtifactsCommand implements CommandStep {
  constructor(private opts: { path: string; destination?: string }) {}
  serialize(): Record<string, unknown> {
    const s: Record<string, unknown> = { path: this.opts.path };
    if (this.opts.destination) s.destination = this.opts.destination;
    return { store_artifacts: s };
  }
}

class StoreTestResultsCommand implements CommandStep {
  constructor(private path: string) {}
  serialize(): Record<string, unknown> {
    return { store_test_results: { path: this.path } };
  }
}

export function storeArtifacts(opts: { path: string; destination?: string }): CommandStep {
  return new StoreArtifactsCommand(opts);
}

export function storeTestResults(path: string): CommandStep {
  return new StoreTestResultsCommand(path);
}

// ─── SSH Keys ────────────────────────────────────────────────────────────────

class AddSshKeysCommand implements CommandStep {
  constructor(private fingerprints?: string[]) {}
  serialize(): Record<string, unknown> {
    if (this.fingerprints) return { add_ssh_keys: { fingerprints: this.fingerprints } };
    return { add_ssh_keys: {} };
  }
}

export function addSshKeys(fingerprints?: string[]): CommandStep {
  return new AddSshKeysCommand(fingerprints);
}

// ─── Setup Remote Docker ─────────────────────────────────────────────────────

class SetupRemoteDockerCommand implements CommandStep {
  constructor(private opts?: { version?: string; dockerLayerCaching?: boolean }) {}
  serialize(): Record<string, unknown> {
    if (!this.opts) return { setup_remote_docker: {} };
    const s: Record<string, unknown> = {};
    if (this.opts.version) s.version = this.opts.version;
    if (this.opts.dockerLayerCaching) s.docker_layer_caching = this.opts.dockerLayerCaching;
    return { setup_remote_docker: s };
  }
}

export function setupRemoteDocker(opts?: { version?: string; dockerLayerCaching?: boolean }): CommandStep {
  return new SetupRemoteDockerCommand(opts);
}

// ─── When / Unless (conditional steps) ───────────────────────────────────────

class WhenCommand implements CommandStep {
  constructor(private condition: string, private steps: CommandStep[]) {}
  serialize(): Record<string, unknown> {
    return { when: { condition: this.condition, steps: this.steps.map((s) => s.serialize()) } };
  }
}

class UnlessCommand implements CommandStep {
  constructor(private condition: string, private steps: CommandStep[]) {}
  serialize(): Record<string, unknown> {
    return { unless: { condition: this.condition, steps: this.steps.map((s) => s.serialize()) } };
  }
}

export function when(condition: string, steps: CommandStep[]): CommandStep {
  return new WhenCommand(condition, steps);
}

export function unless(condition: string, steps: CommandStep[]): CommandStep {
  return new UnlessCommand(condition, steps);
}
