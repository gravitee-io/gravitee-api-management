import { describe, it, expect } from 'vitest';
import { config } from '../../src/sdk/config.js';
import { job } from '../../src/sdk/job.js';
import { workflow, wfJob } from '../../src/sdk/workflow.js';
import { docker } from '../../src/sdk/executors.js';
import { run, checkout } from '../../src/sdk/commands.js';
import { orb } from '../../src/sdk/orbs.js';
import { reusableCommand } from '../../src/sdk/reusable.js';
import { param } from '../../src/sdk/parameters.js';

describe('config', () => {
  it('generates a complete YAML config', () => {
    const keeper = orb('keeper', 'gravitee-io/keeper@0.7.0');
    const hello = reusableCommand('say-hello', [run('Hello', 'echo hello')]);
    const buildJob = job('build', {
      executor: docker('cimg/node:20', 'large'),
      steps: [checkout(), run('Build', 'npm build')],
    });
    const wf = workflow('ci', [
      wfJob(buildJob, { name: 'Build', context: 'my-ctx' }),
    ]);
    const cfg = config({
      parameters: { deploy: param.boolean(false) },
      orbs: [keeper],
      commands: [hello],
      jobs: [buildJob],
      workflows: [wf],
    });
    expect(cfg.toYAML()).toMatchSnapshot();
  });
});
