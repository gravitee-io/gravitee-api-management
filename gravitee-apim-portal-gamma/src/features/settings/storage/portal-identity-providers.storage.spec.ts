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
    createPortalIdentityProvider,
    deleteIdentityProvidersForPortal,
    getEnabledIdentityProvidersByPortalId,
    getIdentityProvidersByPortalId,
    setIdentityProviderEnabled,
    updatePortalIdentityProvider,
} from './portal-identity-providers.storage';

describe('portal-identity-providers.storage', () => {
    beforeEach(async () => {
        await clearPortalsDatabase();
    });

    afterEach(async () => {
        await clearPortalsDatabase();
    });

    it('should create and list identity providers for a portal', async () => {
        const provider = await createPortalIdentityProvider('portal-1', {
            type: 'GOOGLE',
            name: 'Corporate Google',
            description: 'Google SSO',
            configuration: { clientId: 'client-1', clientSecret: 'secret-1' },
        });

        expect(provider).toMatchObject({
            portalId: 'portal-1',
            type: 'GOOGLE',
            name: 'Corporate Google',
            description: 'Google SSO',
            enabled: true,
            syncMappings: false,
            emailRequired: true,
            configuration: expect.objectContaining({
                clientId: 'client-1',
                clientSecret: 'secret-1',
            }),
        });

        const listed = await getIdentityProvidersByPortalId('portal-1');
        expect(listed).toHaveLength(1);
        expect(listed[0]?.id).toBe(provider.id);
    });

    it('should update configuration and enabled flag', async () => {
        const provider = await createPortalIdentityProvider('portal-1', {
            type: 'OIDC',
            name: 'OIDC',
            configuration: {
                clientId: 'c1',
                clientSecret: 's1',
                authorizeEndpoint: 'https://auth.example/authorize',
                tokenEndpoint: 'https://auth.example/token',
                userInfoEndpoint: 'https://auth.example/userinfo',
            },
        });

        await updatePortalIdentityProvider(provider.id, {
            name: 'Corporate OIDC',
            configuration: { color: '#112233' },
        });
        await setIdentityProviderEnabled(provider.id, false);

        const listed = await getIdentityProvidersByPortalId('portal-1');
        expect(listed[0]).toMatchObject({
            name: 'Corporate OIDC',
            enabled: false,
            configuration: expect.objectContaining({
                clientId: 'c1',
                color: '#112233',
                authorizeEndpoint: 'https://auth.example/authorize',
            }),
        });
    });

    it('should list only enabled providers', async () => {
        await createPortalIdentityProvider('portal-1', {
            type: 'GOOGLE',
            name: 'Enabled',
            configuration: { clientId: 'a', clientSecret: 'b' },
        });
        const disabled = await createPortalIdentityProvider('portal-1', {
            type: 'GITHUB',
            name: 'Disabled',
            configuration: { clientId: 'c', clientSecret: 'd' },
        });
        await setIdentityProviderEnabled(disabled.id, false);

        const enabled = await getEnabledIdentityProvidersByPortalId('portal-1');
        expect(enabled).toHaveLength(1);
        expect(enabled[0]?.name).toBe('Enabled');
    });

    it('should delete all identity providers for a portal', async () => {
        await createPortalIdentityProvider('portal-1', {
            type: 'GOOGLE',
            name: 'A',
            configuration: { clientId: 'a', clientSecret: 'b' },
        });
        await createPortalIdentityProvider('portal-1', {
            type: 'GITHUB',
            name: 'B',
            configuration: { clientId: 'c', clientSecret: 'd' },
        });
        await createPortalIdentityProvider('portal-2', {
            type: 'GOOGLE',
            name: 'Other',
            configuration: { clientId: 'e', clientSecret: 'f' },
        });

        await deleteIdentityProvidersForPortal('portal-1');

        expect(await getIdentityProvidersByPortalId('portal-1')).toEqual([]);
        expect(await getIdentityProvidersByPortalId('portal-2')).toHaveLength(1);
    });
});
