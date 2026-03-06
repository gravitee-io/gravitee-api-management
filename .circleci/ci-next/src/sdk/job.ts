/**
 * CircleCI job builder.
 */
import { CommandStep } from './commands.js';
import { Executor, serializeExecutor } from './executors.js';
import { ParameterDefinition, serializeParameters } from './parameters.js';

export interface JobDefinition {
  name: string;
  executor: Executor;
  steps: CommandStep[];
  parameters?: Record<string, ParameterDefinition>;
  environment?: Record<string, string | number | boolean>;
  workingDirectory?: string;
  shell?: string;
  parallelism?: number;
}

/**
 * Create a job.
 *
 * @example
 * const build = job('build-backend', {
 *   executor: docker('cimg/openjdk:21.0.5', 'xlarge'),
 *   steps: [checkout(), run('Build', 'mvn install')],
 * });
 */
export function job(name: string, opts: Omit<JobDefinition, 'name'>): JobDefinition {
  return { name, ...opts };
}

export function serializeJob(j: JobDefinition): Record<string, unknown> {
  const result: Record<string, unknown> = {
    ...serializeExecutor(j.executor),
    steps: j.steps.map((s) => s.serialize()),
  };
  if (j.parameters) result.parameters = serializeParameters(j.parameters);
  if (j.environment) result.environment = j.environment;
  if (j.workingDirectory) result.working_directory = j.workingDirectory;
  if (j.shell) result.shell = j.shell;
  if (j.parallelism) result.parallelism = j.parallelism;
  return result;
}
