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
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { authzApiService } from '../authz-api.service';

const get = vi.fn();
const put = vi.fn();
const del = vi.fn();
vi.mock('../authz-api-client', () => ({
    authzCoreApiClient: {
        get: (path: string) => get(path),
        post: vi.fn(),
        put: (path: string, body: unknown) => put(path, body),
        delete: (path: string) => del(path),
    },
    ApiError: class ApiError extends Error {},
}));

function policy(name: string, entityId: string | null) {
    return {
        id: name,
        name,
        kind: entityId ? 'RESOURCE' : 'GLOBAL',
        entityId,
        policyText: '',
        status: 'DRAFT',
        environmentId: 'DEFAULT',
        createdAt: '2026-01-01T00:00:00Z',
        updatedAt: '2026-01-01T00:00:00Z',
    };
}

describe('authzApiService.listPolicies — type filtering across pages', () => {
    beforeEach(() => get.mockReset());

    it('fetches the full set and filters by type, so MCP policies past the first page still surface', async () => {
        // Backend returns mixed types; the two MCP policies sit at the end —
        // exactly the case the old single-page client filter dropped.
        const data = [
            ...Array.from({ length: 9 }, (_, i) => policy(`global-${i}`, null)),
            policy('mcp-a', 'mcp.bookings'),
            policy('mcp-b', 'mcp.customers'),
            policy('api-x', 'api.orders'),
        ];
        get.mockResolvedValue({ data, total: data.length, page: 1, perPage: 100 });

        const res = await authzApiService.listPolicies('DEFAULT', { type: 'MCP', page: 1, perPage: 10 });

        // Asks the backend for the full set (MAX_PER_PAGE), not a single UI page.
        expect(get).toHaveBeenCalledWith(expect.stringContaining('perPage=100'));
        expect(get).toHaveBeenCalledWith(expect.not.stringContaining('page=2'));
        expect(res.total).toBe(2);
        expect(res.data.map(p => p.name)).toEqual(['mcp-a', 'mcp-b']);
    });

    it('paginates the filtered set locally', async () => {
        const data = [policy('mcp-1', 'mcp.a'), policy('mcp-2', 'mcp.b'), policy('mcp-3', 'mcp.c')];
        get.mockResolvedValue({ data, total: data.length, page: 1, perPage: 100 });

        const res = await authzApiService.listPolicies('DEFAULT', { type: 'MCP', page: 2, perPage: 2 });

        expect(res.total).toBe(3);
        expect(res.page).toBe(2);
        expect(res.data.map(p => p.name)).toEqual(['mcp-3']);
    });

    it('passes through server pagination when no type filter is set', async () => {
        get.mockResolvedValue({ data: [policy('p', null)], total: 42, page: 3, perPage: 10 });

        const res = await authzApiService.listPolicies('DEFAULT', { page: 3, perPage: 10 });

        expect(res.total).toBe(42);
        expect(res.page).toBe(3);
        expect(res.data.map(p => p.name)).toEqual(['p']);
    });
});

function canonicalEntity(overrides: Record<string, unknown> = {}) {
    return {
        id: 'e1',
        environmentId: 'DEFAULT',
        entityId: 'user.alice',
        attributes: { _displayName: 'Alice', department: 'engineering' },
        parents: ['group.developers'],
        source: 'local',
        createdAt: '2026-01-01T00:00:00Z',
        updatedAt: '2026-01-02T00:00:00Z',
        ...overrides,
    };
}

describe('authzApiService.updateEntity', () => {
    beforeEach(() => put.mockReset());

    it('PUTs attributes + parents to the entity path and returns the adapted entity', async () => {
        put.mockResolvedValue(canonicalEntity({ attributes: { _displayName: 'Alice Smith', department: 'engineering' } }));

        const body = { attributes: { _displayName: 'Alice Smith', department: 'engineering' }, parents: ['group.developers'] };
        const res = await authzApiService.updateEntity('DEFAULT', 'user.alice', body);

        expect(put).toHaveBeenCalledTimes(1);
        const [path, sentBody] = put.mock.calls[0];
        expect(path).toContain('/environments/DEFAULT/modules/authz/entities/user.alice');
        expect(sentBody).toEqual(body);
        // Adapted shape: canonical entityId → uid, source surfaced as _source.
        expect(res.uid).toBe('user.alice');
        expect(res.attributes._displayName).toBe('Alice Smith');
        expect(res.attributes.department).toBe('engineering');
        expect(res.attributes._source).toBe('local');
        expect(res.parents).toEqual(['group.developers']);
    });

    it('url-encodes the entityId in the path', async () => {
        put.mockResolvedValue(canonicalEntity({ entityId: 'custom prefix.id' }));

        await authzApiService.updateEntity('DEFAULT', 'custom prefix.id', { attributes: {}, parents: [] });

        expect(put.mock.calls[0][0]).toContain('/entities/custom%20prefix.id');
    });
});

describe('authzApiService.deleteEntity', () => {
    beforeEach(() => del.mockReset());

    it('DELETEs the url-encoded entity path', async () => {
        del.mockResolvedValue(undefined);

        await authzApiService.deleteEntity('DEFAULT', 'user.alice');

        expect(del).toHaveBeenCalledTimes(1);
        expect(del.mock.calls[0][0]).toContain('/environments/DEFAULT/modules/authz/entities/user.alice');
    });

    it('url-encodes the entityId in the path', async () => {
        del.mockResolvedValue(undefined);

        await authzApiService.deleteEntity('DEFAULT', 'custom prefix.id');

        expect(del.mock.calls[0][0]).toContain('/entities/custom%20prefix.id');
    });
});

describe('authzApiService.updateSchema', () => {
    beforeEach(() => put.mockReset());

    it('PUTs the schema text to the schema path and maps the canonical response', async () => {
        put.mockResolvedValue({ schema: 'entity Edited {};' });

        const res = await authzApiService.updateSchema('DEFAULT', 'entity Edited {};');

        expect(put).toHaveBeenCalledTimes(1);
        const [path, sentBody] = put.mock.calls[0];
        expect(path).toBe('/environments/DEFAULT/modules/authz/schema');
        expect(sentBody).toEqual({ schema: 'entity Edited {};' });
        expect(res).toEqual({ environmentId: 'DEFAULT', schemaText: 'entity Edited {};', updatedAt: null });
    });
});

describe('authzApiService.deleteSchema', () => {
    beforeEach(() => del.mockReset());

    it('DELETEs the schema path', async () => {
        del.mockResolvedValue(undefined);

        await authzApiService.deleteSchema('DEFAULT');

        expect(del).toHaveBeenCalledTimes(1);
        expect(del.mock.calls[0][0]).toBe('/environments/DEFAULT/modules/authz/schema');
    });
});
