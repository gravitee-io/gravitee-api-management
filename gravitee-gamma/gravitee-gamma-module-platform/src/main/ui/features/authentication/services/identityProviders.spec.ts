/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import {
    createIdentityProvider,
    deleteIdentityProvider,
    getIdentityProvider,
    listActivatedIdentityProviders,
    listIdentityProviders,
    updateActivatedIdentityProviders,
    updateIdentityProvider,
} from './identityProviders';
import { apimFetchJsonOrg } from '../../../shared/api/apimClient';
import type { NewIdentityProviderPayload } from '../types/identityProvider';

jest.mock('../../../shared/api/apimClient', () => ({
    apimFetchJsonOrg: jest.fn(),
}));

const mockApimFetchJsonOrg = jest.mocked(apimFetchJsonOrg);

describe('identityProviders service', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        mockApimFetchJsonOrg.mockResolvedValue([]);
    });

    it('lists identity providers from the organization configuration resource', async () => {
        await listIdentityProviders();
        expect(mockApimFetchJsonOrg).toHaveBeenCalledWith('/configuration/identities');
    });

    it('lists activated identity providers from the organization identities resource', async () => {
        await listActivatedIdentityProviders();
        expect(mockApimFetchJsonOrg).toHaveBeenCalledWith('/identities');
    });

    it('PUTs activated identity provider ids', async () => {
        await updateActivatedIdentityProviders(['google', 'github']);
        expect(mockApimFetchJsonOrg).toHaveBeenCalledWith('/identities', {
            method: 'PUT',
            body: JSON.stringify([{ identityProvider: 'google' }, { identityProvider: 'github' }]),
        });
    });

    it('POSTs a new identity provider', async () => {
        const payload: NewIdentityProviderPayload = {
            name: 'Google SSO',
            type: 'GOOGLE',
            enabled: true,
            emailRequired: true,
            syncMappings: false,
            configuration: { clientId: 'id', clientSecret: 'secret' },
        };
        await createIdentityProvider(payload);
        expect(mockApimFetchJsonOrg).toHaveBeenCalledWith('/configuration/identities', {
            method: 'POST',
            body: JSON.stringify(payload),
        });
    });

    it('encodes special characters in the delete path', async () => {
        await deleteIdentityProvider('google/idp');
        expect(mockApimFetchJsonOrg).toHaveBeenCalledWith('/configuration/identities/google%2Fidp', { method: 'DELETE' });
    });

    it('GETs an identity provider and defaults missing mappings to empty arrays', async () => {
        mockApimFetchJsonOrg.mockResolvedValue({
            id: 'google-idp',
            name: 'Google',
            type: 'GOOGLE',
            enabled: true,
        });
        await expect(getIdentityProvider('google/idp')).resolves.toEqual({
            id: 'google-idp',
            name: 'Google',
            type: 'GOOGLE',
            enabled: true,
            groupMappings: [],
            roleMappings: [],
        });
        expect(mockApimFetchJsonOrg).toHaveBeenCalledWith('/configuration/identities/google%2Fidp');
    });

    it('PUTs an identity provider update payload', async () => {
        mockApimFetchJsonOrg.mockResolvedValue({
            id: 'google-idp',
            name: 'Google SSO',
            type: 'GOOGLE',
            enabled: false,
            groupMappings: [],
            roleMappings: [],
        });
        const payload = {
            name: 'Google SSO',
            enabled: false,
            emailRequired: true,
            syncMappings: false,
            configuration: { clientId: 'id', clientSecret: 'secret' },
            groupMappings: [{ condition: "{#jsonPath(#profile, '$.email')}", groups: ['group-1'] }],
            roleMappings: [],
        };
        await updateIdentityProvider('google-idp', payload);
        expect(mockApimFetchJsonOrg).toHaveBeenCalledWith('/configuration/identities/google-idp', {
            method: 'PUT',
            body: JSON.stringify(payload),
        });
    });
});
