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

import { permissionService } from '@gravitee/gamma-modules-sdk';

import { startPermissionSync } from './permission-sync';
import { buildUser, TEST_MANAGEMENT_BASE } from '../../testing/factories';
import { respondWithError, trackHandler } from '../../testing/helpers';
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

        useEnvironmentStore.getState().setEnvironment('test-org', 'staging');

        await waitFor(() => expect(tracker.callCount).toBe(1));
        expect(permissionService.hasAnyOf(['environment-api-r'])).toBe(true);
    });

    it('should not load environment permissions when no user is logged in', () => {
        const tracker = trackHandler('get', `${TEST_MANAGEMENT_BASE}/environments/staging/permissions`, {
            API: ['R'],
        });

        useEnvironmentStore.getState().setEnvironment('test-org', 'staging');

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

    it('should reload environment permissions on login when environmentId does not change', async () => {
        useEnvironmentStore.getState().setEnvironment('test-org', 'DEFAULT');
        const envPermTracker = trackHandler('get', `${TEST_MANAGEMENT_BASE}/environments/DEFAULT/permissions`, {
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
