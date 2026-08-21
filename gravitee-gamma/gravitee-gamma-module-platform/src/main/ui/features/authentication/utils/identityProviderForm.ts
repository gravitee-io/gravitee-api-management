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

import { DEFAULT_USER_PROFILE, hasUserProfileMapping } from './identityProviderDisplay';
import type {
    IdentityProviderConfiguration,
    IdentityProviderType,
    IdentityProviderUserProfileMapping,
    NewIdentityProviderPayload,
} from '../types/identityProvider';

export const IDENTITY_PROVIDER_NAME_MIN = 2;
export const IDENTITY_PROVIDER_NAME_MAX = 50;

export interface IdentityProviderFormState {
    type: IdentityProviderType;
    name: string;
    description: string;
    enabled: boolean;
    emailRequired: boolean;
    syncMappings: boolean;
    configuration: IdentityProviderConfiguration;
    userProfileMapping: IdentityProviderUserProfileMapping;
}

export function emptyConfiguration(type: IdentityProviderType): IdentityProviderConfiguration {
    if (type === 'GRAVITEEIO_AM') {
        return { clientId: '', clientSecret: '', serverURL: '', domain: '', scopes: [], color: '' };
    }
    if (type === 'OIDC') {
        return {
            clientId: '',
            clientSecret: '',
            tokenEndpoint: '',
            tokenIntrospectionEndpoint: '',
            authorizeEndpoint: '',
            userInfoEndpoint: '',
            userLogoutEndpoint: '',
            scopes: ['openid', 'profile', 'email'],
            color: '',
        };
    }
    return { clientId: '', clientSecret: '' };
}

export function emptyIdentityProviderForm(): IdentityProviderFormState {
    return {
        type: 'GRAVITEEIO_AM',
        name: '',
        description: '',
        enabled: true,
        emailRequired: true,
        syncMappings: false,
        configuration: emptyConfiguration('GRAVITEEIO_AM'),
        userProfileMapping: { ...DEFAULT_USER_PROFILE },
    };
}

export function formWithType(form: IdentityProviderFormState, type: IdentityProviderType): IdentityProviderFormState {
    return {
        ...form,
        type,
        configuration: emptyConfiguration(type),
        userProfileMapping: { ...DEFAULT_USER_PROFILE },
    };
}

export function validateIdentityProviderForm(form: IdentityProviderFormState): Record<string, string> {
    const errors: Record<string, string> = {};
    const name = form.name.trim();
    if (name === '') errors.name = 'Identity provider name is required.';
    else if (name.length < IDENTITY_PROVIDER_NAME_MIN)
        errors.name = `The identity provider has to be at least ${IDENTITY_PROVIDER_NAME_MIN} characters long.`;
    else if (name.length > IDENTITY_PROVIDER_NAME_MAX)
        errors.name = `The identity provider has to be at most ${IDENTITY_PROVIDER_NAME_MAX} characters long.`;

    if (!form.configuration.clientId.trim()) errors.clientId = 'Client Id is required.';
    if (!form.configuration.clientSecret.trim()) errors.clientSecret = 'Client Secret is required.';

    if (form.type === 'GRAVITEEIO_AM') {
        if (!form.configuration.serverURL?.trim()) errors.serverURL = 'Server URL is required.';
        if (!form.configuration.domain?.trim()) errors.domain = 'Security domain is required.';
        if (!form.userProfileMapping.id.trim()) errors.profileId = 'ID is required.';
    }

    if (form.type === 'OIDC') {
        if (!form.configuration.tokenEndpoint?.trim()) errors.tokenEndpoint = 'Token Endpoint is required.';
        if (!form.configuration.authorizeEndpoint?.trim()) errors.authorizeEndpoint = 'Authorize Endpoint is required.';
        if (!form.configuration.userInfoEndpoint?.trim()) errors.userInfoEndpoint = 'UserInfo Endpoint is required.';
        if ((form.configuration.scopes ?? []).length === 0) errors.scopes = 'Scopes are required.';
        if (!form.userProfileMapping.id.trim()) errors.profileId = 'ID is required.';
    }

    return errors;
}

function optionalTrimmed(value: string | undefined): string | undefined {
    const trimmed = value?.trim() ?? '';
    return trimmed === '' ? undefined : trimmed;
}

function optionalHexColor(value: string | undefined): string | undefined {
    const trimmed = value?.trim() ?? '';
    return /^#([0-9a-fA-F]{6})$/.test(trimmed) ? trimmed : undefined;
}

function compactConfiguration(form: IdentityProviderFormState): IdentityProviderConfiguration {
    const clientId = form.configuration.clientId.trim();
    const clientSecret = form.configuration.clientSecret;
    const color = optionalHexColor(form.configuration.color);
    if (form.type === 'GOOGLE' || form.type === 'GITHUB') {
        return { clientId, clientSecret };
    }
    if (form.type === 'GRAVITEEIO_AM') {
        return {
            clientId,
            clientSecret,
            serverURL: form.configuration.serverURL?.trim() ?? '',
            domain: form.configuration.domain?.trim() ?? '',
            scopes: form.configuration.scopes ? [...form.configuration.scopes] : [],
            ...(color ? { color } : {}),
        };
    }
    return {
        clientId,
        clientSecret,
        tokenEndpoint: form.configuration.tokenEndpoint?.trim() ?? '',
        tokenIntrospectionEndpoint: optionalTrimmed(form.configuration.tokenIntrospectionEndpoint),
        authorizeEndpoint: form.configuration.authorizeEndpoint?.trim() ?? '',
        userInfoEndpoint: form.configuration.userInfoEndpoint?.trim() ?? '',
        userLogoutEndpoint: optionalTrimmed(form.configuration.userLogoutEndpoint),
        scopes: form.configuration.scopes ? [...form.configuration.scopes] : [],
        ...(color ? { color } : {}),
    };
}

export function formToCreatePayload(form: IdentityProviderFormState): NewIdentityProviderPayload {
    const payload: NewIdentityProviderPayload = {
        name: form.name.trim(),
        description: form.description.trim(),
        type: form.type,
        enabled: form.enabled,
        emailRequired: form.emailRequired,
        syncMappings: form.syncMappings,
        configuration: compactConfiguration(form),
    };
    if (hasUserProfileMapping(form.type)) {
        payload.userProfileMapping = {
            id: form.userProfileMapping.id.trim(),
            firstname: optionalTrimmed(form.userProfileMapping.firstname),
            lastname: optionalTrimmed(form.userProfileMapping.lastname),
            email: optionalTrimmed(form.userProfileMapping.email),
            picture: optionalTrimmed(form.userProfileMapping.picture),
        };
    }
    return payload;
}
