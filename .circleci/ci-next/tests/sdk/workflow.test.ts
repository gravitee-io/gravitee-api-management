import { describe, it, expect } from 'vitest';
import { workflow, wfJob, serializeWorkflow } from '../../src/sdk/workflow.js';
import { job } from '../../src/sdk/job.js';
import { docker } from '../../src/sdk/executors.js';
import { run, checkout } from '../../src/sdk/commands.js';

describe('workflow', () => {
  const setupJob = job('setup', {
    executor: docker('cimg/base:stable', 'small'),
    steps: [checkout()],
  });

  const buildJob = job('build', {
    executor: docker('cimg/node:20', 'large'),
    steps: [run('Build', 'npm build')],
  });

  it('serializes a workflow with dependencies', () => {
    const wf = workflow('ci', [
      wfJob(setupJob, { name: 'Setup', context: 'my-context' }),
      wfJob(buildJob, { name: 'Build', requires: ['Setup'], context: ['ctx1', 'ctx2'] }),
    ]);
    expect(serializeWorkflow(wf)).toMatchSnapshot();
  });

  it('serializes a workflow with matrix', () => {
    const testJob = job('test', {
      executor: docker('cimg/node:20'),
      steps: [run('Test', 'npm test')],
    });
    const wf = workflow('test-matrix', [
      wfJob(testJob, {
        name: 'Test << matrix.db >>',
        matrix: { db: ['mongo', 'postgres'], mode: ['v3', 'v4'] },
      }),
    ]);
    expect(serializeWorkflow(wf)).toMatchSnapshot();
  });
});
