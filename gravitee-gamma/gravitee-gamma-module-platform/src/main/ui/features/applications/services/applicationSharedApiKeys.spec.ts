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
import { listApplicationApiKeys, renewApplicationApiKey, revokeApplicationApiKey } from './applicationSharedApiKeys';
import { apimFetchJsonV1Env } from '../../../shared/api/apimClient';

jest.mock('../../../shared/api/apimClient', () => ({
    apimFetchJsonV1Env: jest.fn(),
}));

const mockFetch = jest.mocked(apimFetchJsonV1Env);

describe('applicationSharedApiKeys', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        mockFetch.mockResolvedValue(undefined);
    });

    it('lists keys for the application', async () => {
        mockFetch.mockResolvedValueOnce([{ id: 'key-1', key: 'value-1' }]);

        await expect(listApplicationApiKeys('DEFAULT', 'app/1')).resolves.toEqual([{ id: 'key-1', key: 'value-1' }]);
        expect(mockFetch).toHaveBeenCalledWith('DEFAULT', '/applications/app%2F1/apikeys');
    });

    it('renews with an empty body when no custom value is given', async () => {
        await renewApplicationApiKey('DEFAULT', 'app-1', '   ');

        expect(mockFetch).toHaveBeenCalledWith('DEFAULT', '/applications/app-1/apikeys/_renew', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: '{}',
        });
    });

    it('renews with a trimmed custom value', async () => {
        await renewApplicationApiKey('DEFAULT', 'app-1', '  custom-key-1  ');

        expect(mockFetch).toHaveBeenCalledWith('DEFAULT', '/applications/app-1/apikeys/_renew', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ customApiKey: 'custom-key-1' }),
        });
    });

    it('revokes one application key', async () => {
        await revokeApplicationApiKey('DEFAULT', 'app-1', 'key/1');

        expect(mockFetch).toHaveBeenCalledWith('DEFAULT', '/applications/app-1/apikeys/key%2F1', { method: 'DELETE' });
    });
});
