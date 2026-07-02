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
import { licenseService } from '@gravitee/gamma-modules-sdk';

import { runApplicationBootstrap } from './bootstrap-initialize';
import { useAuthStore } from './features/auth/auth.store';
import { useEnvironmentStore } from './features/environment/environment.store';
import * as permissionSync from './features/permissions/permission-sync';
import { TEST_ENVIRONMENTS, TEST_MANAGEMENT_BASE, TEST_MANAGEMENT_V2_ORGANIZATION_BASE } from './testing/factories';
import { trackHandler, respondWith, respondWithError } from './testing/helpers';

describe('runApplicationBootstrap', () => {
    let startSyncSpy: jest.SpyInstance;

    beforeEach(() => {
        startSyncSpy = jest.spyOn(permissionSync, 'startPermissionSync').mockReturnValue(jest.fn());
        licenseService.setLicense(null);
    });

    afterEach(() => {
        startSyncSpy.mockRestore();
    });

    it('should not request environments when the user is not authenticated', async () => {
        respondWithError('get', `${TEST_MANAGEMENT_BASE}/user`, 401);
        const envListTracker = trackHandler('get', `${TEST_MANAGEMENT_BASE}/environments`, TEST_ENVIRONMENTS);

        await runApplicationBootstrap();

        expect(useAuthStore.getState().user).toBeNull();
        expect(envListTracker.callCount).toBe(0);
        expect(useEnvironmentStore.getState().initialized).toBe(false);
        expect(licenseService.getLicense()).toBeNull();
    });

    it('should load environments and the organization license when a session is already present', async () => {
        const envListTracker = trackHandler('get', `${TEST_MANAGEMENT_BASE}/environments`, TEST_ENVIRONMENTS);
        respondWith('get', `${TEST_MANAGEMENT_V2_ORGANIZATION_BASE}/license`, { tier: 'enterprise', packs: [], features: [] });

        await runApplicationBootstrap();

        expect(useAuthStore.getState().user).toBeTruthy();
        expect(envListTracker.callCount).toBe(1);
        expect(useEnvironmentStore.getState().environments).toEqual(TEST_ENVIRONMENTS);
        expect(useEnvironmentStore.getState().initialized).toBe(true);
        expect(licenseService.getLicense()?.tier).toBe('enterprise');
    });
});
