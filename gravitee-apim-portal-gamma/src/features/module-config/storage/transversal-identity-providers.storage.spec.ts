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
import { clearPortalsDatabase } from '../../portals/storage/portals.storage.test-utils';
import {
    createTransversalIdentityProvider,
    deleteTransversalIdentityProvider,
    listTransversalIdentityProviders,
    setTransversalIdentityProviderEnabled,
    updateTransversalIdentityProvider,
} from './transversal-identity-providers.storage';

describe('transversal-identity-providers.storage', () => {
    beforeEach(async () => {
        await clearPortalsDatabase();
    });

    afterEach(async () => {
        await clearPortalsDatabase();
    });

    it('should create and list transversal identity providers', async () => {
        const provider = await createTransversalIdentityProvider({
            type: 'GOOGLE',
            name: 'Org Google',
            description: 'Shared Google SSO',
            portalIds: ['portal-1', 'portal-2'],
            configuration: { clientId: 'client-1', clientSecret: 'secret-1' },
        });

        expect(provider).toMatchObject({
            type: 'GOOGLE',
            name: 'Org Google',
            description: 'Shared Google SSO',
            enabled: true,
            portalIds: ['portal-1', 'portal-2'],
            configuration: expect.objectContaining({
                clientId: 'client-1',
                clientSecret: 'secret-1',
            }),
        });

        const listed = await listTransversalIdentityProviders();
        expect(listed).toHaveLength(1);
        expect(listed[0]?.id).toBe(provider.id);
    });

    it('should update configuration, portals, and enabled flag', async () => {
        const provider = await createTransversalIdentityProvider({
            type: 'OIDC',
            name: 'OIDC',
            portalIds: ['portal-1'],
            configuration: { clientId: 'c1', clientSecret: 's1' },
        });

        await updateTransversalIdentityProvider(provider.id, {
            name: 'Corporate OIDC',
            portalIds: ['portal-1', 'portal-3'],
            configuration: { color: '#112233' },
        });
        await setTransversalIdentityProviderEnabled(provider.id, false);

        const listed = await listTransversalIdentityProviders();
        expect(listed[0]).toMatchObject({
            name: 'Corporate OIDC',
            enabled: false,
            portalIds: ['portal-1', 'portal-3'],
            configuration: expect.objectContaining({
                clientId: 'c1',
                color: '#112233',
            }),
        });
    });

    it('should delete a transversal identity provider', async () => {
        const provider = await createTransversalIdentityProvider({
            type: 'GITHUB',
            name: 'GitHub',
            configuration: { clientId: 'a', clientSecret: 'b' },
        });

        await deleteTransversalIdentityProvider(provider.id);

        expect(await listTransversalIdentityProviders()).toEqual([]);
    });
});
