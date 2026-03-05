import { describe, it, expect } from 'vitest';
import { job, serializeJob } from '../../src/sdk/job.js';
import { docker } from '../../src/sdk/executors.js';
import { run, checkout } from '../../src/sdk/commands.js';
import { param } from '../../src/sdk/parameters.js';

describe('job', () => {
  it('serializes a simple job', () => {
    const j = job('build', {
      executor: docker('cimg/node:20', 'large'),
      steps: [checkout(), run('Build', 'npm run build')],
    });
    expect(serializeJob(j)).toMatchSnapshot();
  });

  it('serializes a parameterized job', () => {
    const j = job('test', {
      executor: docker('cimg/node:20', 'medium'),
      parameters: {
        module: param.string('', 'Module to test'),
      },
      steps: [run('Test', 'npm test -- << parameters.module >>')],
      parallelism: 3,
    });
    expect(serializeJob(j)).toMatchSnapshot();
  });
});
