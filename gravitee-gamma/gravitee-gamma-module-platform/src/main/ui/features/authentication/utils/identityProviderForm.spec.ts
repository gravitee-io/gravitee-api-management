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
    emptyIdentityProviderForm,
    formToCreatePayload,
    formWithType,
    IDENTITY_PROVIDER_NAME_MAX,
    IDENTITY_PROVIDER_NAME_MIN,
    validateIdentityProviderForm,
} from './identityProviderForm';

describe('identityProviderForm', () => {
    it('requires name, client id, and client secret', () => {
        expect(validateIdentityProviderForm(emptyIdentityProviderForm())).toEqual(
            expect.objectContaining({
                name: 'Identity provider name is required.',
                clientId: 'Client Id is required.',
                clientSecret: 'Client Secret is required.',
                serverURL: 'Server URL is required.',
                domain: 'Security domain is required.',
            }),
        );
    });

    it('rejects a name shorter than the classic minimum of 2 characters', () => {
        const form = { ...emptyIdentityProviderForm(), name: 'a' };
        expect(validateIdentityProviderForm(form).name).toBe(
            `The identity provider has to be at least ${IDENTITY_PROVIDER_NAME_MIN} characters long.`,
        );
        expect(validateIdentityProviderForm({ ...form, name: 'ab' }).name).toBeUndefined();
    });

    it('allows a 50-character name and rejects a longer one', () => {
        const form = { ...emptyIdentityProviderForm(), name: 'a'.repeat(IDENTITY_PROVIDER_NAME_MAX) };
        expect(validateIdentityProviderForm(form).name).toBeUndefined();
        expect(validateIdentityProviderForm({ ...form, name: 'a'.repeat(IDENTITY_PROVIDER_NAME_MAX + 1) }).name).toBe(
            `The identity provider has to be at most ${IDENTITY_PROVIDER_NAME_MAX} characters long.`,
        );
    });

    it('requires OIDC endpoints and at least one scope', () => {
        const form = formWithType(emptyIdentityProviderForm(), 'OIDC');
        form.name = 'Okta';
        form.configuration.clientId = 'id';
        form.configuration.clientSecret = 'secret';
        form.configuration.scopes = [];
        expect(validateIdentityProviderForm(form)).toEqual(
            expect.objectContaining({
                tokenEndpoint: 'Token Endpoint is required.',
                authorizeEndpoint: 'Authorize Endpoint is required.',
                userInfoEndpoint: 'UserInfo Endpoint is required.',
                scopes: 'Scopes are required.',
            }),
        );
    });

    it('omits user profile mapping for Google', () => {
        const form = formWithType(emptyIdentityProviderForm(), 'GOOGLE');
        form.name = 'Google SSO';
        form.configuration.clientId = 'id';
        form.configuration.clientSecret = 'secret';
        expect(formToCreatePayload(form).userProfileMapping).toBeUndefined();
        expect(formToCreatePayload(form).configuration).toEqual({ clientId: 'id', clientSecret: 'secret' });
    });

    it('includes AM configuration and user profile mapping', () => {
        const form = emptyIdentityProviderForm();
        form.name = 'AM';
        form.configuration.clientId = 'id';
        form.configuration.clientSecret = 'secret';
        form.configuration.serverURL = 'https://am.example';
        form.configuration.domain = 'auth';
        form.configuration.scopes = ['openid'];
        const payload = formToCreatePayload(form);
        expect(payload.userProfileMapping?.id).toBe('sub');
        expect(payload.configuration).toEqual(
            expect.objectContaining({
                serverURL: 'https://am.example',
                domain: 'auth',
                scopes: ['openid'],
            }),
        );
    });

    it('omits a color that is not a six-digit hex value', () => {
        const form = emptyIdentityProviderForm();
        form.name = 'AM';
        form.configuration.clientId = 'id';
        form.configuration.clientSecret = 'secret';
        form.configuration.serverURL = 'https://am.example';
        form.configuration.domain = 'auth';
        form.configuration.color = 'red';
        expect(formToCreatePayload(form).configuration.color).toBeUndefined();
        form.configuration.color = '#112233';
        expect(formToCreatePayload(form).configuration.color).toBe('#112233');
    });
});
