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
import { http, HttpResponse } from 'msw';

import { useEnvironmentStore } from './environment.store';
import { TEST_ENVIRONMENTS, TEST_MANAGEMENT_BASE } from '../../testing/factories';
import { resetAllStores, trackHandler, respondWithError, seedBootstrap } from '../../testing/helpers';
import { server } from '../../testing/server';

describe('environmentStore', () => {
    beforeEach(() => {
        resetAllStores();
        seedBootstrap();
    });

    it('should fetch environments on initialize and set first as current', async () => {
        await useEnvironmentStore.getState().initialize('test-org');

        expect(useEnvironmentStore.getState().environments).toEqual(TEST_ENVIRONMENTS);
        expect(useEnvironmentStore.getState().currentEnvironment).toEqual(TEST_ENVIRONMENTS[0]);
        expect(useEnvironmentStore.getState().environmentId).toBe('env-1-id');
    });

    it('should not refetch if already initialized', async () => {
        const tracker = trackHandler('get', `${TEST_MANAGEMENT_BASE}/environments`, TEST_ENVIRONMENTS);

        await useEnvironmentStore.getState().initialize('test-org');
        await useEnvironmentStore.getState().initialize('test-org');

        expect(tracker.callCount).toBe(1);
    });

    it('should set error on API failure during initialize', async () => {
        respondWithError('get', `${TEST_MANAGEMENT_BASE}/environments`, 500);

        await useEnvironmentStore.getState().initialize('test-org');

        expect(useEnvironmentStore.getState().error).toBeTruthy();
        expect(useEnvironmentStore.getState().environments).toEqual([]);
    });

    it('should set error when environment list is empty', async () => {
        server.use(http.get(`${TEST_MANAGEMENT_BASE}/environments`, () => HttpResponse.json([])));

        await useEnvironmentStore.getState().initialize('test-org');

        expect(useEnvironmentStore.getState().error?.message).toBe('No environment found!');
        expect(useEnvironmentStore.getState().environments).toEqual([]);
    });

    it('resolveEnvironment should find by hrid', async () => {
        await useEnvironmentStore.getState().initialize('test-org');

        expect(useEnvironmentStore.getState().resolveEnvironment('env-2')?.id).toBe('env-2-id');
    });

    it('resolveEnvironment should find by id (case insensitive)', async () => {
        await useEnvironmentStore.getState().initialize('test-org');

        expect(useEnvironmentStore.getState().resolveEnvironment('ENV-1-ID')?.id).toBe('env-1-id');
    });

    it('resolveEnvironment should return null for unknown value', async () => {
        await useEnvironmentStore.getState().initialize('test-org');

        expect(useEnvironmentStore.getState().resolveEnvironment('nope')).toBeNull();
    });

    it('setCurrentEnvironment should update currentEnvironment and environmentId', async () => {
        await useEnvironmentStore.getState().initialize('test-org');
        const second = TEST_ENVIRONMENTS[1]!;

        useEnvironmentStore.getState().setCurrentEnvironment(second);

        expect(useEnvironmentStore.getState().currentEnvironment).toEqual(second);
        expect(useEnvironmentStore.getState().environmentId).toBe('env-2-id');
    });
});
