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
import { useBootstrapStore } from './bootstrap.store';
import { TEST_CONFIG } from '../../testing/factories';
import { trackHandler, respondWithError, resetAllStores } from '../../testing/helpers';

describe('bootstrapStore', () => {
    beforeEach(() => {
        // Reset all stores, including bootstrap store, for bootstrap store tests
        resetAllStores();
    });
    it('should fetch and store config', async () => {
        await useBootstrapStore.getState().initialize();

        const { config, loading, error } = useBootstrapStore.getState();
        expect(config).toEqual(TEST_CONFIG);
        expect(loading).toBe(false);
        expect(error).toBeNull();
    });

    it('should not refetch if already initialized', async () => {
        const tracker = trackHandler('get', '/constants.json', { gammaBaseURL: TEST_CONFIG.gammaBaseURL });

        await useBootstrapStore.getState().initialize();
        await useBootstrapStore.getState().initialize();

        expect(tracker.callCount).toBe(1);
    });

    it('should set error on bootstrap failure', async () => {
        respondWithError('get', '/constants.json', 500);

        await expect(useBootstrapStore.getState().initialize()).rejects.toThrow();
        expect(useBootstrapStore.getState().error).toBeTruthy();
        expect(useBootstrapStore.getState().config).toBeNull();
    });
});
