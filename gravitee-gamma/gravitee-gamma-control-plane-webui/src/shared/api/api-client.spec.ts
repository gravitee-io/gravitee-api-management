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
import { ApiError, gammaApi, managementApi } from './api-client';
import { TEST_GAMMA_BASE, TEST_MANAGEMENT_BASE } from '../../testing/factories';
import { trackHandler, respondWithError } from '../../testing/helpers';

describe('managementApi', () => {
    it('should resolve to correct management url', async () => {
        const tracker = trackHandler('get', `${TEST_MANAGEMENT_BASE}/user`, { displayName: 'Alice' });

        await managementApi.get('/user');

        expect(tracker.callCount).toBe(1);
        expect(tracker.lastCall?.url).toBe(`${TEST_MANAGEMENT_BASE}/user`);
    });

    it('should include csrf header when token exists', async () => {
        localStorage.setItem('XSRF-TOKEN', 'my-csrf-token');
        const tracker = trackHandler('get', `${TEST_MANAGEMENT_BASE}/user`, {});

        await managementApi.get('/user');

        expect(tracker.lastCall?.headers.get('X-Xsrf-Token')).toBe('my-csrf-token');
        expect(tracker.lastCall?.headers.get('X-Requested-With')).toBe('XMLHttpRequest');
    });

    it('should throw api error on non ok response', async () => {
        respondWithError('get', `${TEST_MANAGEMENT_BASE}/user`, 403);

        await expect(managementApi.get('/user')).rejects.toThrow(ApiError);
    });

    it('should send json body on post', async () => {
        const tracker = trackHandler('post', `${TEST_MANAGEMENT_BASE}/apis`, { id: '123' });

        await managementApi.post('/apis', { name: 'My API' });

        expect(tracker.lastCall?.body).toEqual({ name: 'My API' });
    });
});

describe('gammaApi', () => {
    it('should resolve to correct gamma url', async () => {
        const tracker = trackHandler('get', `${TEST_GAMMA_BASE}/modules`, []);

        await gammaApi.get('/modules');

        expect(tracker.callCount).toBe(1);
        expect(tracker.lastCall?.url).toBe(`${TEST_GAMMA_BASE}/modules`);
    });
});
