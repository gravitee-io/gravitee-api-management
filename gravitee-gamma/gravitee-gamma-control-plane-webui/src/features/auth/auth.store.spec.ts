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
import { waitFor } from '@testing-library/react';
import { http, HttpResponse } from 'msw';

import { useAuthStore } from './auth.store';
import { TEST_ENVIRONMENTS, TEST_MANAGEMENT_BASE, buildUser } from '../../testing/factories';
import { respondWithError, trackHandler } from '../../testing/helpers';
import { server } from '../../testing/server';
import { useEnvironmentStore } from '../environment/environment.store';

describe('authStore', () => {
    it('should initialize with existing session', async () => {
        await useAuthStore.getState().initialize();

        expect(useAuthStore.getState().user?.displayName).toBe('Test User');
        expect(useAuthStore.getState().initialized).toBe(true);
    });

    it('should initialize as null when not authenticated', async () => {
        respondWithError('get', `${TEST_MANAGEMENT_BASE}/user`, 401);

        await useAuthStore.getState().initialize();

        expect(useAuthStore.getState().user).toBeNull();
        expect(useAuthStore.getState().initialized).toBe(true);
        expect(useAuthStore.getState().sessionCheckFailed).toBe(false);
    });

    it('should not say the session could not be checked when the platform refuses the request, as waiting would not change it', async () => {
        respondWithError('get', `${TEST_MANAGEMENT_BASE}/user`, 400);

        await useAuthStore.getState().initialize();

        expect(useAuthStore.getState().user).toBeNull();
        expect(useAuthStore.getState().sessionCheckFailed).toBe(false);
    });

    describe('when the session cannot be checked', () => {
        let error: jest.SpyInstance;

        beforeEach(() => {
            error = jest.spyOn(console, 'error').mockImplementation(() => undefined);
        });

        afterEach(() => {
            jest.restoreAllMocks();
        });

        it.each([
            ['the platform answers with an error', () => HttpResponse.json({ message: 'Error 503' }, { status: 503 })],
            ['the platform does not answer', () => HttpResponse.error()],
        ])('should say so instead of passing for a sign-out when %s', async (_case, respond) => {
            server.use(http.get(`${TEST_MANAGEMENT_BASE}/user`, respond));

            await useAuthStore.getState().initialize();

            expect(useAuthStore.getState().user).toBeNull();
            expect(useAuthStore.getState().initialized).toBe(true);
            expect(useAuthStore.getState().sessionCheckFailed).toBe(true);
            expect(error).toHaveBeenCalledWith('[Auth] Could not check the session.', expect.any(Error));
        });

        it('should forget it as soon as a sign-in starts, whatever its outcome', async () => {
            useAuthStore.setState({ sessionCheckFailed: true });
            respondWithError('post', `${TEST_MANAGEMENT_BASE}/user/login`, 503);

            await expect(useAuthStore.getState().login('bob', 'password')).rejects.toBeTruthy();

            expect(useAuthStore.getState().sessionCheckFailed).toBe(false);
            expect(useAuthStore.getState().user).toBeNull();
        });
    });

    it('should not reinitialize if already done', async () => {
        const tracker = trackHandler('get', `${TEST_MANAGEMENT_BASE}/user`, buildUser());

        await useAuthStore.getState().initialize();
        await useAuthStore.getState().initialize();

        expect(tracker.callCount).toBe(1);
    });

    it('should call login then get user', async () => {
        const loginTracker = trackHandler('post', `${TEST_MANAGEMENT_BASE}/user/login`, null, 200);
        const userTracker = trackHandler('get', `${TEST_MANAGEMENT_BASE}/user`, buildUser({ displayName: 'Bob' }));

        await useAuthStore.getState().login('bob', 'password');

        expect(loginTracker.callCount).toBe(1);
        expect(userTracker.callCount).toBe(1);
        expect(useAuthStore.getState().user?.displayName).toBe('Bob');
        await waitFor(() => {
            expect(useEnvironmentStore.getState().initialized).toBe(true);
        });
    });

    it('should load environments after login', async () => {
        useEnvironmentStore.getState().reset();
        const envTracker = trackHandler('get', `${TEST_MANAGEMENT_BASE}/environments`, TEST_ENVIRONMENTS);
        trackHandler('post', `${TEST_MANAGEMENT_BASE}/user/login`, null, 200);
        trackHandler('get', `${TEST_MANAGEMENT_BASE}/user`, buildUser({ displayName: 'Bob' }));

        await useAuthStore.getState().login('bob', 'password');

        await waitFor(() => {
            expect(envTracker.callCount).toBe(1);
        });
        expect(useEnvironmentStore.getState().environments).toEqual(TEST_ENVIRONMENTS);
    });

    it('should clear user and reset environment state on logout', async () => {
        useAuthStore.setState({ user: buildUser() });
        useEnvironmentStore.setState({
            organizationId: 'test-org',
            environmentId: 'e1',
            environments: TEST_ENVIRONMENTS,
            currentEnvironment: TEST_ENVIRONMENTS[0]!,
            loading: false,
            error: null,
            initialized: true,
        });

        await useAuthStore.getState().logout();

        expect(useAuthStore.getState().user).toBeNull();
        expect(useEnvironmentStore.getState().environments).toEqual([]);
        expect(useEnvironmentStore.getState().initialized).toBe(false);
    });
});
