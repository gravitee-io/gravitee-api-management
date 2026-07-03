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
import { Run } from '../commands';
import { DockerExecutor } from '../executors';
import { OrbImport } from '../orb';
import { CustomParameter, CustomParametersList } from '../parameters';
import { ParameterizedJob, ReusableCommand, ReusedCommand } from '../reusable';

describe('ReusableCommand', () => {
  it('emits steps only when it has no parameters nor description', () => {
    const command = new ReusableCommand('cmd-install-yarn', [new Run({ name: 'Yarn', command: 'corepack enable' })]);
    expect(command.generate()).toStrictEqual({
      'cmd-install-yarn': {
        steps: [{ run: { name: 'Yarn', command: 'corepack enable' } }],
      },
    });
  });

  it('emits parameters, steps and description in order', () => {
    const command = new ReusableCommand(
      'cmd-restore-maven-job-cache',
      [new Run({ command: 'restore' })],
      new CustomParametersList([new CustomParameter('jobName', 'string', '', 'The job name')]),
      'Restore Maven cache for a dedicated job',
    );
    const generated = command.generate()['cmd-restore-maven-job-cache'] as Record<string, unknown>;
    expect(Object.keys(generated)).toStrictEqual(['parameters', 'steps', 'description']);
  });
});

describe('ReusedCommand', () => {
  const yarn = new ReusableCommand('cmd-install-yarn', [new Run({ command: 'corepack enable' })]);

  it('emits the bare name when there is no argument', () => {
    expect(new ReusedCommand(yarn).generate()).toBe('cmd-install-yarn');
  });

  it('emits name mapped to its arguments otherwise', () => {
    const keeper = new OrbImport('keeper', 'gravitee-io', 'keeper', '0.7.0', undefined, {
      commands: { 'env-export': new CustomParametersList([new CustomParameter('var-name', 'string')]) },
    });
    expect(new ReusedCommand(keeper.commands['env-export'], { 'var-name': 'TOKEN' }).generate()).toStrictEqual({
      'keeper/env-export': { 'var-name': 'TOKEN' },
    });
  });
});

describe('ParameterizedJob', () => {
  it('emits parameters, the executor fragment and steps', () => {
    const job = new ParameterizedJob(
      'job-sonarcloud-analysis',
      new DockerExecutor('sonarsource/sonar-scanner-cli:11.2', 'large'),
      new CustomParametersList([new CustomParameter('working_directory', 'string', 'gravitee-apim-rest-api', 'Directory')]),
      [new Run({ name: 'Run Sonarcloud Analysis', command: 'sonar-scanner' })],
    );
    expect(job.generate()).toStrictEqual({
      'job-sonarcloud-analysis': {
        parameters: { working_directory: { type: 'string', default: 'gravitee-apim-rest-api', description: 'Directory' } },
        docker: [{ image: 'sonarsource/sonar-scanner-cli:11.2' }],
        resource_class: 'large',
        steps: [{ run: { name: 'Run Sonarcloud Analysis', command: 'sonar-scanner' } }],
      },
    });
  });
});
