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
import { applyUserTypeChange, isAddUserFormValid, resolvePreRegisterUserSource } from './userFormValidation';
import { GRAVITEE_IDP } from '../types/user';

const baseForm = {
    type: 'EXTERNAL_USER' as const,
    firstName: 'Jane',
    lastName: 'Doe',
    email: 'jane@company.com',
    source: GRAVITEE_IDP.id,
    sourceId: '',
};

describe('isAddUserFormValid', () => {
    it('accepts a valid external user', () => {
        expect(isAddUserFormValid(baseForm, { showIdentityProviderFields: false, identityProvidersReady: true })).toBe(true);
    });

    it('accepts a service account with email', () => {
        expect(
            isAddUserFormValid(
                { ...baseForm, type: 'SERVICE_ACCOUNT', firstName: '', lastName: 'Bat-AI', email: 'bot@company.com' },
                { showIdentityProviderFields: true, identityProvidersReady: true },
            ),
        ).toBe(true);
    });

    it('accepts a service account without email', () => {
        expect(
            isAddUserFormValid(
                { ...baseForm, type: 'SERVICE_ACCOUNT', firstName: '', lastName: 'Bat-AI', email: '' },
                { showIdentityProviderFields: true, identityProvidersReady: true },
            ),
        ).toBe(true);
    });

    it('does not require identity provider fields for service accounts', () => {
        expect(
            isAddUserFormValid(
                {
                    ...baseForm,
                    type: 'SERVICE_ACCOUNT',
                    firstName: '',
                    lastName: 'service11',
                    email: 'service11@gmail.com',
                    source: 'ldap',
                    sourceId: '',
                },
                { showIdentityProviderFields: true, identityProvidersReady: true },
            ),
        ).toBe(true);
    });

    it('requires identifier for external users with a non-gravitee provider', () => {
        expect(
            isAddUserFormValid(
                { ...baseForm, source: 'ldap', sourceId: '' },
                { showIdentityProviderFields: true, identityProvidersReady: true },
            ),
        ).toBe(false);
    });
});

describe('applyUserTypeChange', () => {
    it('clears identity provider state when switching to service account', () => {
        expect(applyUserTypeChange({ ...baseForm, source: 'ldap', sourceId: 'ldap-user-1' }, 'SERVICE_ACCOUNT')).toEqual({
            ...baseForm,
            type: 'SERVICE_ACCOUNT',
            firstName: '',
            source: GRAVITEE_IDP.id,
            sourceId: '',
        });
    });
});

describe('resolvePreRegisterUserSource', () => {
    it('uses gravitee for service accounts', () => {
        expect(resolvePreRegisterUserSource(true, true, 'ldap')).toBe(GRAVITEE_IDP.id);
    });

    it('uses the selected source when multiple identity providers are shown', () => {
        expect(resolvePreRegisterUserSource(false, true, 'ldap')).toBe('ldap');
    });

    it('uses gravitee when identity provider fields are hidden', () => {
        expect(resolvePreRegisterUserSource(false, false, 'ldap')).toBe(GRAVITEE_IDP.id);
    });
});
