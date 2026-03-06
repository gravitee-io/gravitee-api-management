/**
 * CircleCI orb import & usage.
 */
import { CommandStep } from './commands.js';

export interface OrbCommandDef {
  [paramName: string]: string; // parameter name → type (for documentation)
}

export interface OrbDefinition {
  ref: string; // e.g. 'gravitee-io/keeper@0.7.0'
  commands?: Record<string, OrbCommandDef>;
  jobs?: Record<string, OrbCommandDef>;
  executors?: Record<string, Record<string, unknown>>;
}

export interface Orb {
  alias: string;
  ref: string;
  commands: Record<string, OrbCommandDef>;
  jobs: Record<string, OrbCommandDef>;
  executors: Record<string, Record<string, unknown>>;
}

/**
 * Import an orb.
 *
 * @example
 * const keeper = orb('keeper', 'gravitee-io/keeper@0.7.0', {
 *   commands: {
 *     'env-export': { 'secret-url': 'string', 'var-name': 'string' },
 *     'install': {},
 *   },
 * });
 */
export function orb(alias: string, ref: string, def?: Partial<OrbDefinition>): Orb {
  return {
    alias,
    ref,
    commands: def?.commands ?? {},
    jobs: def?.jobs ?? {},
    executors: def?.executors ?? {},
  };
}

export function serializeOrbs(orbs: Orb[]): Record<string, string> {
  const result: Record<string, string> = {};
  for (const o of orbs) {
    result[o.alias] = o.ref;
  }
  return result;
}

// ─── Orb Command / Job usage ─────────────────────────────────────────────────

class OrbCommandStep implements CommandStep {
  constructor(
    private orbAlias: string,
    private commandName: string,
    private params?: Record<string, unknown>,
  ) {}
  serialize(): Record<string, unknown> {
    const key = `${this.orbAlias}/${this.commandName}`;
    if (this.params && Object.keys(this.params).length > 0) {
      return { [key]: this.params };
    }
    return { [key]: {} };
  }
}

/**
 * Use an orb command as a step.
 *
 * @example
 * use(keeper, 'env-export', { 'secret-url': '...', 'var-name': 'TOKEN' })
 */
export function useOrb(o: Orb, commandName: string, params?: Record<string, unknown>): CommandStep {
  return new OrbCommandStep(o.alias, commandName, params);
}

// ─── Orb Job reference (for workflow jobs) ───────────────────────────────────

export interface OrbJobRef {
  kind: 'orb-job';
  orb: Orb;
  jobName: string;
  name: string; // The serialized key is orb/jobName
}

export function orbJob(o: Orb, jobName: string): OrbJobRef {
  return { kind: 'orb-job', orb: o, jobName, name: `${o.alias}/${jobName}` };
}
