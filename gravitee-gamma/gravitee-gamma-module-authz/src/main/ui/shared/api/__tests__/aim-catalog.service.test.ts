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
import { aimCatalogService } from '../aim-catalog.service';

const get = vi.fn();
vi.mock('../authz-api-client', () => ({
    authzCoreApiClient: {
        get: (path: string) => get(path),
        post: vi.fn(),
        put: vi.fn(),
        delete: vi.fn(),
    },
    ApiError: class ApiError extends Error {},
}));

describe('aimCatalogService.listItems', () => {
    beforeEach(() => get.mockReset());

    it('targets the AIM module catalog path with kind/page/perPage query params', async () => {
        get.mockResolvedValue({ data: [], pagination: { page: 1, perPage: 50, totalCount: 0 } });

        await aimCatalogService.listItems('env one', 'model', 2, 50);

        const path = get.mock.calls[0][0] as string;
        expect(path).toContain('/environments/env%20one/modules/aim/catalog/items');
        expect(path).toContain('kind=model');
        expect(path).toContain('page=2');
        expect(path).toContain('perPage=50');
    });

    it('flattens the AIM pagination envelope into a CatalogItemPage', async () => {
        const item = { id: 'm1', kind: 'model', definition: { name: 'GPT', queryName: 'gpt' } };
        get.mockResolvedValue({ data: [item], pagination: { page: 3, perPage: 25, totalCount: 120 } });

        const page = await aimCatalogService.listItems('DEFAULT', 'model', 3, 25);

        expect(page).toEqual({ data: [item], page: 3, perPage: 25, total: 120 });
    });

    it('encodes the kind for each catalog item type', async () => {
        get.mockResolvedValue({ data: [], pagination: { page: 1, perPage: 10, totalCount: 0 } });

        await aimCatalogService.listItems('DEFAULT', 'mcp-server', 1, 10);
        expect(get.mock.calls[0][0]).toContain('kind=mcp-server');

        await aimCatalogService.listItems('DEFAULT', 'agent', 1, 10);
        expect(get.mock.calls[1][0]).toContain('kind=agent');
    });
});
