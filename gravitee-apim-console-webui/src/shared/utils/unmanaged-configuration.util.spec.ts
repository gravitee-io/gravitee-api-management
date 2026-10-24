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
import { GioJsonSchema } from '@gravitee/ui-particles-angular';

import { keepUndeclaredConfiguration } from './unmanaged-configuration.util';

describe('keepUndeclaredConfiguration', () => {
  const schema: GioJsonSchema = {
    type: 'object',
    properties: {
      target: { type: 'string' },
      upstreamAuth: { type: 'object', properties: { type: { type: 'string' }, token: { type: 'string' }, username: { type: 'string' } } },
    },
  };

  it('should keep stored properties the schema does not declare', () => {
    const stored = { target: 'https://old', specification: { tools: [{ name: 'get_weather' }] } };

    expect(keepUndeclaredConfiguration(schema, stored, { target: 'https://new' })).toEqual({
      target: 'https://new',
      specification: { tools: [{ name: 'get_weather' }] },
    });
  });

  it('should take a declared property from the form as a whole', () => {
    const stored = { target: 'https://b', upstreamAuth: { type: 'bearer', token: 'secret' } };
    const formValue = { target: 'https://b', upstreamAuth: { type: 'basic', username: 'jdoe' } };

    expect(keepUndeclaredConfiguration(schema, stored, formValue)).toEqual(formValue);
  });

  it('should drop a declared property the form no longer has', () => {
    const stored = { target: 'https://b', upstreamAuth: { type: 'bearer', token: 'secret' }, specification: {} };

    expect(keepUndeclaredConfiguration(schema, stored, { target: 'https://b' })).toEqual({ target: 'https://b', specification: {} });
  });

  it.each(['$ref', 'allOf', 'anyOf', 'oneOf', 'if', 'then', 'else', 'dependencies', 'additionalProperties', 'patternProperties'])(
    'should return the form value unchanged when the schema uses %s',
    keyword => {
      const complexSchema = { ...schema, [keyword]: {} } as GioJsonSchema;

      expect(keepUndeclaredConfiguration(complexSchema, { target: 'old', specification: {} }, { target: 'new' })).toEqual({
        target: 'new',
      });
    },
  );

  it.each([
    ['the schema is missing', null, { specification: {} }, { target: 'new' }],
    ['the stored configuration is missing', schema, undefined, { target: 'new' }],
    ['the stored configuration is not an object', schema, 'raw', { target: 'new' }],
    ['the form value is not an object', schema, { specification: {} }, null],
  ])('should return the form value unchanged when %s', (_, givenSchema, stored, formValue) => {
    expect(keepUndeclaredConfiguration(givenSchema, stored, formValue)).toEqual(formValue);
  });
});
