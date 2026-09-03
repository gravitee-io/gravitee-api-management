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

import { listIntegrations } from './integrationList';
import { apimFetchJsonV2 } from '../../../shared/api/apimClient';

jest.mock('../../../shared/api/apimClient', () => ({
    apimFetchJsonV2: jest.fn(),
}));

const mockApimFetchJsonV2 = jest.mocked(apimFetchJsonV2);

describe('integrations list service', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        mockApimFetchJsonV2.mockResolvedValue(undefined);
    });

    it('lists integrations for the environment with offset page and perPage parameters', async () => {
        await listIntegrations('env-1', { page: 1, perPage: 10 });

        expect(mockApimFetchJsonV2).toHaveBeenCalledWith('env-1', '/integrations?page=1&perPage=10');
    });

    it('carries no parameter other than page and perPage on any page', async () => {
        await listIntegrations('env-1', { page: 1, perPage: 10 });
        await listIntegrations('env-1', { page: 2, perPage: 25 });

        const parameterNames = mockApimFetchJsonV2.mock.calls.map(([, path]) => [
            ...new URLSearchParams(path.slice(path.indexOf('?'))).keys(),
        ]);

        expect(parameterNames).toEqual([
            ['page', 'perPage'],
            ['page', 'perPage'],
        ]);
    });
});
