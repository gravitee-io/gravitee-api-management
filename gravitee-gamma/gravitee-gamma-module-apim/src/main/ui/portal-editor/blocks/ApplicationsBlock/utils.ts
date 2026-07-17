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
import type { Application, ApplicationType, ApplicationTypeConfig } from '../../editor/entities/application';

export function formatApplicationType(type?: ApplicationType): string {
    switch (type) {
        case 'SIMPLE':
            return 'Simple';
        case 'SPA':
            return 'SPA';
        case 'WEB':
            return 'Web';
        case 'NATIVE':
            return 'Native';
        case 'BACKEND_TO_BACKEND':
            return 'Backend to backend';
        default:
            return type ?? 'Unknown';
    }
}

export function formatDate(value?: string): string {
    if (!value) return '—';
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return value;
    return date.toLocaleDateString(undefined, { year: 'numeric', month: 'short', day: 'numeric' });
}

export function formatGrantType(type: string, typeConfig?: ApplicationTypeConfig): string {
    const grant = typeConfig?.allowed_grant_types.find(item => item.type === type);
    return grant?.name ?? type.replace(/_/g, ' ');
}

export function isOAuthApplication(application: Application): boolean {
    return !!application.settings.oauth;
}

export function generateClientId(): string {
    return `client-${crypto.randomUUID().replace(/-/g, '').slice(0, 16)}`;
}

export function generateClientSecret(): string {
    return crypto.randomUUID().replace(/-/g, '') + crypto.randomUUID().replace(/-/g, '').slice(0, 8);
}

export function buildApplicationFromForm(
    form: ApplicationFormState,
    typeConfig: ApplicationTypeConfig,
    existing?: Application,
): Application {
    const now = new Date().toISOString();
    const base: Application = {
        id: existing?.id ?? crypto.randomUUID(),
        name: form.name.trim(),
        description: form.description.trim() || undefined,
        domain: form.domain.trim() || undefined,
        applicationType: typeConfig.applicationType,
        api_key_mode: existing?.api_key_mode ?? 'EXCLUSIVE',
        created_at: existing?.created_at ?? now,
        updated_at: now,
        owner: existing?.owner ?? {
            id: 'current-user',
            display_name: 'Current User',
            email: 'user@example.com',
        },
        settings: {},
    };

    if (typeConfig.applicationType === 'SIMPLE') {
        base.settings = {
            app: {
                type: form.appType.trim() || undefined,
                client_id: form.clientId.trim() || undefined,
            },
        };
        return base;
    }

    const metadata = Object.fromEntries(
        form.metadataEntries.filter(entry => entry.key.trim()).map(entry => [entry.key.trim(), entry.value.trim()]),
    );

    base.settings = {
        oauth: {
            client_id: existing?.settings.oauth?.client_id ?? (form.clientId.trim() || generateClientId()),
            client_secret: existing?.settings.oauth?.client_secret ?? generateClientSecret(),
            redirect_uris: form.redirectUris,
            grant_types: form.grantTypes,
            application_type: typeConfig.id,
            ...(Object.keys(metadata).length > 0 ? { additional_client_metadata: metadata } : {}),
        },
    };

    return base;
}

export interface MetadataEntry {
    key: string;
    value: string;
}

export interface ApplicationFormState {
    name: string;
    description: string;
    domain: string;
    appType: string;
    clientId: string;
    redirectUris: string[];
    grantTypes: string[];
    metadataEntries: MetadataEntry[];
}

export function applicationToFormState(application: Application, typeConfig: ApplicationTypeConfig): ApplicationFormState {
    if (application.settings.app) {
        return {
            name: application.name,
            description: application.description ?? '',
            domain: application.domain ?? '',
            appType: application.settings.app.type ?? '',
            clientId: application.settings.app.client_id ?? '',
            redirectUris: [],
            grantTypes: [],
            metadataEntries: [],
        };
    }

    const oauth = application.settings.oauth;
    const metadata = oauth?.additional_client_metadata ?? {};

    return {
        name: application.name,
        description: application.description ?? '',
        domain: application.domain ?? '',
        appType: '',
        clientId: oauth?.client_id ?? '',
        redirectUris: oauth?.redirect_uris ?? [],
        grantTypes: oauth?.grant_types ?? typeConfig.mandatory_grant_types.map(grant => grant.type),
        metadataEntries: Object.entries(metadata).map(([key, value]) => ({ key, value })),
    };
}

export function createEmptyFormState(): ApplicationFormState {
    return {
        name: '',
        description: '',
        domain: '',
        appType: '',
        clientId: '',
        redirectUris: [],
        grantTypes: [],
        metadataEntries: [],
    };
}

export function isFormValid(form: ApplicationFormState, typeConfig: ApplicationTypeConfig | undefined): boolean {
    if (!typeConfig || !form.name.trim()) return false;

    if (typeConfig.applicationType === 'SIMPLE') {
        return true;
    }

    if (typeConfig.requires_redirect_uris && form.redirectUris.length === 0) {
        return false;
    }

    const mandatory = typeConfig.mandatory_grant_types.map(grant => grant.type);
    return mandatory.every(type => form.grantTypes.includes(type));
}
