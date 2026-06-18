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
    getAmConnection,
    getDomain,
    isAmUnavailable,
    listDomainEntrypoints,
    listDomains,
    listEnvironments,
    saveAmConnection,
    testAmConnection,
} from './amManagement';
import { ApimApiError, gammaFetchJson } from '../../../shared/api/apimClient';
import { type AmConfig } from '../utils/amConfig';

jest.mock('../../../shared/api/apimClient', () => ({
    ...jest.requireActual('../../../shared/api/apimClient'),
    gammaFetchJson: jest.fn(),
}));

const mockFetch = jest.mocked(gammaFetchJson);

const cfg: AmConfig = { organizationId: 'DEFAULT', environmentId: '', domainId: '', graviteeEnvironmentId: 'gv-env-1' };
const BASE = '/organizations/DEFAULT/environments/gv-env-1/modules/platform/am';

describe('amManagement service', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        mockFetch.mockResolvedValue(undefined as never);
    });

    describe('URL construction', () => {
        it('listEnvironments hits the environments resource', async () => {
            await listEnvironments(cfg);
            expect(mockFetch).toHaveBeenCalledWith(`${BASE}/environments`);
        });

        it('listDomains omits the query suffix when no term is given', async () => {
            await listDomains(cfg, 'env-1');
            expect(mockFetch).toHaveBeenCalledWith(`${BASE}/environments/env-1/domains`);
        });

        it('listDomains encodes the search term', async () => {
            await listDomains(cfg, 'env-1', 'a b&c');
            expect(mockFetch).toHaveBeenCalledWith(`${BASE}/environments/env-1/domains?q=a%20b%26c`);
        });

        it('getDomain encodes the domain id', async () => {
            await getDomain(cfg, 'env-1', 'dom/1');
            expect(mockFetch).toHaveBeenCalledWith(`${BASE}/environments/env-1/domains/dom%2F1`);
        });

        it('listDomainEntrypoints hits the entrypoints resource', async () => {
            await listDomainEntrypoints(cfg, 'env-1', 'dom-1');
            expect(mockFetch).toHaveBeenCalledWith(`${BASE}/environments/env-1/domains/dom-1/entrypoints`);
        });

        it('getAmConnection hits am-config', async () => {
            await getAmConnection(cfg);
            expect(mockFetch).toHaveBeenCalledWith(`${BASE}/am-config`);
        });

        it('saveAmConnection PUTs the payload to am-config', async () => {
            const payload = { baseUrl: 'http://localhost:8093', defaultDomainId: null, defaultDomainHrid: null, gatewayUrl: null };
            await saveAmConnection(cfg, payload);
            expect(mockFetch).toHaveBeenCalledWith(`${BASE}/am-config`, { method: 'PUT', body: JSON.stringify(payload) });
        });

        it('testAmConnection POSTs the payload to am-config/_test', async () => {
            const payload = { baseUrl: 'http://localhost:8093', defaultDomainId: null, defaultDomainHrid: null, gatewayUrl: null };
            await testAmConnection(cfg, payload);
            expect(mockFetch).toHaveBeenCalledWith(`${BASE}/am-config/_test`, { method: 'POST', body: JSON.stringify(payload) });
        });
    });

    describe('isAmUnavailable', () => {
        it('is true for a 503', () => {
            expect(isAmUnavailable(new ApimApiError(503, 'unavailable'))).toBe(true);
        });

        it('is true for an am_not_configured body', () => {
            expect(isAmUnavailable(new ApimApiError(404, 'nope', { code: 'am_not_configured' }))).toBe(true);
        });

        it('is false for an unrelated ApimApiError', () => {
            expect(isAmUnavailable(new ApimApiError(500, 'boom', { code: 'other' }))).toBe(false);
        });

        it('is false for a non-ApimApiError', () => {
            expect(isAmUnavailable(new Error('plain'))).toBe(false);
        });
    });
});
