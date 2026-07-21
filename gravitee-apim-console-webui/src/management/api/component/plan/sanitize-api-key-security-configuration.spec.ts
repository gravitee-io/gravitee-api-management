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
import {
  DEFAULT_API_KEY_HEADER,
  sanitizeApiKeySecurityConfiguration,
  shouldStripApiKeyHeaderSchemaDefault,
  stripApiKeyHeaderSchemaDefault,
} from './sanitize-api-key-security-configuration';

describe('shouldStripApiKeyHeaderSchemaDefault', () => {
  it('does not strip on create so the default header is shown', () => {
    expect(shouldStripApiKeyHeaderSchemaDefault('create', {})).toBe(false);
    expect(shouldStripApiKeyHeaderSchemaDefault('create', { source: 'HEADER' })).toBe(false);
  });

  it('strips on edit when the stored plan has no apiKeyHeader', () => {
    expect(shouldStripApiKeyHeaderSchemaDefault('edit', { source: 'HEADER' })).toBe(true);
    expect(shouldStripApiKeyHeaderSchemaDefault('edit', { propagateApiKey: true })).toBe(true);
  });

  it('does not strip on edit when the stored plan already has apiKeyHeader', () => {
    expect(
      shouldStripApiKeyHeaderSchemaDefault('edit', {
        source: 'HEADER',
        apiKeyHeader: DEFAULT_API_KEY_HEADER,
      }),
    ).toBe(false);
  });

  it('strips on edit when stored configuration is nullish or not a plain object', () => {
    expect(shouldStripApiKeyHeaderSchemaDefault('edit', null)).toBe(true);
    expect(shouldStripApiKeyHeaderSchemaDefault('edit', undefined)).toBe(true);
    expect(shouldStripApiKeyHeaderSchemaDefault('edit', ['HEADER'])).toBe(true);
  });
});

describe('sanitizeApiKeySecurityConfiguration', () => {
  it('keeps the default header on create', () => {
    expect(
      sanitizeApiKeySecurityConfiguration({
        source: 'HEADER',
        apiKeyHeader: DEFAULT_API_KEY_HEADER,
      }),
    ).toEqual({
      source: 'HEADER',
      apiKeyHeader: DEFAULT_API_KEY_HEADER,
    });
  });

  it('keeps a custom header when enableCustomApiKeyHeader is true', () => {
    expect(
      sanitizeApiKeySecurityConfiguration({
        source: 'HEADER',
        enableCustomApiKeyHeader: true,
        apiKeyHeader: 'X-Custom-Header',
      }),
    ).toEqual({
      source: 'HEADER',
      enableCustomApiKeyHeader: true,
      apiKeyHeader: 'X-Custom-Header',
    });
  });

  it('keeps apiKeyHeader even when enableCustomApiKeyHeader is not set', () => {
    expect(
      sanitizeApiKeySecurityConfiguration({
        source: 'HEADER',
        apiKeyHeader: DEFAULT_API_KEY_HEADER,
      }),
    ).toEqual({
      source: 'HEADER',
      apiKeyHeader: DEFAULT_API_KEY_HEADER,
    });
  });

  it('removes cleared header values from export', () => {
    expect(
      sanitizeApiKeySecurityConfiguration({
        source: 'HEADER',
        apiKeyHeader: '',
        enableCustomApiKeyHeader: true,
      }),
    ).toEqual({ source: 'HEADER' });

    expect(
      sanitizeApiKeySecurityConfiguration({
        source: 'HEADER',
        apiKeyHeader: '   ',
      }),
    ).toEqual({ source: 'HEADER' });
  });

  it('preserves propagateApiKey and other unrelated fields', () => {
    expect(
      sanitizeApiKeySecurityConfiguration({
        source: 'HEADER',
        propagateApiKey: true,
        apiKeyHeader: DEFAULT_API_KEY_HEADER,
      }),
    ).toEqual({
      source: 'HEADER',
      propagateApiKey: true,
      apiKeyHeader: DEFAULT_API_KEY_HEADER,
    });
  });

  it('returns non-object values unchanged', () => {
    expect(sanitizeApiKeySecurityConfiguration(null)).toBeNull();
    expect(sanitizeApiKeySecurityConfiguration('value')).toBe('value');
    expect(sanitizeApiKeySecurityConfiguration(['HEADER'])).toEqual(['HEADER']);
  });
});

describe('stripApiKeyHeaderSchemaDefault', () => {
  it('removes apiKeyHeader default from the policy schema', () => {
    const schema = {
      type: 'object',
      properties: {
        apiKeyHeader: {
          type: 'string',
          default: DEFAULT_API_KEY_HEADER,
        },
      },
    };

    expect(stripApiKeyHeaderSchemaDefault(schema)).toEqual({
      type: 'object',
      properties: {
        apiKeyHeader: {
          type: 'string',
        },
      },
    });
  });

  it('returns the original schema when apiKeyHeader has no default', () => {
    const schema = {
      type: 'object',
      properties: {
        apiKeyHeader: { type: 'string' },
      },
    };

    expect(stripApiKeyHeaderSchemaDefault(schema)).toBe(schema);
  });

  it('returns non-object values unchanged', () => {
    expect(stripApiKeyHeaderSchemaDefault(null)).toBeNull();
    expect(stripApiKeyHeaderSchemaDefault(['object'])).toEqual(['object']);
  });

  it('does not mutate the original schema object', () => {
    const schema = {
      type: 'object',
      properties: {
        apiKeyHeader: {
          type: 'string',
          default: DEFAULT_API_KEY_HEADER,
        },
      },
    };

    stripApiKeyHeaderSchemaDefault(schema);

    expect(schema.properties.apiKeyHeader).toEqual({
      type: 'string',
      default: DEFAULT_API_KEY_HEADER,
    });
  });
});
