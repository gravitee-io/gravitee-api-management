/**
 * Reusable commands — define once, use many times.
 */
import { CommandStep } from './commands.js';
import { ParameterDefinition, serializeParameters } from './parameters.js';

export interface ReusableCommandDefinition {
  name: string;
  description?: string;
  parameters?: Record<string, ParameterDefinition>;
  steps: CommandStep[];
}

/**
 * Define a reusable command.
 *
 * @example
 * const restoreCache = reusableCommand('restore-maven-cache', [
 *   cache.restore({ keys: ['maven-{{ checksum "pom.xml" }}'] }),
 * ]);
 */
export function reusableCommand(
  name: string,
  steps: CommandStep[],
  opts?: { parameters?: Record<string, ParameterDefinition>; description?: string },
): ReusableCommandDefinition {
  return {
    name,
    steps,
    ...(opts?.parameters && { parameters: opts.parameters }),
    ...(opts?.description && { description: opts.description }),
  };
}

export function serializeReusableCommand(cmd: ReusableCommandDefinition): Record<string, unknown> {
  const result: Record<string, unknown> = {
    steps: cmd.steps.map((s) => s.serialize()),
  };
  if (cmd.parameters) result.parameters = serializeParameters(cmd.parameters);
  if (cmd.description) result.description = cmd.description;
  return result;
}

// ─── Using a reusable command as a step ──────────────────────────────────────

class UseReusableCommandStep implements CommandStep {
  constructor(
    private cmd: ReusableCommandDefinition,
    private params?: Record<string, unknown>,
  ) {}
  serialize(): Record<string, unknown> {
    if (this.params && Object.keys(this.params).length > 0) {
      return { [this.cmd.name]: this.params };
    }
    return { [this.cmd.name]: {} };
  }
}

/**
 * Use a reusable command as a step in a job.
 *
 * @example
 * use(restoreMavenCache, { jobName: 'build' })
 */
export function use(cmd: ReusableCommandDefinition, params?: Record<string, unknown>): CommandStep {
  return new UseReusableCommandStep(cmd, params);
}
