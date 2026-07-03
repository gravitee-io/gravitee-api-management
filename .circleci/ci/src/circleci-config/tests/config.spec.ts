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
import { parse } from 'yaml';
import { Checkout, Run } from '../commands';
import { Config } from '../config';
import { DockerExecutor } from '../executors';
import { Job } from '../job';
import { OrbImport } from '../orb';
import { ReusableCommand } from '../reusable';
import { Workflow, WorkflowJob } from '../workflow';

function buildConfig(): Config {
  const config = new Config();
  const keeper = new OrbImport('keeper', 'gravitee-io', 'keeper', '0.7.0');
  const setupJob = new Job('job-setup', new DockerExecutor('cimg/base:stable', 'small'), [new Checkout()]);

  config.defineParameter('dry_run', 'boolean', true, 'Run in dry run mode?');
  config.importOrb(keeper);
  config.importOrb(keeper); // duplicate import must be deduplicated
  config.addReusableCommand(new ReusableCommand('cmd-install-yarn', [new Run({ name: 'Yarn', command: 'corepack enable' })]));
  config.addJob(setupJob);
  config.addJob(setupJob); // duplicate job must be deduplicated
  config.addWorkflow(new Workflow('pull_requests', [new WorkflowJob(setupJob, { name: 'Setup', context: ['cicd-orchestrator'] })]));
  return config;
}

describe('Config', () => {
  it('emits the sections in canonical order', () => {
    expect(Object.keys(buildConfig().generate())).toStrictEqual([
      'version',
      'setup',
      'parameters',
      'commands',
      'jobs',
      'workflows',
      'orbs',
    ]);
  });

  it('assembles and deduplicates every section', () => {
    expect(buildConfig().generate()).toStrictEqual({
      version: 2.1,
      setup: false,
      parameters: {
        dry_run: { type: 'boolean', default: true, description: 'Run in dry run mode?' },
      },
      commands: {
        'cmd-install-yarn': { steps: [{ run: { name: 'Yarn', command: 'corepack enable' } }] },
      },
      jobs: {
        'job-setup': {
          docker: [{ image: 'cimg/base:stable' }],
          resource_class: 'small',
          steps: ['checkout'],
        },
      },
      workflows: {
        pull_requests: {
          jobs: [{ 'job-setup': { name: 'Setup', context: ['cicd-orchestrator'] } }],
        },
      },
      orbs: { keeper: 'gravitee-io/keeper@0.7.0' },
    });
  });

  it('omits empty sections', () => {
    const config = new Config();
    config.addWorkflow(new Workflow('empty', []));
    expect(Object.keys(config.generate())).toStrictEqual(['version', 'setup', 'workflows']);
  });

  it('prepends a generated-file header and round-trips through YAML', () => {
    const config = buildConfig();
    const yaml = config.stringify();
    expect(yaml.startsWith('# This file is generated from the TypeScript sources in .circleci/ci.')).toBe(true);
    expect(parse(yaml)).toStrictEqual(config.generate());
  });
});
