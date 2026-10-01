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
import { useEnvironmentStore } from '../../features/environment/environment.store';
import { buildEnvironment } from '../../testing/factories';
import { resetAllStores } from '../../testing/helpers';
import {
    addCloudEnvironment,
    rehydrateCloudEnvironments,
    retainDefaultEnvironmentOnly,
    useCloudEnvironmentStore,
} from './cloud-environment.store';

describe('cloud-environment.store', () => {
    beforeEach(() => {
        resetAllStores();
        useEnvironmentStore.setState({
            organizationId: 'org-1',
            environments: [buildEnvironment({ id: 'env-default', name: 'Default', hrids: ['default'] })],
            currentEnvironment: buildEnvironment({ id: 'env-default', name: 'Default', hrids: ['default'] }),
            environmentId: 'env-default',
            initialized: true,
            loading: false,
        });
    });

    it('inserts created cloud environments directly under the default environment', () => {
        const created = addCloudEnvironment({ name: 'Staging', hrid: 'staging', product: 'APIM' });

        expect(created).toBeTruthy();
        expect(useEnvironmentStore.getState().environments.map(env => env.name)).toEqual(['Default', 'Staging']);
        expect(useCloudEnvironmentStore.getState().productByEnvironmentId[created!.id]).toBe('APIM');
    });

    it('rehydrates cloud environments from session storage after reload', () => {
        const created = addCloudEnvironment({ name: 'Staging', hrid: 'staging', product: 'APIM' });
        expect(created).toBeTruthy();

        useEnvironmentStore.getState().reset();
        useCloudEnvironmentStore.setState({ productByEnvironmentId: {} });
        useEnvironmentStore.setState({
            organizationId: 'org-1',
            environments: [buildEnvironment({ id: 'env-default', name: 'Default', hrids: ['default'] })],
            currentEnvironment: buildEnvironment({ id: 'env-default', name: 'Default', hrids: ['default'] }),
            environmentId: 'env-default',
            initialized: true,
            loading: false,
        });

        rehydrateCloudEnvironments();

        expect(useEnvironmentStore.getState().environments.map(env => env.name)).toEqual(['Default', 'Staging']);
        expect(useCloudEnvironmentStore.getState().productByEnvironmentId[created!.id]).toBe('APIM');
    });

    it('retains only the default environment and clears cloud session data', () => {
        addCloudEnvironment({ name: 'Staging', hrid: 'staging', product: 'APIM' });
        addCloudEnvironment({ name: 'Development AM', hrid: 'development-am', product: 'AM' });

        retainDefaultEnvironmentOnly();

        expect(useEnvironmentStore.getState().environments.map(env => env.name)).toEqual(['Default']);
        expect(useCloudEnvironmentStore.getState().productByEnvironmentId).toEqual({});
        expect(window.sessionStorage.getItem('gamma.cloud.created-environments')).toBeNull();
    });

    it('returns null when the HRID already exists', () => {
        addCloudEnvironment({ name: 'Staging', hrid: 'staging', product: 'APIM' });
        const duplicate = addCloudEnvironment({ name: 'Staging 2', hrid: 'staging', product: 'APIM' });

        expect(duplicate).toBeNull();
        expect(useEnvironmentStore.getState().environments).toHaveLength(2);
    });
});
