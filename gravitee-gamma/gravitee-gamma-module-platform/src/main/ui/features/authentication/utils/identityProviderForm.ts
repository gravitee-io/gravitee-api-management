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
    GroupMapping,
    IdentityProvider,
    IdentityProviderConfiguration,
    IdentityProviderType,
    IdentityProviderUserProfileMapping,
    NewIdentityProviderPayload,
    RoleMapping,
    UpdateIdentityProviderPayload,
} from '../types/identityProvider';

export const IDENTITY_PROVIDER_NAME_MIN = 2;
export const IDENTITY_PROVIDER_NAME_MAX = 50;

export interface IdentityProviderGroupMappingForm {
    condition: string;
    groups: string[];
}

export interface IdentityProviderRoleMappingForm {
    condition: string;
    organizations: string[];
    environments: Record<string, string[]>;
}

export interface IdentityProviderFormState {
    type: IdentityProviderType;
    name: string;
    description: string;
    enabled: boolean;
    emailRequired: boolean;
    syncMappings: boolean;
    configuration: IdentityProviderConfiguration;
    userProfileMapping: IdentityProviderUserProfileMapping;
    groupMappings: IdentityProviderGroupMappingForm[];
    roleMappings: IdentityProviderRoleMappingForm[];
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

export function emptyGroupMapping(): IdentityProviderGroupMappingForm {
    return { condition: '', groups: [] };
}

export function emptyRoleMapping(environmentIds: readonly string[]): IdentityProviderRoleMappingForm {
    return {
        condition: '',
        organizations: [],
        environments: Object.fromEntries(environmentIds.map(environmentId => [environmentId, []])),
    };
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
        groupMappings: [],
        roleMappings: [],
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

    form.groupMappings.forEach((mapping, index) => {
        if (!mapping.condition.trim()) errors[`groupMappings.${index}.condition`] = 'Condition is required.';
        if (mapping.groups.length === 0) errors[`groupMappings.${index}.groups`] = 'At least one group is required.';
    });
    addDuplicateConditionErrors(form.groupMappings, 'groupMappings', errors);

    form.roleMappings.forEach((mapping, index) => {
        if (!mapping.condition.trim()) errors[`roleMappings.${index}.condition`] = 'Condition is required.';
        if (mapping.organizations.length === 0)
            errors[`roleMappings.${index}.organizations`] = 'At least one organization role is required.';
    });
    addDuplicateConditionErrors(form.roleMappings, 'roleMappings', errors);

    return errors;
}

function addDuplicateConditionErrors(
    mappings: readonly { condition: string }[],
    keyPrefix: 'groupMappings' | 'roleMappings',
    errors: Record<string, string>,
) {
    const indexesByCondition = new Map<string, number[]>();
    mappings.forEach((mapping, index) => {
        const condition = mapping.condition.trim();
        if (!condition) {
            return;
        }
        const indexes = indexesByCondition.get(condition) ?? [];
        indexes.push(index);
        indexesByCondition.set(condition, indexes);
    });
    for (const indexes of indexesByCondition.values()) {
        if (indexes.length < 2) {
            continue;
        }
        for (const index of indexes) {
            errors[`${keyPrefix}.${index}.condition`] = 'Condition must be unique.';
        }
    }
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
    const clientSecret = form.configuration.clientSecret.trim();
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
        payload.userProfileMapping = compactUserProfileMapping(form);
    }
    return payload;
}

function compactUserProfileMapping(form: IdentityProviderFormState): IdentityProviderUserProfileMapping {
    return {
        id: form.userProfileMapping.id.trim(),
        firstname: optionalTrimmed(form.userProfileMapping.firstname),
        lastname: optionalTrimmed(form.userProfileMapping.lastname),
        email: optionalTrimmed(form.userProfileMapping.email),
        picture: optionalTrimmed(form.userProfileMapping.picture),
    };
}

function compactGroupMappings(form: IdentityProviderFormState): GroupMapping[] {
    return form.groupMappings.map(mapping => ({
        condition: mapping.condition.trim(),
        groups: [...mapping.groups],
    }));
}

function compactRoleMappings(form: IdentityProviderFormState): RoleMapping[] {
    return form.roleMappings.map(mapping => ({
        condition: mapping.condition.trim(),
        organizations: [...mapping.organizations],
        environments: Object.fromEntries(Object.entries(mapping.environments).map(([environmentId, roles]) => [environmentId, [...roles]])),
    }));
}

export function formToUpdatePayload(form: IdentityProviderFormState): UpdateIdentityProviderPayload {
    const created = formToCreatePayload(form);
    const payload: UpdateIdentityProviderPayload = {
        name: created.name,
        description: created.description,
        enabled: created.enabled,
        emailRequired: created.emailRequired,
        syncMappings: created.syncMappings,
        configuration: created.configuration,
        groupMappings: compactGroupMappings(form),
        roleMappings: compactRoleMappings(form),
    };
    const userProfileMapping = userProfileMappingForUpdate(form);
    if (userProfileMapping) {
        payload.userProfileMapping = userProfileMapping;
    }
    return payload;
}

function sortedIds(ids: readonly string[]): string[] {
    return [...ids].sort((left, right) => left.localeCompare(right));
}

function normalizeMappingsForCompare(payload: UpdateIdentityProviderPayload): UpdateIdentityProviderPayload {
    return {
        ...payload,
        groupMappings: payload.groupMappings.map(mapping => ({
            condition: mapping.condition,
            groups: sortedIds(mapping.groups),
        })),
        roleMappings: payload.roleMappings.map(mapping => ({
            condition: mapping.condition,
            organizations: sortedIds(mapping.organizations),
            environments: Object.fromEntries(
                Object.entries(mapping.environments)
                    .sort(([left], [right]) => left.localeCompare(right))
                    .map(([environmentId, roles]) => [environmentId, sortedIds(roles)]),
            ),
        })),
    };
}

export function isIdentityProviderFormDirty(current: IdentityProviderFormState, initial: IdentityProviderFormState): boolean {
    return (
        JSON.stringify(normalizeMappingsForCompare(formToUpdatePayload(current))) !==
        JSON.stringify(normalizeMappingsForCompare(formToUpdatePayload(initial)))
    );
}

function userProfileMappingForUpdate(form: IdentityProviderFormState): IdentityProviderUserProfileMapping | undefined {
    if (hasUserProfileMapping(form.type)) {
        return compactUserProfileMapping(form);
    }
    const compacted = compactUserProfileMapping(form);
    if (!compacted.id && !compacted.firstname && !compacted.lastname && !compacted.email && !compacted.picture) {
        return undefined;
    }
    return compacted;
}

export function identityProviderToForm(provider: IdentityProvider, environmentIds: readonly string[] = []): IdentityProviderFormState {
    const defaults = emptyConfiguration(provider.type);
    const mappingEnvIds = [
        ...new Set([...environmentIds, ...provider.roleMappings.flatMap(mapping => Object.keys(mapping.environments ?? {}))]),
    ];
    return {
        type: provider.type,
        name: provider.name,
        description: provider.description ?? '',
        enabled: provider.enabled,
        emailRequired: provider.emailRequired ?? true,
        syncMappings: provider.syncMappings ?? false,
        configuration: {
            ...defaults,
            ...provider.configuration,
            clientId: provider.configuration?.clientId ?? '',
            clientSecret: provider.configuration?.clientSecret ?? '',
            scopes: provider.configuration?.scopes ?? defaults.scopes,
            color: provider.configuration?.color ?? '',
        },
        userProfileMapping: {
            id: provider.userProfileMapping?.id ?? (hasUserProfileMapping(provider.type) ? DEFAULT_USER_PROFILE.id : ''),
            firstname: provider.userProfileMapping?.firstname ?? '',
            lastname: provider.userProfileMapping?.lastname ?? '',
            email: provider.userProfileMapping?.email ?? '',
            picture: provider.userProfileMapping?.picture ?? '',
        },
        groupMappings: provider.groupMappings.map(mapping => ({
            condition: mapping.condition ?? '',
            groups: [...(mapping.groups ?? [])],
        })),
        roleMappings: provider.roleMappings.map(mapping => ({
            condition: mapping.condition ?? '',
            organizations: [...(mapping.organizations ?? [])],
            environments: Object.fromEntries(
                mappingEnvIds.map(environmentId => [environmentId, [...(mapping.environments?.[environmentId] ?? [])]]),
            ),
        })),
    };
}
