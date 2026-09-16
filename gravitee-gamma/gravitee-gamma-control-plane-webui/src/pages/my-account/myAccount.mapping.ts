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
import type { ConsoleAuthenticationSettings, NamedEnvironment, ProfileDraft, UpdateCurrentUserPayload } from './myAccount.types';

export const TOKEN_NAME_MAX_LENGTH = 64;
export const TOKEN_NAME_MIN_LENGTH = 2;

export function isInternalUser(source: string | undefined): boolean {
    return source === 'gravitee' || source === 'memory';
}

export function displayDangerZone(auth: ConsoleAuthenticationSettings | undefined): boolean {
    if (!auth) {
        return false;
    }
    return !auth.externalAuth?.enabled || Boolean(auth.externalAuthAccountDeletion?.enabled);
}

export function pictureFieldForUpdate(draft: {
    readonly pictureDataUrl: string | null;
    readonly resetToDefault: boolean;
    readonly hadPictureOnLoad: boolean;
}): string | undefined {
    if (draft.pictureDataUrl) {
        return draft.pictureDataUrl;
    }
    if (draft.resetToDefault && draft.hadPictureOnLoad) {
        return '';
    }
    return undefined;
}

export function toUpdateUserPayload(draft: ProfileDraft): UpdateCurrentUserPayload {
    const picture = pictureFieldForUpdate(draft);
    return {
        firstname: draft.firstname,
        lastname: draft.lastname,
        email: draft.email,
        customFields: { ...draft.customFields },
        ...(picture !== undefined ? { picture } : {}),
    };
}

export function hasMissingRequiredCustomFields(
    customFields: Readonly<Record<string, string>>,
    fieldDefs: ReadonlyArray<{ readonly key: string; readonly required: boolean }>,
): boolean {
    return fieldDefs.some(field => field.required && !(customFields[field.key] ?? '').trim());
}

export function managementApiRoot(managementBaseURL: string): string {
    return managementBaseURL.endsWith('/') ? managementBaseURL.slice(0, -1) : managementBaseURL;
}

export function currentUserAvatarUrl(managementBaseURL: string, organizationId: string, userId: string, cacheBust: number): string {
    return `${managementApiRoot(managementBaseURL)}/organizations/${organizationId}/user/avatar?${encodeURIComponent(userId)}&cacheBust=${cacheBust}`;
}

export function formatRoles(roles: ReadonlyArray<{ readonly scope?: string; readonly name?: string }> | undefined): string {
    if (!roles?.length) {
        return '—';
    }
    return roles.map(role => `[${role.scope ?? ''}] ${role.name ?? ''}`).join(' - ');
}

export function formatGroupsByEnvironment(
    groupsByEnvironment: Readonly<Record<string, readonly string[]>> | undefined,
    environments: readonly NamedEnvironment[],
): string {
    if (!groupsByEnvironment) {
        return '—';
    }
    const occupied = Object.keys(groupsByEnvironment).filter(id => (groupsByEnvironment[id]?.length ?? 0) > 0);
    if (occupied.length === 0) {
        return '—';
    }
    if (occupied.length === 1) {
        return (groupsByEnvironment[occupied[0]!] ?? []).join(' - ');
    }
    const seen = new Set<string>();
    const ordered: string[] = [];
    for (const env of environments) {
        if ((groupsByEnvironment[env.id]?.length ?? 0) > 0) {
            ordered.push(env.id);
            seen.add(env.id);
        }
    }
    for (const id of occupied) {
        if (!seen.has(id)) {
            ordered.push(id);
        }
    }
    const nameById = Object.fromEntries(environments.map(env => [env.id, env.name ?? env.id]));
    return ordered.map(id => `[${nameById[id] ?? id}] ${(groupsByEnvironment[id] ?? []).join('/')}`).join(' - ');
}

export function validateTokenName(name: string): string | null {
    const trimmed = name.trim();
    if (!trimmed) {
        return 'Name is required.';
    }
    if (trimmed.length < TOKEN_NAME_MIN_LENGTH) {
        return `Name has to be at least ${TOKEN_NAME_MIN_LENGTH} characters long.`;
    }
    if (trimmed.length > TOKEN_NAME_MAX_LENGTH) {
        return `Name has to be at most ${TOKEN_NAME_MAX_LENGTH} characters long.`;
    }
    return null;
}

export function isDuplicateTokenError(technicalCode: string | undefined, message: string): boolean {
    if (technicalCode?.toLowerCase() === 'token.alreadyexists') {
        return true;
    }
    return message.toLowerCase().includes('a token with the name');
}

export function buildTokenUsageExample(token: string, managementBaseURL: string, organizationId: string, environmentId: string): string {
    return `curl -H "Authorization: Bearer ${token}" "${managementApiRoot(managementBaseURL)}/organizations/${organizationId}/environments/${environmentId}"`;
}

export function authenticationFromConsole(consoleJson: unknown): ConsoleAuthenticationSettings | undefined {
    if (!consoleJson || typeof consoleJson !== 'object') {
        return undefined;
    }
    const { authentication } = consoleJson as { authentication?: ConsoleAuthenticationSettings };
    return authentication;
}

export function customFieldsAsStrings(fields: Readonly<Record<string, unknown>> | undefined): Record<string, string> {
    if (!fields) {
        return {};
    }
    return Object.fromEntries(
        Object.entries(fields).map(([key, value]) => [key, value === null || value === undefined ? '' : String(value)]),
    );
}

export function formatTokenTimestamp(timestamp: number | undefined): string {
    if (!timestamp) {
        return 'never';
    }
    return new Intl.DateTimeFormat(undefined, { dateStyle: 'medium', timeStyle: 'medium' }).format(new Date(timestamp));
}
