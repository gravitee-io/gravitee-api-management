/**
 * YAML serialization for CircleCI config.
 */
import { stringify } from 'yaml';

import { JobDefinition, serializeJob } from './job.js';
import { WorkflowDefinition, serializeWorkflow } from './workflow.js';
import { Orb, serializeOrbs } from './orbs.js';
import { ParameterDefinition, serializeParameters } from './parameters.js';
import { ReusableCommandDefinition, serializeReusableCommand } from './reusable.js';

export interface CircleCIConfig {
  version: '2.1';
  setup?: boolean;
  parameters?: Record<string, ParameterDefinition>;
  orbs?: Orb[];
  commands?: ReusableCommandDefinition[];
  jobs?: JobDefinition[];
  workflows?: WorkflowDefinition[];
}

export function serializeConfig(cfg: CircleCIConfig): Record<string, unknown> {
  const result: Record<string, unknown> = { version: cfg.version };

  if (cfg.setup) result.setup = cfg.setup;

  if (cfg.parameters) {
    result.parameters = serializeParameters(cfg.parameters);
  }

  if (cfg.orbs && cfg.orbs.length > 0) {
    result.orbs = serializeOrbs(cfg.orbs);
  }

  if (cfg.commands && cfg.commands.length > 0) {
    const cmds: Record<string, unknown> = {};
    for (const cmd of cfg.commands) {
      cmds[cmd.name] = serializeReusableCommand(cmd);
    }
    result.commands = cmds;
  }

  if (cfg.jobs && cfg.jobs.length > 0) {
    const jobs: Record<string, unknown> = {};
    for (const j of cfg.jobs) {
      jobs[j.name] = serializeJob(j);
    }
    result.jobs = jobs;
  }

  if (cfg.workflows && cfg.workflows.length > 0) {
    const workflows: Record<string, unknown> = {};
    for (const w of cfg.workflows) {
      workflows[w.name] = serializeWorkflow(w);
    }
    result.workflows = workflows;
  }

  return result;
}

export function toYAML(cfg: CircleCIConfig): string {
  return stringify(serializeConfig(cfg), {
    lineWidth: 0,
    defaultKeyType: 'PLAIN',
    defaultStringType: 'PLAIN',
    nullStr: '',
    singleQuote: true,
  });
}
