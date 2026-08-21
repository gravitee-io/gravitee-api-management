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

import type { IdentityProviderType } from '../types/identityProvider';

export const IDENTITY_PROVIDER_TYPES: { value: IdentityProviderType; label: string }[] = [
    { value: 'GRAVITEEIO_AM', label: 'Gravitee.io AM' },
    { value: 'OIDC', label: 'OpenID Connect' },
    { value: 'GOOGLE', label: 'Google' },
    { value: 'GITHUB', label: 'GitHub' },
];

export const DEFAULT_USER_PROFILE = {
    id: 'sub',
    firstname: 'given_name',
    lastname: 'family_name',
    email: 'email',
    picture: 'picture',
} as const;

export const LOCAL_LOGIN_READONLY_PROPERTY = 'console.authentication.localLogin.enabled';
export const SYSTEM_READONLY_TOOLTIP = 'Configuration provided by the system';
export const LOCAL_LOGIN_NEEDS_ACTIVATED_IDP_TOOLTIP =
    'You must create and activate an identity provider to be able to update this setting';
export const LOCAL_LOGIN_LOAD_FAILED_TOOLTIP = 'Identity provider settings could not be loaded';
export const LOCAL_LOGIN_NO_PERMISSION_TOOLTIP =
    'You do not have permission to modify these settings. Contact your administrator for access.';

export function localLoginSettingTooltip({
    isLoading,
    isError,
    canUpdateSettings,
    systemReadonly,
    hasActivatedIdp,
}: {
    readonly isLoading: boolean;
    readonly isError: boolean;
    readonly canUpdateSettings: boolean;
    readonly systemReadonly: boolean;
    readonly hasActivatedIdp: boolean;
}): string | null {
    if (isLoading) {
        return null;
    }
    if (isError) {
        return LOCAL_LOGIN_LOAD_FAILED_TOOLTIP;
    }
    if (systemReadonly) {
        return SYSTEM_READONLY_TOOLTIP;
    }
    if (!canUpdateSettings) {
        return LOCAL_LOGIN_NO_PERMISSION_TOOLTIP;
    }
    if (!hasActivatedIdp) {
        return LOCAL_LOGIN_NEEDS_ACTIVATED_IDP_TOOLTIP;
    }
    return null;
}

export function identityProviderTypeLabel(type: IdentityProviderType): string {
    return IDENTITY_PROVIDER_TYPES.find(item => item.value === type)?.label ?? type;
}

export function hasUserProfileMapping(type: IdentityProviderType): boolean {
    return type === 'GRAVITEEIO_AM' || type === 'OIDC';
}

export function isLocalLoginReadonly(readonly: readonly string[] | undefined): boolean {
    return (readonly ?? []).includes(LOCAL_LOGIN_READONLY_PROPERTY);
}
