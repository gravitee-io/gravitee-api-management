/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import {
    filterResponseTemplates,
    fromResponseTemplates,
    isDuplicateKeyAccept,
    parseResponseTemplatePath,
    removeResponseTemplate,
    toResponseTemplatePath,
    toResponseTemplates,
    upsertResponseTemplate,
} from './responseTemplates';
import type { ResponseTemplatesMap } from '../types/responseTemplate';

describe('toResponseTemplates / fromResponseTemplates', () => {
    const nested: ResponseTemplatesMap = {
        DEFAULT: {
            'application/json': {
                statusCode: 400,
                body: '{"error":true}',
                headers: { 'X-Error': '1' },
                propagateErrorKeyToLogs: true,
            },
        },
        API_KEY_MISSING: {
            '*/*': { statusCode: 401 },
        },
    };

    it('flattens nested maps into rows with unambiguous path ids', () => {
        expect(toResponseTemplates(nested)).toEqual([
            {
                id: 'DEFAULT/application%2Fjson',
                key: 'DEFAULT',
                contentType: 'application/json',
                statusCode: 400,
                body: '{"error":true}',
                headers: { 'X-Error': '1' },
                propagateErrorKeyToLogs: true,
            },
            {
                id: 'API_KEY_MISSING/%2A%2F%2A',
                key: 'API_KEY_MISSING',
                contentType: '*/*',
                statusCode: 401,
                body: undefined,
                headers: undefined,
                propagateErrorKeyToLogs: undefined,
            },
        ]);
    });

    it('returns an empty array for nullish input', () => {
        expect(toResponseTemplates(undefined)).toEqual([]);
        expect(toResponseTemplates(null)).toEqual([]);
    });

    it('round-trips flat rows back to the nested map shape', () => {
        const flat = toResponseTemplates(nested);
        expect(fromResponseTemplates(flat)).toEqual(nested);
    });

    it('does not collide when key/contentType pairs share dashes', () => {
        const colliding: ResponseTemplatesMap = {
            ERR: { 'x-yaml': { statusCode: 400 } },
            'ERR-x': { yaml: { statusCode: 401 } },
        };
        const rows = toResponseTemplates(colliding);
        expect(rows.map(r => r.id)).toEqual(['ERR/x-yaml', 'ERR-x/yaml']);
        expect(new Set(rows.map(r => r.id)).size).toBe(2);
    });
});

describe('isDuplicateKeyAccept', () => {
    const rows = toResponseTemplates({
        DEFAULT: {
            'application/json': { statusCode: 400 },
            '*/*': { statusCode: 400 },
        },
    });

    it('detects an existing key + Accept pair', () => {
        expect(isDuplicateKeyAccept(rows, 'DEFAULT', 'application/json')).toBe(true);
    });

    it('ignores the row being edited by key + content-type', () => {
        expect(isDuplicateKeyAccept(rows, 'DEFAULT', 'application/json', { key: 'DEFAULT', contentType: 'application/json' })).toBe(false);
    });

    it('allows a new Accept header for the same key', () => {
        expect(isDuplicateKeyAccept(rows, 'DEFAULT', 'text/plain')).toBe(false);
    });
});

describe('filterResponseTemplates', () => {
    const rows = toResponseTemplates({
        DEFAULT: { 'application/json': { statusCode: 400 } },
        RATE_LIMIT_TOO_MANY_REQUESTS: { '*/*': { statusCode: 429 } },
    });

    it('filters by key, content-type, or status', () => {
        expect(filterResponseTemplates(rows, 'rate')).toHaveLength(1);
        expect(filterResponseTemplates(rows, 'application/json')).toHaveLength(1);
        expect(filterResponseTemplates(rows, '429')).toHaveLength(1);
        expect(filterResponseTemplates(rows, '')).toHaveLength(2);
    });
});

describe('toResponseTemplatePath / parseResponseTemplatePath', () => {
    it('encodes for navigate and parses useParams-decoded segments', () => {
        expect(toResponseTemplatePath('DEFAULT', 'application/json')).toBe('DEFAULT/application%2Fjson');
        expect(toResponseTemplatePath('API_KEY_MISSING', '*/*')).toBe('API_KEY_MISSING/%2A%2F%2A');
        // useParams already returns decoded values
        expect(parseResponseTemplatePath('DEFAULT', 'application/json')).toEqual({
            key: 'DEFAULT',
            contentType: 'application/json',
        });
        expect(parseResponseTemplatePath('API_KEY_MISSING', '*/*')).toEqual({
            key: 'API_KEY_MISSING',
            contentType: '*/*',
        });
    });

    it('preserves literal % in keys (no second decodeURIComponent)', () => {
        expect(parseResponseTemplatePath('ERR%RETRY', 'application/json')).toEqual({
            key: 'ERR%RETRY',
            contentType: 'application/json',
        });
    });

    it('round-trips wildcard content-types via encode then simulate useParams decode', () => {
        const path = toResponseTemplatePath('API_KEY_MISSING', '*/*');
        const [templateKey, contentType] = path.split('/');
        expect(parseResponseTemplatePath(decodeURIComponent(templateKey!), decodeURIComponent(contentType!))).toEqual({
            key: 'API_KEY_MISSING',
            contentType: '*/*',
        });
    });
});

describe('upsertResponseTemplate', () => {
    const current: ResponseTemplatesMap = {
        DEFAULT: { 'application/json': { statusCode: 400 } },
    };

    it('appends on create', () => {
        const next = upsertResponseTemplate(current, {
            id: 'API_KEY_MISSING/%2A%2F%2A',
            key: 'API_KEY_MISSING',
            contentType: '*/*',
            statusCode: 401,
        });
        expect(next.API_KEY_MISSING['*/*'].statusCode).toBe(401);
        expect(next.DEFAULT['application/json'].statusCode).toBe(400);
    });

    it('replaces on edit matched by key + content-type', () => {
        const next = upsertResponseTemplate(
            current,
            { id: 'DEFAULT/application%2Fjson', key: 'DEFAULT', contentType: 'application/json', statusCode: 418 },
            { key: 'DEFAULT', contentType: 'application/json' },
        );
        expect(next.DEFAULT['application/json'].statusCode).toBe(418);
    });

    it('throws when the edit target is missing from fresh data', () => {
        expect(() =>
            upsertResponseTemplate(
                current,
                { id: 'GONE/%2A%2F%2A', key: 'GONE', contentType: '*/*', statusCode: 400 },
                { key: 'GONE', contentType: '*/*' },
            ),
        ).toThrow(/no longer exists/i);
    });

    it('throws on write-side key + Accept collision', () => {
        expect(() =>
            upsertResponseTemplate(current, {
                id: 'DEFAULT/application%2Fjson',
                key: 'DEFAULT',
                contentType: 'application/json',
                statusCode: 400,
            }),
        ).toThrow(/already exists/i);
    });
});

describe('removeResponseTemplate', () => {
    it('removes by key + content-type', () => {
        const next = removeResponseTemplate(
            {
                DEFAULT: { 'application/json': { statusCode: 400 }, '*/*': { statusCode: 400 } },
            },
            { key: 'DEFAULT', contentType: 'application/json' },
        );
        expect(next.DEFAULT['application/json']).toBeUndefined();
        expect(next.DEFAULT['*/*'].statusCode).toBe(400);
    });

    it('drops the parent key when the last content-type is removed', () => {
        const next = removeResponseTemplate(
            {
                DEFAULT: { 'application/json': { statusCode: 400 } },
            },
            { key: 'DEFAULT', contentType: 'application/json' },
        );
        expect(next.DEFAULT).toBeUndefined();
        expect(next).toEqual({});
    });

    it('throws when the target is already gone', () => {
        expect(() =>
            removeResponseTemplate(
                {
                    DEFAULT: { 'application/json': { statusCode: 400 } },
                },
                { key: 'GONE', contentType: '*/*' },
            ),
        ).toThrow(/no longer exists/i);
    });
});

describe('path encoding edge cases', () => {
    it('encodes keys with % and / for navigation without double-decoding on parse', () => {
        const key = 'ERR%RETRY';
        const contentType = 'application/vnd.api+json';
        const path = toResponseTemplatePath(key, contentType);
        expect(path).toBe('ERR%25RETRY/application%2Fvnd.api%2Bjson');
        const [templateKey, encodedContentType] = path.split('/');
        expect(parseResponseTemplatePath(decodeURIComponent(templateKey!), decodeURIComponent(encodedContentType!))).toEqual({
            key,
            contentType,
        });
    });
});
