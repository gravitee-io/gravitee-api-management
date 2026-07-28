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
/** Strips HTML tags from user-provided name fields before submit. */
export function sanitizeTextInput(value: string): string {
    return value.replace(/<[^>]*>/g, '').trim();
}

const EMAIL_REGEX =
    /^(([^<>()[\]\\.,;:\s@"]+(\.[^<>()[\]\\.,;:\s@"]+)*)|(".+"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,}))$/;

export function isValidEmail(value: string): boolean {
    return EMAIL_REGEX.test(value.trim());
}

export function formatRoleDisplayName(name: string): string {
    if (name === name.toUpperCase() && !name.includes('_')) {
        return name.charAt(0) + name.slice(1).toLowerCase();
    }
    return name;
}

export function formatRoleSummary(roles: { name?: string; scope?: string }[] | undefined): string {
    if (!roles?.length) {
        return '—';
    }
    const names = roles
        .map(role => role.name)
        .filter((name): name is string => Boolean(name))
        .map(formatRoleDisplayName);
    return names.length > 0 ? names.join(', ') : '—';
}

/** First N role names for compact display; full list is intended for tooltip. */
export function formatTruncatedRoleSummary(
    roles: { name?: string; scope?: string }[] | undefined,
    visibleCount = 3,
): { display: string; full: string; truncated: boolean } {
    if (!roles?.length) {
        return { display: '—', full: '—', truncated: false };
    }
    const names = roles
        .map(role => role.name)
        .filter((name): name is string => Boolean(name))
        .map(formatRoleDisplayName);
    if (names.length === 0) {
        return { display: '—', full: '—', truncated: false };
    }
    const full = names.join(', ');
    if (names.length <= visibleCount) {
        return { display: full, full, truncated: false };
    }
    return {
        display: `${names.slice(0, visibleCount).join(', ')}...`,
        full,
        truncated: true,
    };
}

export function formatUserStatus(status: string | undefined): string {
    if (!status) {
        return 'Unknown';
    }
    const normalized = status.toUpperCase();
    if (normalized === 'ARCHIVED') {
        return 'Deletion In Progress';
    }
    return normalized.charAt(0) + normalized.slice(1).toLowerCase();
}

export type StatusBadgeVariant = 'success' | 'warning' | 'destructive' | 'secondary';

export function statusBadgeVariant(status: string | undefined): StatusBadgeVariant {
    switch (status?.toUpperCase()) {
        case 'ACTIVE':
            return 'success';
        case 'PENDING':
        case 'ARCHIVED':
            return 'warning';
        case 'REJECTED':
            return 'destructive';
        default:
            return 'secondary';
    }
}

const KNOWN_IDP_LABELS: Record<string, string> = {
    gravitee: 'Gravitee',
    memory: 'Memory',
    ldap: 'LDAP',
    oidc: 'OIDC',
    openid: 'OpenID Provider',
    'openid-provider': 'OpenID Provider',
};

export function formatSourceLabel(source: string | undefined): string {
    if (!source) {
        return '—';
    }
    const known = KNOWN_IDP_LABELS[source.toLowerCase()];
    if (known) {
        return known;
    }
    if (source.includes('-')) {
        return source
            .split('-')
            .map(part => part.charAt(0).toUpperCase() + part.slice(1).toLowerCase())
            .join(' ');
    }
    return source.charAt(0).toUpperCase() + source.slice(1);
}

export type SourceBadgeVariant = 'warning' | 'outline';

export function sourceBadgeVariant(source: string | undefined): SourceBadgeVariant {
    const normalized = source?.toLowerCase() ?? '';
    if (normalized === 'gravitee' || normalized === 'memory') {
        return 'outline';
    }
    return 'warning';
}

export function detailStatusBadgeVariant(status: string | undefined): StatusBadgeVariant {
    if (status?.toUpperCase() === 'PENDING') {
        return 'secondary';
    }
    return statusBadgeVariant(status);
}

export function isDuplicateUserError(message: string): boolean {
    const lower = message.toLowerCase();
    return lower.includes('user cannot be created') || lower.includes('already exists for organization');
}
