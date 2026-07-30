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
import { generatePullRequestsConfig } from '../pipeline-pull-requests';

/**
 * The generated-config snapshots pin the current output, not the rule behind it.
 * This encodes the rule: machine executors ship an older JDK than the one the
 * project builds with, so any machine job invoking Maven has to install it first.
 *
 * Without this, a machine job added later gets a green snapshot diff and runs
 * silently on the VM's JDK — the same class of failure as a rule that reports
 * success while checking nothing.
 */
describe('JDK on machine executors', () => {
  const INSTALL_JDK_COMMAND = 'cmd-install-jdk';

  function generatedJobs(): Record<string, { machine?: unknown; steps?: unknown[] }> {
    const result = generatePullRequestsConfig({
      action: 'pull_requests',
      apimVersionPath: './src/pipelines/tests/resources/common/pom.xml',
      baseBranch: 'master',
      branch: 'master',
      sha1: '784ff35ca',
      changedFiles: ['pom.xml'],
      buildNum: '1234',
      buildId: '1234',
      isDryRun: false,
      graviteeioVersion: '',
    });

    return parse(result.stringify()).jobs ?? {};
  }

  /** Flattens a job's steps into the shell it runs plus the reusable commands it invokes. */
  function stepsOf(job: { steps?: unknown[] }): { commands: string[]; shell: string } {
    const commands: string[] = [];
    let shell = '';

    for (const step of job.steps ?? []) {
      if (typeof step === 'string') {
        commands.push(step);
        continue;
      }
      for (const [name, value] of Object.entries(step as Record<string, unknown>)) {
        commands.push(name);
        if (name === 'run') {
          shell += typeof value === 'string' ? value : ((value as { command?: string })?.command ?? '');
          shell += '\n';
        }
      }
    }

    return { commands, shell };
  }

  it('installs the JDK in every machine job that runs Maven', () => {
    const jobs = Object.entries(generatedJobs()).filter(([, job]) => job.machine !== undefined);
    expect(jobs.length).toBeGreaterThan(0);

    const missing = jobs
      .filter(([, job]) => {
        const { commands, shell } = stepsOf(job);
        return /\bmvn\b/.test(shell) && !commands.includes(INSTALL_JDK_COMMAND);
      })
      .map(([name]) => name);

    expect(missing).toStrictEqual([]);
  });

  it('does not install the JDK in machine jobs that never run Maven', () => {
    const superfluous = Object.entries(generatedJobs())
      .filter(([, job]) => job.machine !== undefined)
      .filter(([, job]) => {
        const { commands, shell } = stepsOf(job);
        return !/\bmvn\b/.test(shell) && commands.includes(INSTALL_JDK_COMMAND);
      })
      .map(([name]) => name);

    expect(superfluous).toStrictEqual([]);
  });

  it('does not install the JDK in docker jobs, which get it from their image', () => {
    const superfluous = Object.entries(generatedJobs())
      .filter(([, job]) => job.machine === undefined)
      .filter(([, job]) => stepsOf(job).commands.includes(INSTALL_JDK_COMMAND))
      .map(([name]) => name);

    expect(superfluous).toStrictEqual([]);
  });
});
