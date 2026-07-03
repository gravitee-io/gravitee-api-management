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
import { DockerExecutor, MachineExecutor } from '../executors';

describe('DockerExecutor', () => {
  it('emits a docker image list and its resource class', () => {
    expect(new DockerExecutor('cimg/openjdk:21.0.9', 'small').generate()).toStrictEqual({
      docker: [{ image: 'cimg/openjdk:21.0.9' }],
      resource_class: 'small',
    });
  });

  it('defaults the resource class to medium', () => {
    expect(new DockerExecutor('cimg/base:stable').generate()).toStrictEqual({
      docker: [{ image: 'cimg/base:stable' }],
      resource_class: 'medium',
    });
  });
});

describe('MachineExecutor', () => {
  it('emits a machine image, docker layer caching flag and resource class', () => {
    expect(new MachineExecutor('large', 'ubuntu-2204:current', false).generate()).toStrictEqual({
      machine: {
        image: 'ubuntu-2204:current',
        docker_layer_caching: false,
      },
      resource_class: 'large',
    });
  });
});
