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
import { generateRepositoriesTestsConfig } from '../pipeline-repositories-tests';
import { generateIntegrationTestsConfig } from '../pipeline-integration-tests';
import { config } from '../../config';

/**
 * The generated-config snapshots pin the current output, not the rule behind it.
 * This encodes the rule: resolving with the shared settings.xml needs a token, because
 * Gravitee.io Bot is a service principal and its token lives one hour, so each job fetches
 * its own.
 *
 * Without this, a job added later resolves with AZURE_ARTIFACTS_PAT unset, takes a 401 on
 * the feed and falls back to Artifactory — green, and blind to whether the feed serves
 * anything. That is how the first version of this change shipped with five jobs missing it.
 *
 * Four pipelines are generated on purpose. The four *-test-container jobs appear in none of
 * the pull-request configurations, and job-test-integration in only some of them: checking
 * pull-requests alone would pass over an empty set, which is the same "reports success while
 * checking nothing" failure the rule exists to prevent.
 */
describe('Azure Artifacts token', () => {
  const TOKEN_COMMAND = 'cmd-azure-artifacts-token';

  const baseEnvironment = {
    baseBranch: 'master',
    branch: 'master',
    sha1: '784ff35ca',
    buildNum: '1234',
    buildId: '1234',
    isDryRun: false,
  };

  type GeneratedJob = { steps?: unknown[] };

  /** Every job of every pipeline that can run Maven, keyed by "<pipeline>/<job>". */
  function generatedJobs(): Record<string, GeneratedJob> {
    const pipelines = {
      'pull-requests': generatePullRequestsConfig({
        ...baseEnvironment,
        action: 'pull_requests',
        apimVersionPath: './src/pipelines/tests/resources/common/pom.xml',
        changedFiles: ['pom.xml'],
        graviteeioVersion: '',
      }),
      // A backend change, which is what brings in the job-test-* family: a root pom.xml
      // change on master generates none of them.
      'pull-requests-backend': generatePullRequestsConfig({
        ...baseEnvironment,
        action: 'pull_requests',
        branch: 'APIM-1234-my-custom-branch',
        apimVersionPath: './src/pipelines/tests/resources/common/pom.xml',
        changedFiles: ['gravitee-apim-definition'],
        graviteeioVersion: '',
      }),
      'repositories-tests': generateRepositoriesTestsConfig({
        ...baseEnvironment,
        action: 'repositories_tests',
        apimVersionPath: './src/pipelines/tests/resources/common/pom.xml',
        changedFiles: [],
        graviteeioVersion: '4.2.0',
      }),
      'integration-tests': generateIntegrationTestsConfig({
        ...baseEnvironment,
        action: 'integration_tests',
        apimVersionPath: './src/pipelines/tests/resources/common/pom.xml',
        changedFiles: [],
        graviteeioVersion: '4.2.0',
      }),
    };

    const jobs: Record<string, GeneratedJob> = {};
    for (const [pipeline, generated] of Object.entries(pipelines)) {
      for (const [name, job] of Object.entries(parse(generated.stringify()).jobs ?? {})) {
        jobs[`${pipeline}/${name}`] = job as GeneratedJob;
      }
    }
    return jobs;
  }

  /** Flattens a job's steps into the shell it runs plus the reusable commands it invokes. */
  function stepsOf(job: GeneratedJob): { commands: string[]; shell: string } {
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

  /** Maven reaching the network, as opposed to `mvn versions:set` and friends. */
  function resolvesWithSharedSettings(shell: string): boolean {
    return new RegExp(`\\bmvn\\b[^\\n]*${config.maven.settingsFile.replaceAll('.', '\\.')}`).test(shell);
  }

  it('fetches a token in every job resolving with the shared settings', () => {
    const resolving = Object.entries(generatedJobs()).filter(([, job]) => resolvesWithSharedSettings(stepsOf(job).shell));

    // Guards against the check passing over an empty set.
    expect(resolving.length).toBeGreaterThan(15);

    const missing = resolving.filter(([, job]) => !stepsOf(job).commands.includes(TOKEN_COMMAND)).map(([name]) => name);

    expect(missing).toStrictEqual([]);
  });

  it('covers the jobs that only exist outside the pull-request pipelines', () => {
    const names = Object.keys(generatedJobs());

    // These are the ones the first version of this change missed; each lives in a pipeline
    // the pull-request configurations never generate.
    expect(names).toEqual(
      expect.arrayContaining([
        'repositories-tests/job-jdbc-test-container',
        'repositories-tests/job-mongo-test-container',
        'repositories-tests/job-elastic-test-container',
        'repositories-tests/job-redis-test-container',
        'integration-tests/job-test-integration',
      ]),
    );
  });

  it('does not fetch a token in jobs that never resolve with the shared settings', () => {
    const superfluous = Object.entries(generatedJobs())
      .filter(([, job]) => {
        const { commands, shell } = stepsOf(job);
        return !resolvesWithSharedSettings(shell) && commands.includes(TOKEN_COMMAND);
      })
      .map(([name]) => name);

    expect(superfluous).toStrictEqual([]);
  });
});
