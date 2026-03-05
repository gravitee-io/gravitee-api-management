/**
 * CircleCI workflow & workflow job.
 */
import { JobDefinition } from './job.js';
import { OrbJobRef } from './orbs.js';

export interface WorkflowJobOptions {
  name?: string;
  requires?: string[];
  context?: string | string[];
  filters?: {
    branches?: { only?: string | string[]; ignore?: string | string[] };
    tags?: { only?: string | string[]; ignore?: string | string[] };
  };
  matrix?: Record<string, unknown[]>;
  // Parameterized job values — passed directly as keys
  [key: string]: unknown;
}

export interface WorkflowJobEntry {
  jobRef: JobDefinition | OrbJobRef;
  options?: WorkflowJobOptions;
}

/**
 * Create a workflow job entry.
 *
 * @example
 * wfJob(buildJob, { requires: ['setup'], context: 'cicd-orchestrator' })
 */
export function wfJob(jobRef: JobDefinition | OrbJobRef, options?: WorkflowJobOptions): WorkflowJobEntry {
  return { jobRef, options };
}

export interface WorkflowDefinition {
  name: string;
  jobs: WorkflowJobEntry[];
  when?: unknown;
  unless?: unknown;
}

/**
 * Create a workflow.
 *
 * @example
 * const pr = workflow('pull_requests', [
 *   wfJob(setupJob, { context: 'cicd-orchestrator' }),
 *   wfJob(buildJob, { requires: ['Setup'] }),
 * ]);
 */
export function workflow(name: string, jobs: WorkflowJobEntry[], condition?: { when?: unknown; unless?: unknown }): WorkflowDefinition {
  return { name, jobs, ...condition };
}

function getJobKey(entry: WorkflowJobEntry): string {
  if ('kind' in entry.jobRef && entry.jobRef.kind === 'orb-job') {
    return entry.jobRef.name;
  }
  return (entry.jobRef as JobDefinition).name;
}

export function serializeWorkflow(wf: WorkflowDefinition): Record<string, unknown> {
  const result: Record<string, unknown> = {
    jobs: wf.jobs.map((entry) => {
      const key = getJobKey(entry);
      if (!entry.options || Object.keys(entry.options).length === 0) {
        return key;
      }
      const opts: Record<string, unknown> = {};
      const { name, requires, context, filters, matrix, ...rest } = entry.options;
      if (name) opts.name = name;
      if (requires) opts.requires = requires;
      if (context) opts.context = context;
      if (filters) opts.filters = filters;
      if (matrix) opts.matrix = { parameters: matrix };
      // Pass-through any extra params (for parameterized jobs)
      // Serialize CommandStep arrays (pre-steps, post-steps, etc.)
      for (const [k, v] of Object.entries(rest)) {
        if (Array.isArray(v) && v.length > 0 && typeof v[0]?.serialize === 'function') {
          opts[k] = v.map((step: { serialize(): Record<string, unknown> }) => step.serialize());
        } else {
          opts[k] = v;
        }
      }
      return { [key]: opts };
    }),
  };
  if (wf.when !== undefined) result.when = wf.when;
  if (wf.unless !== undefined) result.unless = wf.unless;
  return result;
}
