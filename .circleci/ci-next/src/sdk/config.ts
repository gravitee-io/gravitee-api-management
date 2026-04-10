/**
 * CircleCI config root builder.
 */
import { JobDefinition } from './job.js';
import { WorkflowDefinition } from './workflow.js';
import { Orb } from './orbs.js';
import { ParameterDefinition } from './parameters.js';
import { ReusableCommandDefinition } from './reusable.js';
import { CircleCIConfig, toYAML, serializeConfig } from './serialize.js';

export interface ConfigOptions {
  setup?: boolean;
  parameters?: Record<string, ParameterDefinition>;
  orbs?: Orb[];
  commands?: ReusableCommandDefinition[];
  jobs?: JobDefinition[];
  workflows?: WorkflowDefinition[];
}

export interface Config {
  readonly raw: CircleCIConfig;
  toYAML(): string;
  toJSON(): Record<string, unknown>;
}

/**
 * Build a CircleCI config.
 *
 * @example
 * const cfg = config({
 *   orbs: [keeper],
 *   jobs: [setupJob, buildJob],
 *   workflows: [prWorkflow],
 * });
 * console.log(cfg.toYAML());
 */
export function config(opts: ConfigOptions = {}): Config {
  const raw: CircleCIConfig = {
    version: '2.1',
    ...opts,
  };

  return {
    raw,
    toYAML: () => toYAML(raw),
    toJSON: () => serializeConfig(raw),
  };
}
