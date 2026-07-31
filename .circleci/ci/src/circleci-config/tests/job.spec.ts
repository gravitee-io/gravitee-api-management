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
import { Checkout, Run } from '../commands';
import { DockerExecutor } from '../executors';
import { Job } from '../job';

describe('Job', () => {
  it('emits the executor fragment then the steps', () => {
    const job = new Job('job-setup', new DockerExecutor('cimg/base:stable', 'small'), [new Checkout()]);
    expect(job.generate()).toStrictEqual({
      'job-setup': {
        docker: [{ image: 'cimg/base:stable' }],
        resource_class: 'small',
        steps: ['checkout'],
      },
    });
  });

  it('appends optional properties after the steps', () => {
    const job = new Job('job-test-integration', new DockerExecutor('cimg/openjdk:21', 'medium'), [new Run({ command: 'mvn test' })], {
      parallelism: 3,
    });
    const body = job.generate()['job-test-integration'] as Record<string, unknown>;
    expect(Object.keys(body)).toStrictEqual(['docker', 'resource_class', 'steps', 'parallelism']);
    expect(body.parallelism).toBe(3);
  });
});
