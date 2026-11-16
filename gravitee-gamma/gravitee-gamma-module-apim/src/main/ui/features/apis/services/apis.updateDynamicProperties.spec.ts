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
import { updateDynamicProperties } from './apis';
import { apimFetchJsonV2 } from '../../../shared/api/apimClient';
import type { DynamicPropertyConfig } from '../types';

jest.mock('../../../shared/api/apimClient', () => ({
    apimFetchJsonV2: jest.fn(),
    apimFetchJsonV2WithMeta: jest.fn(),
    apimFetchBlobV2: jest.fn(),
}));

const mockFetchV2 = jest.mocked(apimFetchJsonV2);

describe('updateDynamicProperties', () => {
    const current: DynamicPropertyConfig = {
        enabled: true,
        overrideConfiguration: true,
        configuration: { url: 'https://stored.example.com' },
    };

    beforeEach(() => {
        jest.clearAllMocks();
        mockFetchV2.mockResolvedValueOnce({
            id: 'api-1',
            name: 'fresh name',
            services: { dynamicProperty: current, healthCheck: { enabled: true } },
        });
        mockFetchV2.mockResolvedValueOnce({});
    });

    it('builds the dynamic property from the one the API stores now', async () => {
        const build = jest.fn((stored: DynamicPropertyConfig | undefined) => ({ enabled: false, configuration: stored?.configuration }));

        await updateDynamicProperties('env-1', 'api-1', build);

        expect(build).toHaveBeenCalledWith(current);
    });

    it('puts the API as it is now with only its dynamic property replaced', async () => {
        const next: DynamicPropertyConfig = { enabled: false };

        await updateDynamicProperties('env-1', 'api-1', () => next);

        const [, path, init] = mockFetchV2.mock.calls[1];
        expect(path).toBe('/apis/api-1');
        expect(init?.method).toBe('PUT');
        expect(JSON.parse(init?.body as string)).toEqual({
            id: 'api-1',
            name: 'fresh name',
            services: { dynamicProperty: next, healthCheck: { enabled: true } },
        });
    });

    it('builds from nothing when the API has no dynamic property', async () => {
        mockFetchV2.mockReset();
        mockFetchV2.mockResolvedValueOnce({ id: 'api-1' });
        mockFetchV2.mockResolvedValueOnce({});
        const build = jest.fn(() => ({ enabled: true }));

        await updateDynamicProperties('env-1', 'api-1', build);

        expect(build).toHaveBeenCalledWith(undefined);
    });
});
