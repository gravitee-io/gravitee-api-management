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
import { getResourceSchema, listResourcePlugins, updateApiResources } from './resources';
import { resetApimClientForTests } from '../../../shared/api/apimClient';
import { TEST_CONFIG, TEST_V2_BASE } from '../../../testing/factories';
import { respondWith, trackHandler } from '../../../testing/helpers';
import type { ApiResource } from '../types/resource';

// No jest.mock anywhere: real fetch + real bootstrap, only the HTTP boundary is
// intercepted by MSW (server wired in test-setup.ts).

const ORG_BASE = `${TEST_CONFIG.managementBaseURL}/v2/organizations/${TEST_CONFIG.organizationId}`;
const RESOURCE_PLUGINS_PATH = `${ORG_BASE}/plugins/resources`;
const RESOURCE_SCHEMA_PATH = `${ORG_BASE}/plugins/resources/:resourceId/schema`;
const API_PATH = `${TEST_V2_BASE}/apis/:apiId`;

const CURRENT_API = {
    id: 'api-1',
    name: 'Passenger Boarding API',
    apiVersion: '1.0.0',
    definitionVersion: 'V4',
    type: 'PROXY',
    listeners: [{ type: 'HTTP', paths: [{ path: '/boarding' }] }],
    properties: [{ key: 'backend.timeout', value: '5000' }],
    resources: [{ name: 'old-cache', type: 'cache', enabled: true, configuration: { timeToLiveSeconds: 30 } }],
};

describe('resources service', () => {
    beforeEach(() => {
        resetApimClientForTests();
    });

    it('listResourcePlugins fetches the org-scoped resource plugin catalog', async () => {
        const plugins = [
            { id: 'cache', name: 'Cache' },
            { id: 'oauth2', name: 'OAuth2 — Generic' },
        ];
        respondWith('get', RESOURCE_PLUGINS_PATH, plugins);

        await expect(listResourcePlugins()).resolves.toEqual(plugins);
    });

    it('getResourceSchema returns the parsed JSON schema for a resource type', async () => {
        const schema = { type: 'object', properties: { timeToLiveSeconds: { type: 'integer', default: 60 } } };
        respondWith('get', RESOURCE_SCHEMA_PATH, schema);

        await expect(getResourceSchema('cache')).resolves.toEqual(schema);
    });

    it('getResourceSchema parses a double-encoded (JSON string) schema', async () => {
        const schema = { type: 'object', properties: { ttl: { type: 'integer' } } };
        respondWith('get', RESOURCE_SCHEMA_PATH, JSON.stringify(schema));

        await expect(getResourceSchema('cache')).resolves.toEqual(schema);
    });

    it('getResourceSchema rejects (instead of returning an empty schema) on malformed JSON', async () => {
        respondWith('get', RESOURCE_SCHEMA_PATH, '{ not: valid json');

        await expect(getResourceSchema('cache')).rejects.toThrow(/malformed json schema/i);
    });

    describe('updateApiResources (read-modify-write)', () => {
        const NEW_RESOURCES: ApiResource[] = [
            { name: 'session-cache', type: 'cache', enabled: true, configuration: { timeToLiveSeconds: 120 } },
        ];

        it('PUTs the full API definition with only the resources array replaced', async () => {
            respondWith('get', API_PATH, CURRENT_API);
            const put = trackHandler('put', API_PATH, {});

            await updateApiResources('DEFAULT', 'api-1', NEW_RESOURCES);

            expect(put.callCount).toBe(1);
            const body = put.lastCall!.body as Record<string, unknown>;

            // Critical: unrelated parts of the definition must survive the round-trip.
            expect(body.name).toBe('Passenger Boarding API');
            expect(body.listeners).toEqual(CURRENT_API.listeners);
            expect(body.properties).toEqual(CURRENT_API.properties);
            expect(body.definitionVersion).toBe('V4');

            // Critical: resources are fully replaced, never appended/merged with the old ones.
            expect(body.resources).toEqual(NEW_RESOURCES);
        });

        it('issues exactly one GET then one PUT against the env-scoped API endpoint', async () => {
            const get = trackHandler('get', API_PATH, CURRENT_API);
            const put = trackHandler('put', API_PATH, {});

            await updateApiResources('DEFAULT', 'api-1', NEW_RESOURCES);

            expect(get.callCount).toBe(1);
            expect(put.callCount).toBe(1);
            expect(new URL(put.lastCall!.url).pathname).toContain('/v2/environments/DEFAULT/apis/api-1');
            expect(put.lastCall!.headers.get('Content-Type')).toBe('application/json');
        });

        it('clears all resources when passed an empty array', async () => {
            respondWith('get', API_PATH, CURRENT_API);
            const put = trackHandler('put', API_PATH, {});

            await updateApiResources('DEFAULT', 'api-1', []);

            expect((put.lastCall!.body as Record<string, unknown>).resources).toEqual([]);
        });

        it('rejects (does not PUT) when the API cannot be read', async () => {
            respondWith('get', API_PATH, { message: 'boom' }, 500);
            const put = trackHandler('put', API_PATH, {});

            await expect(updateApiResources('DEFAULT', 'api-1', NEW_RESOURCES)).rejects.toBeTruthy();
            expect(put.callCount).toBe(0);
        });
    });
});
