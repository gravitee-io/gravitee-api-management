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
import type { ApplicationListItem, ApplicationOwner } from '../types/application';

const TYPE_LABELS: Record<string, string> = {
    SIMPLE: 'Service',
    BROWSER: 'SPA',
    WEB: 'Web',
    NATIVE: 'Mobile',
    BACKEND_TO_BACKEND: 'Backend',
    AI_AGENT: 'AI Agent',
};

/** Security type labels for application detail (matches applications/types.json names). */
const SECURITY_TYPE_LABELS: Record<string, string> = {
    SIMPLE: 'Simple',
    BROWSER: 'SPA',
    WEB: 'Web',
    NATIVE: 'Native',
    BACKEND_TO_BACKEND: 'Backend to backend',
    AI_AGENT: 'AI Agent',
};

const OWNER_TYPE_LABELS: Record<string, string> = {
    USER: 'User',
    GROUP: 'Group',
};

export function formatApplicationSecurityTypeLabel(application: Pick<ApplicationListItem, 'type' | 'settings'>): string {
    const settingsType = application.settings?.app?.type;
    if (settingsType) {
        const normalized = settingsType.toUpperCase();
        return SECURITY_TYPE_LABELS[normalized] ?? toTitleLabel(settingsType);
    }
    if (!application.type) {
        return '—';
    }
    const normalized = application.type.toUpperCase();
    return SECURITY_TYPE_LABELS[normalized] ?? toTitleLabel(application.type);
}

export function formatApplicationOwnerLabel(owner: ApplicationOwner | undefined): string | null {
    if (!owner?.displayName) {
        return null;
    }
    const roleLabel = owner.type ? (OWNER_TYPE_LABELS[owner.type.toUpperCase()] ?? toTitleLabel(owner.type)) : null;
    return roleLabel ? `${owner.displayName} (${roleLabel})` : owner.displayName;
}

export function formatApplicationTypeLabel(application: Pick<ApplicationListItem, 'type' | 'settings'>): string {
    const settingsType = application.settings?.app?.type;
    if (settingsType) {
        return TYPE_LABELS[settingsType] ?? toTitleLabel(settingsType);
    }
    if (!application.type) {
        return '—';
    }
    return TYPE_LABELS[application.type] ?? toTitleLabel(application.type);
}

const API_KEY_MODE_LABELS: Record<string, string> = {
    UNSPECIFIED: 'Unspecified',
    SHARED: 'Shared',
    EXCLUSIVE: 'Exclusive',
};

export function formatApplicationApiKeyMode(mode: string | undefined): string {
    if (!mode) return '—';
    return API_KEY_MODE_LABELS[mode] ?? mode;
}

export function formatApplicationDateTime(value: number | string | undefined): string {
    if (value === undefined || value === '') return '—';
    const date = typeof value === 'number' ? new Date(value) : new Date(value);
    if (Number.isNaN(date.getTime())) return '—';
    return date.toLocaleString('en-US', {
        year: 'numeric',
        month: 'short',
        day: 'numeric',
        hour: 'numeric',
        minute: '2-digit',
    });
}

function toTitleLabel(value: string): string {
    return value
        .toLowerCase()
        .split('_')
        .map(part => part.charAt(0).toUpperCase() + part.slice(1))
        .join(' ');
}
