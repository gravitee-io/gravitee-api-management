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
import { CustomEnumParameter, CustomParameter, CustomParametersList } from '../parameters';

describe('CustomParameter', () => {
  it('emits type, default and description', () => {
    const parameter = new CustomParameter('dry_run', 'boolean', true, 'Run in dry run mode?');
    expect(parameter.generate()).toStrictEqual({
      type: 'boolean',
      default: true,
      description: 'Run in dry run mode?',
    });
  });

  it('keeps an explicit empty-string description', () => {
    const parameter = new CustomParameter('gio_action', 'string', 'pull_requests', '');
    expect(parameter.generate()).toStrictEqual({
      type: 'string',
      default: 'pull_requests',
      description: '',
    });
  });

  it('omits default and description when not provided', () => {
    const parameter = new CustomParameter('database', 'string');
    expect(parameter.generate()).toStrictEqual({
      type: 'string',
    });
  });

  it('keeps an empty-string default', () => {
    const parameter = new CustomParameter('jobName', 'string', '', 'The job name');
    expect(parameter.generate()).toStrictEqual({
      type: 'string',
      default: '',
      description: 'The job name',
    });
  });
});

describe('CustomEnumParameter', () => {
  it('emits type enum, default, description and enum values in order', () => {
    const parameter = new CustomEnumParameter('cache_type', ['backend', 'frontend'], 'backend', 'Type of cache to use');
    expect(Object.keys(parameter.generate())).toStrictEqual(['type', 'default', 'description', 'enum']);
    expect(parameter.generate()).toStrictEqual({
      type: 'enum',
      default: 'backend',
      description: 'Type of cache to use',
      enum: ['backend', 'frontend'],
    });
  });
});

describe('CustomParametersList', () => {
  it('emits a map keyed by parameter name, preserving order', () => {
    const list = new CustomParametersList([
      new CustomParameter('working_directory', 'string', 'gravitee-apim-rest-api', 'Directory'),
      new CustomEnumParameter('cache_type', ['backend', 'frontend'], 'backend', 'Type of cache to use'),
    ]);
    expect(Object.keys(list.generate())).toStrictEqual(['working_directory', 'cache_type']);
    expect(list.generate()).toStrictEqual({
      working_directory: { type: 'string', default: 'gravitee-apim-rest-api', description: 'Directory' },
      cache_type: { type: 'enum', default: 'backend', description: 'Type of cache to use', enum: ['backend', 'frontend'] },
    });
  });

  it('emits an empty map when there are no parameters', () => {
    expect(new CustomParametersList().generate()).toStrictEqual({});
  });
});
