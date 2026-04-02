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
import { useAuthStore } from './auth.store';
import { TEST_MANAGEMENT_BASE, buildUser } from '../../testing/factories';
import { respondWithError, trackHandler } from '../../testing/helpers';

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
    });

    it('should clear user on logout', async () => {
        useAuthStore.setState({ user: buildUser() });

        await useAuthStore.getState().logout();

        expect(useAuthStore.getState().user).toBeNull();
    });
});
