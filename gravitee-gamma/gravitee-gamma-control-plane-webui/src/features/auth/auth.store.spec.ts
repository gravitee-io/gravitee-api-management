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
import type { License } from '@gravitee/gamma-modules-sdk/types';
import { waitFor } from '@testing-library/react';
import { http, HttpResponse } from 'msw';

import { licenseService } from '@gravitee/gamma-modules-sdk';

import { useAuthStore } from './auth.store';
import { TEST_ENVIRONMENTS, TEST_MANAGEMENT_BASE, TEST_MANAGEMENT_V2_ORGANIZATION_BASE, buildUser } from '../../testing/factories';
import { respondWith, respondWithError, trackHandler } from '../../testing/helpers';
import { server } from '../../testing/server';
import { useEnvironmentStore } from '../environment/environment.store';

const LICENSE: License = { tier: 'enterprise', packs: [], features: [], scope: 'ORGANIZATION', isExpired: false };

describe('authStore', () => {
    beforeEach(() => {
        licenseService.setLicense(null);
    });

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

    it('should load the organization license before login resolves', async () => {
        trackHandler('post', `${TEST_MANAGEMENT_BASE}/user/login`, null, 200);
        trackHandler('get', `${TEST_MANAGEMENT_BASE}/user`, buildUser());
        respondWith('get', `${TEST_MANAGEMENT_V2_ORGANIZATION_BASE}/license`, LICENSE);

        await useAuthStore.getState().login('bob', 'password');

        expect(licenseService.getLicense()).toEqual(LICENSE);
    });

    it('should load the organization license before setting the user on login', async () => {
        trackHandler('post', `${TEST_MANAGEMENT_BASE}/user/login`, null, 200);
        trackHandler('get', `${TEST_MANAGEMENT_BASE}/user`, buildUser({ displayName: 'Bob' }));
        let licenseRequested = false;
        let releaseLicense: (() => void) | undefined;
        const gate = new Promise<void>(resolve => {
            releaseLicense = resolve;
        });
        server.use(
            http.get(`${TEST_MANAGEMENT_V2_ORGANIZATION_BASE}/license`, async () => {
                licenseRequested = true;
                await gate;
                return HttpResponse.json(LICENSE);
            }),
        );
        let licenseWhenUserSet: License | null | undefined;
        const unsubscribe = useAuthStore.subscribe(state => {
            if (state.user && licenseWhenUserSet === undefined) {
                licenseWhenUserSet = licenseService.getLicense();
            }
        });

        const login = useAuthStore.getState().login('bob', 'password');
        await waitFor(() => expect(licenseRequested).toBe(true));
        expect(useAuthStore.getState().user).toBeNull();
        releaseLicense?.();
        await login;
        unsubscribe();

        expect(licenseWhenUserSet).toEqual(LICENSE);
        expect(useAuthStore.getState().user?.displayName).toBe('Bob');
    });

    it('should finish login when the license does not respond within 2 seconds', async () => {
        jest.useFakeTimers();
        jest.spyOn(console, 'warn').mockImplementation(() => {});
        trackHandler('post', `${TEST_MANAGEMENT_BASE}/user/login`, null, 200);
        trackHandler('get', `${TEST_MANAGEMENT_BASE}/user`, buildUser({ displayName: 'Bob' }));
        let licenseRequested = false;
        let releaseLicense: (() => void) | undefined;
        const gate = new Promise<void>(resolve => {
            releaseLicense = resolve;
        });
        server.use(
            http.get(`${TEST_MANAGEMENT_V2_ORGANIZATION_BASE}/license`, async () => {
                licenseRequested = true;
                await gate;
                return HttpResponse.json(LICENSE);
            }),
        );

        let settled = false;
        const login = useAuthStore
            .getState()
            .login('bob', 'password')
            .then(() => {
                settled = true;
            });
        try {
            await waitFor(() => expect(licenseRequested).toBe(true));
            await jest.advanceTimersByTimeAsync(2000);

            expect(settled).toBe(true);
            expect(useAuthStore.getState().user?.displayName).toBe('Bob');
            expect(licenseService.getLicense()).toBeNull();
        } finally {
            releaseLicense?.();
            await login;
            jest.useRealTimers();
        }
    });

    it('should still sign the user in when the license request fails', async () => {
        jest.spyOn(console, 'error').mockImplementation(() => {});
        trackHandler('post', `${TEST_MANAGEMENT_BASE}/user/login`, null, 200);
        trackHandler('get', `${TEST_MANAGEMENT_BASE}/user`, buildUser({ displayName: 'Bob' }));
        respondWithError('get', `${TEST_MANAGEMENT_V2_ORGANIZATION_BASE}/license`, 500);

        await useAuthStore.getState().login('bob', 'password');

        expect(useAuthStore.getState().user?.displayName).toBe('Bob');
        expect(licenseService.getLicense()).toBeNull();
    });

    it('should clear the license on logout', async () => {
        licenseService.setLicense(LICENSE);
        useAuthStore.setState({ user: buildUser() });

        await useAuthStore.getState().logout();

        expect(licenseService.getLicense()).toBeNull();
    });

    it('should refresh the current user and bump the avatar cache', async () => {
        useAuthStore.setState({ user: buildUser(), avatarCacheBust: 1 });
        trackHandler('get', `${TEST_MANAGEMENT_BASE}/user`, buildUser({ displayName: 'Ada Lovelace', firstname: 'Ada' }));

        const user = await useAuthStore.getState().refreshCurrentUser();

        expect(user.displayName).toBe('Ada Lovelace');
        expect(useAuthStore.getState().user?.displayName).toBe('Ada Lovelace');
        expect(useAuthStore.getState().avatarCacheBust).toBeGreaterThan(1);
    });

    it('should not restore the user if refresh finishes after logout', async () => {
        useAuthStore.setState({ user: buildUser(), initialized: true });
        let releaseGet: (() => void) | undefined;
        const gate = new Promise<void>(resolve => {
            releaseGet = resolve;
        });
        server.use(
            http.get(`${TEST_MANAGEMENT_BASE}/user`, async () => {
                await gate;
                return HttpResponse.json(buildUser({ displayName: 'Stale' }));
            }),
        );

        const refresh = useAuthStore.getState().refreshCurrentUser();
        await useAuthStore.getState().logout();
        releaseGet?.();

        await expect(refresh).rejects.toThrow(/signed out/i);
        expect(useAuthStore.getState().user).toBeNull();
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
