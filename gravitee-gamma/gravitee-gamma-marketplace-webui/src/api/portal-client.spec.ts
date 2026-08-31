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
import { ApiError, portalApi } from './portal-client';
import { TEST_PORTAL_API } from '../testing/factories';
import { respondWithError, trackHandler } from '../testing/helpers';

describe('portalApi', () => {
    it('should resolve to the environment-scoped portal url', async () => {
        const tracker = trackHandler('get', `${TEST_PORTAL_API}/user`, { display_name: 'Jane' });

        await portalApi.get('/user');

        expect(tracker.callCount).toBe(1);
        expect(tracker.lastCall?.url).toBe(`${TEST_PORTAL_API}/user`);
    });

    it('should include csrf header when token exists', async () => {
        localStorage.setItem('XSRF-TOKEN', 'my-csrf-token');
        const tracker = trackHandler('get', `${TEST_PORTAL_API}/user`, {});

        await portalApi.get('/user');

        expect(tracker.lastCall?.headers.get('X-Xsrf-Token')).toBe('my-csrf-token');
        expect(tracker.lastCall?.headers.get('X-Requested-With')).toBe('XMLHttpRequest');
    });

    it('should throw api error on non ok response', async () => {
        respondWithError('get', `${TEST_PORTAL_API}/user`, 403);

        await expect(portalApi.get('/user')).rejects.toThrow(ApiError);
    });

    it('should surface backend message from json error responses', async () => {
        respondWithError('get', `${TEST_PORTAL_API}/user`, 400);

        await expect(portalApi.get('/user')).rejects.toMatchObject({
            message: 'Error 400',
            status: 400,
        });
    });

    it('should send json body on post', async () => {
        const tracker = trackHandler('post', `${TEST_PORTAL_API}/subscriptions`, { id: '123' });

        await portalApi.post('/subscriptions', { plan: 'team' });

        expect(tracker.lastCall?.body).toEqual({ plan: 'team' });
    });

    it('should send extra headers on post', async () => {
        const tracker = trackHandler('post', `${TEST_PORTAL_API}/auth/login`, null, 200);

        await portalApi.post('/auth/login', undefined, { Authorization: 'Basic abc' });

        expect(tracker.lastCall?.headers.get('Authorization')).toBe('Basic abc');
    });
});
