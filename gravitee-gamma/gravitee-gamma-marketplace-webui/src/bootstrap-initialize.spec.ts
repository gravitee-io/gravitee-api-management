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
import { useAuthStore } from './app/auth';
import { runApplicationBootstrap } from './bootstrap-initialize';
import { useBootstrapStore } from './shared/config/bootstrap.store';
import { TEST_CONFIG, TEST_PORTAL_API, buildUser } from './testing/factories';
import { resetAllStores, trackHandler } from './testing/helpers';

describe('runApplicationBootstrap', () => {
    beforeEach(() => {
        resetAllStores();
    });

    it('should leave the user anonymous when the portal session is missing', async () => {
        await runApplicationBootstrap();

        expect(useBootstrapStore.getState().config).toEqual(TEST_CONFIG);
        expect(useAuthStore.getState().user).toBeNull();
        expect(useAuthStore.getState().initialized).toBe(true);
    });

    it('should restore an existing portal session', async () => {
        trackHandler('get', `${TEST_PORTAL_API}/user`, buildUser());

        await runApplicationBootstrap();

        expect(useAuthStore.getState().user?.display_name).toBe('Jane Doe');
    });
});
