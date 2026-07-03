/*
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import { Checkout } from '../commands';
import { DockerExecutor } from '../executors';
import { Job } from '../job';
import { OrbImport, OrbRef } from '../orb';
import { CustomParametersList } from '../parameters';
import { Workflow, WorkflowJob } from '../workflow';

const job = new Job('job-danger-js', new DockerExecutor('cimg/node:22', 'small'), [new Checkout()]);

describe('WorkflowJob', () => {
  it('emits the bare name when it has no options', () => {
    expect(new WorkflowJob(job).generate()).toBe('job-danger-js');
  });

  it('preserves the insertion order of its options', () => {
    const generated = new WorkflowJob(job, {
      name: 'Sonar',
      context: ['cicd-orchestrator'],
      requires: ['Build backend'],
      working_directory: 'gravitee-apim-definition',
      cache_type: 'backend',
    }).generate() as Record<string, unknown>;
    expect(Object.keys(generated['job-danger-js'] as object)).toStrictEqual([
      'name',
      'context',
      'requires',
      'working_directory',
      'cache_type',
    ]);
  });

  it('renames preSteps to pre-steps and generates them', () => {
    const generated = new WorkflowJob(job, { context: ['ctx'], preSteps: [new Checkout()] }).generate() as Record<string, unknown>;
    expect(generated['job-danger-js']).toStrictEqual({
      context: ['ctx'],
      'pre-steps': ['checkout'],
    });
  });

  it('wraps the matrix values under parameters', () => {
    const generated = new WorkflowJob(job, {
      name: 'E2E',
      matrix: { execution_mode: ['v3'], database: ['mongo', 'jdbc'] },
    }).generate() as Record<string, unknown>;
    expect((generated['job-danger-js'] as Record<string, unknown>).matrix).toStrictEqual({
      parameters: { execution_mode: ['v3'], database: ['mongo', 'jdbc'] },
    });
  });

  it('keys the entry by the orb job qualified name', () => {
    const aquasec = new OrbImport('aquasec', 'gravitee-io', 'aquasec', '1.0.5');
    aquasec.jobs.fs_scan = new OrbRef('fs_scan', new CustomParametersList(), aquasec);
    const generated = new WorkflowJob(aquasec.jobs.fs_scan, { context: ['ctx'] }).generate() as Record<string, unknown>;
    expect(Object.keys(generated)).toStrictEqual(['aquasec/fs_scan']);
  });
});

describe('Workflow', () => {
  it('emits the workflow name mapped to its jobs', () => {
    expect(new Workflow('pull_requests', [new WorkflowJob(job)]).generate()).toStrictEqual({
      pull_requests: { jobs: ['job-danger-js'] },
    });
  });
});
