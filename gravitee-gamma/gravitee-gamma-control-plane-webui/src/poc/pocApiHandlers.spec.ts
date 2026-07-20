/*
 * Copyright (C) 2026 The Gravitee team (http://gravitee.io)
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

import { handlePocApiRequest } from './pocApiHandlers';
import { DEMO_API_ID, POC_API_STORAGE_KEY } from './pocApiStore';

describe('pocApiHandlers', () => {
    beforeEach(() => {
        window.localStorage.clear();
    });

    async function post(path: string, body?: unknown): Promise<Response> {
        const url = new URL(path, 'http://localhost:4200');
        return (await handlePocApiRequest(
            new Request(url.toString(), {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: body === undefined ? undefined : JSON.stringify(body),
            }),
            url.pathname,
            url.searchParams,
        ))!;
    }

    async function get(path: string): Promise<Response> {
        const url = new URL(path, 'http://localhost:4200');
        return (await handlePocApiRequest(new Request(url.toString()), url.pathname, url.searchParams))!;
    }

    it('seeds demo-api and returns it from search', async () => {
        const res = await post('/management/v2/environments/env-1-id/apis/_search?page=1&perPage=10', {});
        expect(res.status).toBe(200);
        const json = await res.json();
        expect(json.data.some((api: { id: string }) => api.id === DEMO_API_ID)).toBe(true);
        expect(window.localStorage.getItem(POC_API_STORAGE_KEY)).toBeTruthy();
    });

    it('rejects duplicate context paths on verify', async () => {
        const res = await post('/management/v2/environments/env-1-id/apis/_verify/paths', {
            paths: [{ path: '/sports/' }],
        });
        const json = await res.json();
        expect(json.ok).toBe(false);
    });

    it('creates api, plan, publish, start, then serves detail and overview stubs', async () => {
        const verify = await post('/management/v2/environments/env-1-id/apis/_verify/paths', {
            paths: [{ path: '/orders' }],
        });
        expect((await verify.json()).ok).toBe(true);

        const createRes = await post('/management/v2/environments/env-1-id/apis', {
            name: 'Orders API',
            apiVersion: '1.0',
            description: 'POC create',
            type: 'PROXY',
            definitionVersion: 'V4',
            visibility: 'PRIVATE',
            listeners: [{ type: 'HTTP', paths: [{ path: '/orders' }], entrypoints: [{ type: 'http-proxy' }] }],
            endpointGroups: [
                {
                    name: 'Default endpoint group',
                    type: 'http-proxy',
                    endpoints: [
                        {
                            name: 'Default endpoint',
                            type: 'http-proxy',
                            configuration: { target: 'https://example.com' },
                        },
                    ],
                },
            ],
        });
        expect(createRes.status).toBe(201);
        const created = await createRes.json();
        expect(created.id).toBeTruthy();
        expect(created.name).toBe('Orders API');

        const planRes = await post(`/management/v2/environments/env-1-id/apis/${created.id}/plans`, {
            name: 'Default keyless plan',
            security: { type: 'KEY_LESS' },
            definitionVersion: 'V4',
            mode: 'STANDARD',
        });
        expect(planRes.status).toBe(201);
        const plan = await planRes.json();

        const publishRes = await post(`/management/v2/environments/env-1-id/apis/${created.id}/plans/${plan.id}/_publish`);
        expect(publishRes.status).toBe(204);

        const startRes = await post(`/management/v2/environments/env-1-id/apis/${created.id}/_start`);
        expect(startRes.status).toBe(204);

        const detail = await get(`/management/v2/environments/env-1-id/apis/${created.id}?expands=deploymentState`);
        expect(detail.status).toBe(200);
        const api = await detail.json();
        expect(api.state).toBe('STARTED');
        expect(api.deploymentState).toBe('DEPLOYED');

        const search = await post('/management/v2/environments/env-1-id/apis/_search?page=1&perPage=10', {});
        const list = await search.json();
        expect(list.data.some((a: { id: string }) => a.id === created.id)).toBe(true);
        expect(list.pagination.totalCount).toBeGreaterThanOrEqual(2);

        const perms = await get(
            `/management/organizations/test-org/environments/env-1-id/apis/${created.id}/members/permissions`,
        );
        expect(perms.status).toBe(200);

        const members = await get(`/management/v2/environments/env-1-id/apis/${created.id}/members`);
        expect((await members.json()).data).toEqual([]);

        const analytics = await get(`/management/v2/environments/env-1-id/apis/${created.id}/analytics?type=STATS`);
        expect(analytics.status).toBe(200);

        const alerts = await get(`/management/organizations/test-org/environments/env-1-id/apis/${created.id}/alerts`);
        expect(await alerts.json()).toEqual([]);

        const entrypoints = await get(`/management/v2/environments/env-1-id/apis/${created.id}/exposedEntrypoints`);
        expect(await entrypoints.json()).toEqual([]);
    });

    it('falls through for unrelated management paths', async () => {
        const url = new URL('/management/organizations/test-org/user', 'http://localhost:4200');
        const handled = await handlePocApiRequest(new Request(url.toString()), url.pathname, url.searchParams);
        expect(handled).toBeNull();
    });
});
