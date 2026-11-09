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

import { permissionService } from '@gravitee/gamma-modules-sdk';

import { reloadEnvironmentPermissions, startPermissionSync } from './permission-sync';
import { buildEnvironment, buildUser, TEST_MANAGEMENT_BASE } from '../../testing/factories';
import { respondWith, respondWithError, seedUser, trackHandler } from '../../testing/helpers';
import { server } from '../../testing/server';
import { useAuthStore } from '../auth/auth.store';
import { useEnvironmentStore } from '../environment/environment.store';

describe('permission-sync', () => {
    let stopSync: () => void;

    beforeEach(() => {
        stopSync = startPermissionSync();
    });

    afterEach(() => {
        stopSync();
    });

    it('should load organization permissions when user is set after initialize', async () => {
        await useAuthStore.getState().initialize();

        expect(permissionService.hasAnyOf(['organization-user-r'])).toBe(true);
    });

    it('should reset permissions when user becomes null after logout', async () => {
        await useAuthStore.getState().initialize();
        expect(permissionService.getAllPermissions().length).toBeGreaterThan(0);

        await useAuthStore.getState().logout();

        expect(permissionService.getAllPermissions()).toEqual([]);
    });

    it('should reset permissions when authentication fails', async () => {
        respondWithError('get', `${TEST_MANAGEMENT_BASE}/user`, 401);

        await useAuthStore.getState().initialize();

        expect(permissionService.getAllPermissions()).toEqual([]);
    });

    it('should load environment permissions when environmentId changes', async () => {
        await useAuthStore.getState().initialize();
        const tracker = trackHandler('get', `${TEST_MANAGEMENT_BASE}/environments/staging/permissions`, {
            API: ['R'],
        });

        useEnvironmentStore.getState().setCurrentEnvironment(buildEnvironment({ id: 'staging' }));

        await waitFor(() => expect(tracker.callCount).toBe(1));
        expect(permissionService.hasAnyOf(['environment-api-r'])).toBe(true);
    });

    it('should not load environment permissions when no user is logged in', () => {
        const tracker = trackHandler('get', `${TEST_MANAGEMENT_BASE}/environments/staging/permissions`, {
            API: ['R'],
        });

        useEnvironmentStore.getState().setCurrentEnvironment(buildEnvironment({ id: 'staging' }));

        expect(tracker.callCount).toBe(0);
    });

    it('should reload organization permissions on login with new roles', async () => {
        trackHandler('post', `${TEST_MANAGEMENT_BASE}/user/login`, null, 200);
        trackHandler(
            'get',
            `${TEST_MANAGEMENT_BASE}/user`,
            buildUser({
                roles: [{ scope: 'ORGANIZATION', permissions: { USER: ['U'], ROLE: ['C'] } }],
            }),
        );

        await useAuthStore.getState().login('bob', 'password');

        expect(permissionService.hasAnyOf(['organization-user-u'])).toBe(true);
        expect(permissionService.hasAnyOf(['organization-role-c'])).toBe(true);
    });

    describe('when an answer comes after the permissions it was asked for were dropped', () => {
        const permissionsUrl = (environmentId: string) => `${TEST_MANAGEMENT_BASE}/environments/${environmentId}/permissions`;

        interface HeldAnswer {
            answer: (body: unknown, status?: number) => void;
        }

        function holdPermissions(environmentId: string): HeldAnswer {
            const held: HeldAnswer = { answer: () => undefined };
            server.use(
                http.get(
                    permissionsUrl(environmentId),
                    () =>
                        new Promise<Response>(
                            resolve => (held.answer = (body, status) => resolve(HttpResponse.json(body as never, { status }))),
                        ),
                    { once: true },
                ),
            );
            return held;
        }

        async function flushAnswers() {
            await jest.runOnlyPendingTimersAsync();
        }

        beforeEach(() => {
            jest.useFakeTimers({ advanceTimers: true });
        });

        afterEach(() => {
            jest.useRealTimers();
        });

        it('should drop the answer for an environment left since', async () => {
            const firstEnvironment = holdPermissions('first');
            holdPermissions('second');
            seedUser();
            useEnvironmentStore.getState().setCurrentEnvironment(buildEnvironment({ id: 'first' }));
            await flushAnswers();
            useEnvironmentStore.getState().setCurrentEnvironment(buildEnvironment({ id: 'second' }));

            firstEnvironment.answer({ API: ['R'] });
            await flushAnswers();

            expect(permissionService.getLoadStatus('environment')).toBe('pending');
            expect(permissionService.hasAnyOf(['environment-api-r'])).toBe(false);
        });

        it('should drop an answer that comes after the user signed out', async () => {
            useEnvironmentStore.getState().setCurrentEnvironment(buildEnvironment({ id: 'staging' }));
            const answerToSignedOutUser = holdPermissions('staging');
            seedUser();
            await flushAnswers();

            useAuthStore.setState({ user: null });
            answerToSignedOutUser.answer({ API: ['R'] });
            await flushAnswers();

            expect(permissionService.getLoadStatus('environment')).toBe('pending');
            expect(permissionService.hasAnyOf(['environment-api-r'])).toBe(false);
        });

        it('should give the next user their own permissions, never those asked for the user before, in the same environment', async () => {
            useEnvironmentStore.getState().setCurrentEnvironment(buildEnvironment({ id: 'staging' }));
            const answerToFirstUser = holdPermissions('staging');
            seedUser({ id: 'ada' });
            await flushAnswers();
            useAuthStore.setState({ user: null });
            const answerToSecondUser = holdPermissions('staging');
            seedUser({ id: 'bob' });
            await flushAnswers();

            answerToFirstUser.answer({ API: ['R', 'U', 'D'] });
            await flushAnswers();
            expect(permissionService.getLoadStatus('environment')).toBe('pending');

            answerToSecondUser.answer({ API: ['R'] });
            await flushAnswers();
            expect(permissionService.hasAnyOf(['environment-api-r'])).toBe(true);
            expect(permissionService.hasAnyOf(['environment-api-d'])).toBe(false);
        });

        it('should drop the failure of an environment left since', async () => {
            const firstEnvironment = holdPermissions('first');
            const secondEnvironment = holdPermissions('second');
            jest.spyOn(console, 'error').mockImplementation(() => undefined);
            seedUser();
            useEnvironmentStore.getState().setCurrentEnvironment(buildEnvironment({ id: 'first' }));
            await flushAnswers();
            useEnvironmentStore.getState().setCurrentEnvironment(buildEnvironment({ id: 'second' }));
            await flushAnswers();

            firstEnvironment.answer({ message: 'boom' }, 500);
            await flushAnswers();
            expect(permissionService.getLoadStatus('environment')).toBe('pending');

            secondEnvironment.answer({});
            await flushAnswers();
            expect(permissionService.getLoadStatus('environment')).toBe('loaded');
        });
    });

    describe('when the environment permissions cannot be read', () => {
        const permissionsUrl = (environmentId: string) => `${TEST_MANAGEMENT_BASE}/environments/${environmentId}/permissions`;

        beforeEach(() => {
            jest.spyOn(console, 'error').mockImplementation(() => undefined);
            seedUser();
        });

        it('should say so, and read them again when asked', async () => {
            respondWithError('get', permissionsUrl('staging'), 500);

            useEnvironmentStore.getState().setCurrentEnvironment(buildEnvironment({ id: 'staging' }));

            await waitFor(() => expect(permissionService.getLoadStatus('environment')).toBe('failed'));

            respondWith('get', permissionsUrl('staging'), { API: ['R'] });
            reloadEnvironmentPermissions();

            await waitFor(() => expect(permissionService.getLoadStatus('environment')).toBe('loaded'));
            expect(permissionService.hasAnyOf(['environment-api-r'])).toBe(true);
        });
    });

    it('should reload environment permissions on login when environmentId does not change', async () => {
        useEnvironmentStore.getState().setCurrentEnvironment(buildEnvironment({ id: 'env-1-id' }));
        const envPermTracker = trackHandler('get', `${TEST_MANAGEMENT_BASE}/environments/env-1-id/permissions`, {
            API: ['R'],
        });

        trackHandler('post', `${TEST_MANAGEMENT_BASE}/user/login`, null, 200);
        trackHandler(
            'get',
            `${TEST_MANAGEMENT_BASE}/user`,
            buildUser({
                roles: [{ scope: 'ORGANIZATION', permissions: { USER: ['R'] } }],
            }),
        );

        await useAuthStore.getState().login('bob', 'password');

        await waitFor(() => expect(envPermTracker.callCount).toBe(1));
        expect(permissionService.hasAnyOf(['environment-api-r'])).toBe(true);
    });
});
