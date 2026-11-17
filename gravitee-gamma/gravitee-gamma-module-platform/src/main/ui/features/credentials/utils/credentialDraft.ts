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
import { CREDENTIAL_STRATEGY_DEFINITIONS } from './credentialStrategies';
import type {
    CreateCredentialPayload,
    Credential,
    CredentialSettings,
    CredentialStrategy,
    UpdateCredentialPayload,
} from '../types/credential';

export type CredentialSecretMode = 'value' | 'reference';

export function isCredentialSecretMode(value: string): value is CredentialSecretMode {
    return value === 'value' || value === 'reference';
}

export interface CredentialDraft {
    readonly name: string;
    readonly serviceId: string;
    readonly strategy: CredentialStrategy;
    readonly expiresAt: string;
    readonly mode: CredentialSecretMode;
    readonly settings: Readonly<Record<string, string>>;
    readonly secrets: Readonly<Record<string, string>>;
}

export type CredentialDraftErrors = Partial<Record<string, string>>;

const MAX_NAME_LENGTH = 256;
const MAX_SETTING_LENGTH = 2048;
const MAX_SCOPES = 50;
const HEADER_NAME = /^[!#$%&'*+.^_`|~0-9A-Za-z-]{1,256}$/;

export function emptyCredentialDraft(strategy: CredentialStrategy = 'BEARER'): CredentialDraft {
    return { name: '', serviceId: '', strategy, expiresAt: '', mode: 'value', settings: {}, secrets: {} };
}

export function draftFromCredential(credential: Credential): CredentialDraft {
    const definition = CREDENTIAL_STRATEGY_DEFINITIONS[credential.strategy];
    const settings: Record<string, string> = {};
    for (const field of definition.settings) {
        const value = credential.configuration?.[field.key];
        if (value !== undefined) {
            settings[field.key] = Array.isArray(value) ? value.join(', ') : value;
        }
    }
    const managed = credential.managed !== false;
    const secrets: Record<string, string> = {};
    if (!managed) {
        for (const [field, secretReference] of Object.entries(credential.secretReferences ?? {})) {
            secrets[field] = secretReference.reference;
        }
    }
    return {
        name: credential.name,
        serviceId: credential.serviceId ?? '',
        strategy: credential.strategy,
        expiresAt: isoToDateInput(credential.expiresAt),
        mode: managed ? 'value' : 'reference',
        settings,
        secrets,
    };
}

export function validateCredentialDraft(
    draft: CredentialDraft,
    options: { readonly secretsRequired: boolean; readonly now: Date; readonly currentExpiresAt?: string },
): CredentialDraftErrors {
    const errors: CredentialDraftErrors = {};
    const definition = CREDENTIAL_STRATEGY_DEFINITIONS[draft.strategy];

    const name = draft.name.trim();
    if (!name) {
        errors.name = 'Name is required.';
    } else if (name.length > MAX_NAME_LENGTH) {
        errors.name = `Name must be at most ${MAX_NAME_LENGTH} characters.`;
    }

    for (const field of definition.settings) {
        const value = (draft.settings[field.key] ?? '').trim();
        if (!value) {
            if (!field.optional) {
                errors[`setting.${field.key}`] = `${field.label} is required.`;
            }
        } else if (field.key === 'scopes') {
            const scopes = listItems(value);
            if (scopes.length > MAX_SCOPES) {
                errors[`setting.${field.key}`] = `Scopes must hold at most ${MAX_SCOPES} entries.`;
            } else if (scopes.some(scope => scope.length > MAX_SETTING_LENGTH)) {
                errors[`setting.${field.key}`] = `Each scope must be at most ${MAX_SETTING_LENGTH} characters.`;
            }
        } else if (value.length > MAX_SETTING_LENGTH) {
            errors[`setting.${field.key}`] = `${field.label} must be at most ${MAX_SETTING_LENGTH} characters.`;
        } else if (field.key === 'headerName' && !HEADER_NAME.test(value)) {
            errors[`setting.${field.key}`] = 'Header name must be a valid HTTP header name.';
        } else if (field.key === 'tokenUrl') {
            const tokenUrlError = tokenUrlProblem(value);
            if (tokenUrlError) {
                errors[`setting.${field.key}`] = tokenUrlError;
            }
        }
    }

    if (options.secretsRequired) {
        for (const field of definition.secrets) {
            if (!(draft.secrets[field.key] ?? '').trim()) {
                errors[`secret.${field.key}`] = `${field.label} is required.`;
            }
        }
    }

    const expiryChanged = draft.expiresAt !== isoToDateInput(options.currentExpiresAt);
    if (draft.expiresAt && expiryChanged && draft.expiresAt < toDateInput(options.now)) {
        errors.expiresAt = 'The expiry date must be today or later.';
    }

    return errors;
}

export function withoutEditedFieldErrors(
    errors: CredentialDraftErrors,
    previous: CredentialDraft,
    next: CredentialDraft,
): CredentialDraftErrors {
    const fieldValue = (draft: CredentialDraft, key: string): string | undefined => {
        const [group, field] = key.split('.', 2);
        if (group === 'setting' && field) {
            return draft.settings[field];
        }
        if (group === 'secret' && field) {
            return draft.secrets[field];
        }
        return draft[key as 'name' | 'serviceId' | 'expiresAt'];
    };
    return Object.fromEntries(Object.entries(errors).filter(([key]) => fieldValue(previous, key) === fieldValue(next, key)));
}

export function toCreateCredentialPayload(draft: CredentialDraft): CreateCredentialPayload {
    return {
        name: draft.name.trim(),
        strategy: draft.strategy,
        ...optionalText('serviceId', draft.serviceId),
        ...(draft.expiresAt ? { expiresAt: endOfDayIso(draft.expiresAt) } : {}),
        configuration: toConfiguration(draft),
        ...(draft.mode === 'reference' ? { references: filledReferences(draft) } : { values: filledValues(draft) }),
    };
}

export function toUpdateCredentialPayload(draft: CredentialDraft, credential: Credential): UpdateCredentialPayload {
    return {
        name: draft.name.trim(),
        ...optionalText('serviceId', draft.serviceId),
        ...updatedExpiry(draft, credential),
        configuration: toConfiguration(draft),
        ...(draft.mode === 'reference' ? { references: filledReferences(draft) } : {}),
    };
}

function updatedExpiry(draft: CredentialDraft, credential: Credential): { expiresAt?: string } {
    if (draft.expiresAt === isoToDateInput(credential.expiresAt)) {
        return optionalText('expiresAt', credential.expiresAt ?? '');
    }
    return draft.expiresAt ? { expiresAt: endOfDayIso(draft.expiresAt) } : {};
}

export function rotationValues(draft: CredentialDraft): Record<string, string> | undefined {
    if (draft.mode !== 'value') {
        return undefined;
    }
    const values = filledValues(draft);
    return Object.keys(values).length > 0 ? values : undefined;
}

function toConfiguration(draft: CredentialDraft): CredentialSettings {
    const configuration: CredentialSettings = {};
    for (const field of CREDENTIAL_STRATEGY_DEFINITIONS[draft.strategy].settings) {
        const value = (draft.settings[field.key] ?? '').trim();
        if (!value) {
            continue;
        }
        configuration[field.key] = field.list ? listItems(value) : value;
    }
    return configuration;
}

function filledReferences(draft: CredentialDraft): Record<string, string> {
    const references: Record<string, string> = {};
    for (const field of CREDENTIAL_STRATEGY_DEFINITIONS[draft.strategy].secrets) {
        const reference = (draft.secrets[field.key] ?? '').trim();
        if (reference) {
            references[field.key] = reference;
        }
    }
    return references;
}

function filledValues(draft: CredentialDraft): Record<string, string> {
    const values: Record<string, string> = {};
    for (const field of CREDENTIAL_STRATEGY_DEFINITIONS[draft.strategy].secrets) {
        const valueWithItsSpaces = draft.secrets[field.key] ?? '';
        if (valueWithItsSpaces.trim()) {
            values[field.key] = valueWithItsSpaces;
        }
    }
    return values;
}

function optionalText<K extends string>(key: K, value: string): Partial<Record<K, string>> {
    const trimmed = value.trim();
    const text: Partial<Record<K, string>> = {};
    if (trimmed) {
        text[key] = trimmed;
    }
    return text;
}

function listItems(value: string): string[] {
    return value
        .split(',')
        .map(item => item.trim())
        .filter(Boolean);
}

function tokenUrlProblem(value: string): string | undefined {
    let url: URL;
    try {
        url = new URL(value);
    } catch {
        return 'Token URL must be an https URL.';
    }
    if (url.protocol !== 'https:' || !url.hostname) {
        return 'Token URL must be an https URL.';
    }
    if (url.username || url.password) {
        return 'Token URL must not carry a user name or password. Put the client secret in its own field.';
    }
    return undefined;
}

export function toDateInput(date: Date): string {
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${date.getFullYear()}-${month}-${day}`;
}

function isoToDateInput(isoDate: string | undefined): string {
    return isoDate ? toDateInput(new Date(isoDate)) : '';
}

function endOfDayIso(date: string): string {
    return new Date(`${date}T23:59:59.999`).toISOString();
}
