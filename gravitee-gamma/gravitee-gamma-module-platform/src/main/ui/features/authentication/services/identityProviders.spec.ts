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
    listActivatedIdentityProviders,
    listIdentityProviders,
    updateActivatedIdentityProviders,
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
});
