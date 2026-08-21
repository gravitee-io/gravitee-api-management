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
    emptyRoleMapping,
    formToCreatePayload,
    formToUpdatePayload,
    formWithType,
    IDENTITY_PROVIDER_NAME_MAX,
    IDENTITY_PROVIDER_NAME_MIN,
    identityProviderToForm,
    isIdentityProviderFormDirty,
    validateIdentityProviderForm,
} from './identityProviderForm';
import type { IdentityProvider } from '../types/identityProvider';

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

    it('requires condition and groups once a group mapping is added', () => {
        const form = { ...emptyIdentityProviderForm(), name: 'AM', groupMappings: [{ condition: '', groups: [] }] };
        form.configuration.clientId = 'id';
        form.configuration.clientSecret = 'secret';
        form.configuration.serverURL = 'https://am.example';
        form.configuration.domain = 'auth';
        expect(validateIdentityProviderForm(form)).toEqual(
            expect.objectContaining({
                'groupMappings.0.condition': 'Condition is required.',
                'groupMappings.0.groups': 'At least one group is required.',
            }),
        );
    });

    it('requires condition and organization roles once a role mapping is added', () => {
        const form = {
            ...emptyIdentityProviderForm(),
            name: 'AM',
            roleMappings: [emptyRoleMapping(['DEFAULT'])],
        };
        form.configuration.clientId = 'id';
        form.configuration.clientSecret = 'secret';
        form.configuration.serverURL = 'https://am.example';
        form.configuration.domain = 'auth';
        expect(validateIdentityProviderForm(form)).toEqual(
            expect.objectContaining({
                'roleMappings.0.condition': 'Condition is required.',
                'roleMappings.0.organizations': 'At least one organization role is required.',
            }),
        );
    });

    it('maps a loaded provider onto the form and back onto the update payload', () => {
        const provider: IdentityProvider = {
            id: 'am-idp',
            name: 'AM',
            description: 'Org AM',
            type: 'GRAVITEEIO_AM',
            enabled: false,
            emailRequired: false,
            syncMappings: true,
            configuration: {
                clientId: 'id',
                clientSecret: 'secret',
                serverURL: 'https://am.example',
                domain: 'auth',
                scopes: ['openid'],
                color: '#112233',
            },
            userProfileMapping: { id: 'sub', firstname: 'given_name' },
            groupMappings: [{ condition: "{#jsonPath(#profile, '$.email')}", groups: ['group-a'] }],
            roleMappings: [
                {
                    condition: "{#jsonPath(#profile, '$.job')}",
                    organizations: ['ADMIN'],
                    environments: { DEFAULT: ['USER'] },
                },
            ],
        };
        const form = identityProviderToForm(provider, ['DEFAULT', 'prod']);
        expect(form.roleMappings[0]?.environments).toEqual({ DEFAULT: ['USER'], prod: [] });
        expect(formToUpdatePayload(form)).toEqual({
            name: 'AM',
            description: 'Org AM',
            enabled: false,
            emailRequired: false,
            syncMappings: true,
            configuration: {
                clientId: 'id',
                clientSecret: 'secret',
                serverURL: 'https://am.example',
                domain: 'auth',
                scopes: ['openid'],
                color: '#112233',
            },
            userProfileMapping: { id: 'sub', firstname: 'given_name' },
            groupMappings: [{ condition: "{#jsonPath(#profile, '$.email')}", groups: ['group-a'] }],
            roleMappings: [
                {
                    condition: "{#jsonPath(#profile, '$.job')}",
                    organizations: ['ADMIN'],
                    environments: { DEFAULT: ['USER'], prod: [] },
                },
            ],
        });
        expect(isIdentityProviderFormDirty(form, form)).toBe(false);
        expect(isIdentityProviderFormDirty({ ...form, name: 'AM 2' }, form)).toBe(true);
    });

    it('treats reordered group and role selections as the same payload', () => {
        const form = identityProviderToForm(
            {
                id: 'am-idp',
                name: 'AM',
                type: 'GRAVITEEIO_AM',
                enabled: true,
                configuration: {
                    clientId: 'id',
                    clientSecret: 'secret',
                    serverURL: 'https://am.example',
                    domain: 'auth',
                    scopes: ['openid', 'profile'],
                },
                groupMappings: [{ condition: "{#jsonPath(#profile, '$.email')}", groups: ['group-a', 'group-b'] }],
                roleMappings: [
                    {
                        condition: "{#jsonPath(#profile, '$.job')}",
                        organizations: ['ADMIN', 'USER'],
                        environments: { DEFAULT: ['USER', 'API_PUBLISHER'], prod: [] },
                    },
                ],
            },
            ['DEFAULT', 'prod'],
        );
        const reordered: typeof form = {
            ...form,
            groupMappings: [{ condition: form.groupMappings[0]!.condition, groups: ['group-b', 'group-a'] }],
            roleMappings: [
                {
                    condition: form.roleMappings[0]!.condition,
                    organizations: ['USER', 'ADMIN'],
                    environments: { prod: [], DEFAULT: ['API_PUBLISHER', 'USER'] },
                },
            ],
        };
        expect(isIdentityProviderFormDirty(reordered, form)).toBe(false);
    });

    it('treats reordered scopes as a dirty payload', () => {
        const form = identityProviderToForm({
            id: 'am-idp',
            name: 'AM',
            type: 'GRAVITEEIO_AM',
            enabled: true,
            configuration: {
                clientId: 'id',
                clientSecret: 'secret',
                serverURL: 'https://am.example',
                domain: 'auth',
                scopes: ['openid', 'profile'],
            },
            groupMappings: [],
            roleMappings: [],
        });
        expect(
            isIdentityProviderFormDirty({ ...form, configuration: { ...form.configuration, scopes: ['profile', 'openid'] } }, form),
        ).toBe(true);
    });

    it('rejects duplicate mapping conditions', () => {
        const form = emptyIdentityProviderForm();
        form.name = 'AM';
        form.configuration.clientId = 'id';
        form.configuration.clientSecret = 'secret';
        form.configuration.serverURL = 'https://am.example';
        form.configuration.domain = 'auth';
        form.groupMappings = [
            { condition: "{#jsonPath(#profile, '$.email')}", groups: ['group-a'] },
            { condition: "{#jsonPath(#profile, '$.email')}", groups: ['group-b'] },
        ];
        form.roleMappings = [
            { condition: "{#jsonPath(#profile, '$.job')}", organizations: ['ADMIN'], environments: { DEFAULT: [] } },
            { condition: "{#jsonPath(#profile, '$.job')}", organizations: ['USER'], environments: { DEFAULT: [] } },
        ];
        expect(validateIdentityProviderForm(form)).toEqual(
            expect.objectContaining({
                'groupMappings.0.condition': 'Condition must be unique.',
                'groupMappings.1.condition': 'Condition must be unique.',
                'roleMappings.0.condition': 'Condition must be unique.',
                'roleMappings.1.condition': 'Condition must be unique.',
            }),
        );
    });

    it('trims client secret on create and update payloads', () => {
        const form = formWithType(emptyIdentityProviderForm(), 'GOOGLE');
        form.name = 'Google SSO';
        form.configuration.clientId = 'id';
        form.configuration.clientSecret = '  secret  ';
        expect(formToCreatePayload(form).configuration.clientSecret).toBe('secret');
        expect(formToUpdatePayload(form).configuration.clientSecret).toBe('secret');
    });

    it('omits empty user profile mapping on update for Google', () => {
        const form = identityProviderToForm(
            {
                id: 'google-idp',
                name: 'Google',
                type: 'GOOGLE',
                enabled: true,
                configuration: { clientId: 'id', clientSecret: 'secret' },
                groupMappings: [],
                roleMappings: [],
            },
            ['DEFAULT'],
        );
        expect(formToUpdatePayload(form).userProfileMapping).toBeUndefined();
    });

    it('includes user profile mapping on update for Google even though create omits it', () => {
        const form = identityProviderToForm(
            {
                id: 'google-idp',
                name: 'Google',
                type: 'GOOGLE',
                enabled: true,
                configuration: { clientId: 'id', clientSecret: 'secret' },
                userProfileMapping: { id: 'email', firstname: 'given_name' },
                groupMappings: [],
                roleMappings: [],
            },
            ['DEFAULT'],
        );
        expect(formToCreatePayload(form).userProfileMapping).toBeUndefined();
        expect(formToUpdatePayload(form).userProfileMapping).toEqual({ id: 'email', firstname: 'given_name' });
    });

    it('includes OIDC endpoints and user profile mapping on update', () => {
        const form = identityProviderToForm({
            id: 'oidc-idp',
            name: 'Okta',
            type: 'OIDC',
            enabled: true,
            configuration: {
                clientId: 'id',
                clientSecret: 'secret',
                tokenEndpoint: 'https://okta.example/token',
                authorizeEndpoint: 'https://okta.example/authorize',
                userInfoEndpoint: 'https://okta.example/userinfo',
                scopes: ['openid'],
            },
            userProfileMapping: { id: 'sub', email: 'email' },
            groupMappings: [],
            roleMappings: [],
        });
        expect(formToUpdatePayload(form)).toEqual(
            expect.objectContaining({
                name: 'Okta',
                userProfileMapping: { id: 'sub', email: 'email' },
                configuration: expect.objectContaining({
                    tokenEndpoint: 'https://okta.example/token',
                    authorizeEndpoint: 'https://okta.example/authorize',
                    userInfoEndpoint: 'https://okta.example/userinfo',
                    scopes: ['openid'],
                }),
            }),
        );
    });
});
