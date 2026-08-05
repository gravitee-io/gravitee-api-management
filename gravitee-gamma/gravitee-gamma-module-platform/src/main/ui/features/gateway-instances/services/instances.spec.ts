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

import { getGatewayInstance, getGatewayInstanceMonitoring, listGatewayInstances } from './instances';
import { apimFetchJsonV1Env } from '../../../shared/api/apimClient';

jest.mock('../../../shared/api/apimClient', () => ({
    apimFetchJsonV1Env: jest.fn(),
}));

const mockFetch = jest.mocked(apimFetchJsonV1Env);

describe('instances service', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        mockFetch.mockResolvedValue({} as never);
    });

    it('lists instances with classic query defaults', async () => {
        await listGatewayInstances('env-1', { page: 0, size: 10 });
        expect(mockFetch).toHaveBeenCalledWith('env-1', '/instances/?includeStopped=true&from=0&to=0&page=0&size=10');
    });

    it('gets a single instance by event id', async () => {
        await getGatewayInstance('env-1', 'event-1');
        expect(mockFetch).toHaveBeenCalledWith('env-1', '/instances/event-1');
    });

    it('gets monitoring data with event id and gateway id', async () => {
        await getGatewayInstanceMonitoring('env-1', 'event-1', 'gateway-1');
        expect(mockFetch).toHaveBeenCalledWith('env-1', '/instances/event-1/monitoring/gateway-1');
    });
});
