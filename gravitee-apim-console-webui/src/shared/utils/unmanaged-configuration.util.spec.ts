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
import { preserveUnmanagedConfiguration } from './unmanaged-configuration.util';

describe('preserveUnmanagedConfiguration', () => {
  const schema = {
    type: 'object',
    properties: {
      target: { type: 'string' },
      upstreamAuth: {
        type: 'object',
        properties: { type: { type: 'string' }, token: { type: 'string' }, username: { type: 'string' } },
      },
    },
  };

  it('should keep stored properties the schema does not declare', () => {
    const stored = { target: 'https://old', specification: { tools: [{ name: 'get_weather' }] } };

    expect(preserveUnmanagedConfiguration(schema, stored, { target: 'https://new' })).toEqual({
      target: 'https://new',
      specification: { tools: [{ name: 'get_weather' }] },
    });
  });

  it('should replace a declared property with the form value rather than merging into it', () => {
    const stored = { target: 'https://b', upstreamAuth: { type: 'bearer', token: 'secret' } };

    expect(
      preserveUnmanagedConfiguration(schema, stored, { target: 'https://b', upstreamAuth: { type: 'basic', username: 'jdoe' } }),
    ).toEqual({
      target: 'https://b',
      upstreamAuth: { type: 'basic', username: 'jdoe' },
    });
  });

  it('should drop a declared property the form no longer has', () => {
    const stored = { target: 'https://b', upstreamAuth: { type: 'bearer', token: 'secret' }, specification: {} };

    expect(preserveUnmanagedConfiguration(schema, stored, { target: 'https://b' })).toEqual({ target: 'https://b', specification: {} });
  });

  it('should treat properties declared through $ref, combinators and conditionals as declared', () => {
    const composedSchema = {
      definitions: { base: { properties: { target: { type: 'string' } } } },
      allOf: [{ $ref: '#/definitions/base' }],
      oneOf: [{ properties: { a: { type: 'string' } } }],
      anyOf: [{ properties: { b: { type: 'string' } } }],
      if: { properties: { c: { const: 'x' } } },
      then: { properties: { d: { type: 'string' } } },
      else: { properties: { e: { type: 'string' } } },
    };
    const stored = { target: 't', a: 1, b: 2, c: 3, d: 4, e: 5, unmanaged: true };

    expect(preserveUnmanagedConfiguration(composedSchema, stored, {})).toEqual({ unmanaged: true });
  });

  it('should resolve a top level $ref', () => {
    const refSchema = { $ref: '#/definitions/config', definitions: { config: { properties: { target: { type: 'string' } } } } };

    expect(preserveUnmanagedConfiguration(refSchema, { target: 'old', unmanaged: true }, { target: 'new' })).toEqual({
      target: 'new',
      unmanaged: true,
    });
  });

  it('should not loop on a recursive $ref', () => {
    const recursiveSchema = { $ref: '#/definitions/node', definitions: { node: { allOf: [{ $ref: '#/definitions/node' }] } } };

    expect(preserveUnmanagedConfiguration(recursiveSchema, { unmanaged: true }, {})).toEqual({ unmanaged: true });
  });

  it.each([
    ['additionalProperties is false', { properties: { target: {} }, additionalProperties: false }],
    ['additionalProperties is true', { properties: { target: {} }, additionalProperties: true }],
    ['additionalProperties is a schema', { properties: { target: {} }, additionalProperties: { type: 'string' } }],
    ['patternProperties is set', { properties: { target: {} }, patternProperties: { '^x-': { type: 'string' } } }],
    ['a combinator constrains undeclared properties', { allOf: [{ additionalProperties: false }] }],
    ['a $ref points outside the schema', { allOf: [{ $ref: 'https://example.com/schema.json' }], properties: { target: {} } }],
    ['a $ref points to a missing definition', { allOf: [{ $ref: '#/definitions/missing' }], properties: { target: {} } }],
  ])('should return the form value unchanged when %s', (_, openSchema) => {
    expect(preserveUnmanagedConfiguration(openSchema, { target: 'old', unmanaged: true }, { target: 'new' })).toEqual({ target: 'new' });
  });

  it.each([
    ['the schema is missing', null, { unmanaged: true }, { target: 'new' }],
    ['the stored configuration is not an object', schema, 'raw', { target: 'new' }],
    ['the stored configuration is missing', schema, undefined, { target: 'new' }],
    ['the form value is not an object', schema, { unmanaged: true }, null],
  ])('should return the form value unchanged when %s', (_, givenSchema, stored, formValue) => {
    expect(preserveUnmanagedConfiguration(givenSchema, stored, formValue)).toEqual(formValue);
  });
});
