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

/** `value`: Gravitee stores what the user types. `reference`: the credential points at secrets that already exist. */
export type CredentialSecretMode = 'value' | 'reference';

export interface CredentialDraft {
    readonly name: string;
    readonly serviceId: string;
    readonly strategy: CredentialStrategy;
    /** `YYYY-MM-DD`, or empty for no expiry. */
    readonly expiresAt: string;
    readonly mode: CredentialSecretMode;
    readonly settings: Readonly<Record<string, string>>;
    /** A value or a reference per secret field, depending on `mode`. */
    readonly secrets: Readonly<Record<string, string>>;
}

export type CredentialDraftErrors = Partial<Record<string, string>>;

const MAX_NAME_LENGTH = 256;
// Same token rule as the backend `CredentialStrategy`, so a bad header name fails before the request.
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
        } else if (field.key === 'headerName' && !HEADER_NAME.test(value)) {
            errors[`setting.${field.key}`] = 'Header name must be a valid HTTP header name.';
        } else if (field.key === 'tokenUrl' && !isHttpsUrl(value)) {
            errors[`setting.${field.key}`] = 'Token URL must be an https URL.';
        } else if (field.key === 'scopes' && listItems(value).length > MAX_SCOPES) {
            errors[`setting.${field.key}`] = `Scopes must hold at most ${MAX_SCOPES} entries.`;
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

/** Drops the error of each field the edit changed, so a fixed field stops showing its error before the next submit. */
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
    const secrets = filledSecrets(draft, draft.mode === 'reference');
    return {
        name: draft.name.trim(),
        strategy: draft.strategy,
        ...optionalText('serviceId', draft.serviceId),
        ...(draft.expiresAt ? { expiresAt: fromDateInput(draft.expiresAt) } : {}),
        configuration: toConfiguration(draft),
        ...(draft.mode === 'reference' ? { references: secrets } : { values: secrets }),
    };
}

/** The PUT body. A managed credential leaves its references out to keep them; its values change through rotation only. */
export function toUpdateCredentialPayload(draft: CredentialDraft, credential: Credential): UpdateCredentialPayload {
    return {
        name: draft.name.trim(),
        ...optionalText('serviceId', draft.serviceId),
        ...updatedExpiry(draft, credential),
        configuration: toConfiguration(draft),
        ...(draft.mode === 'reference' ? { references: filledSecrets(draft, true) } : {}),
    };
}

/** Keeps the stored instant when the day is unchanged, so a save does not move an expiry set elsewhere. */
function updatedExpiry(draft: CredentialDraft, credential: Credential): { expiresAt?: string } {
    if (draft.expiresAt === isoToDateInput(credential.expiresAt)) {
        return optionalText('expiresAt', credential.expiresAt ?? '');
    }
    return draft.expiresAt ? { expiresAt: fromDateInput(draft.expiresAt) } : {};
}

/** The values typed into a managed credential, or `undefined` when there is nothing to rotate. */
export function rotationValues(draft: CredentialDraft): Record<string, string> | undefined {
    if (draft.mode !== 'value') {
        return undefined;
    }
    const values = filledSecrets(draft, false);
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

function filledSecrets(draft: CredentialDraft, trim: boolean): Record<string, string> {
    const secrets: Record<string, string> = {};
    for (const field of CREDENTIAL_STRATEGY_DEFINITIONS[draft.strategy].secrets) {
        const raw = draft.secrets[field.key] ?? '';
        // A typed value is sent as is: leading or trailing spaces can be part of a secret.
        const value = trim ? raw.trim() : raw;
        if (value.trim()) {
            secrets[field.key] = value;
        }
    }
    return secrets;
}

function optionalText<K extends string>(key: K, value: string): Partial<Record<K, string>> {
    const trimmed = value.trim();
    return trimmed ? ({ [key]: trimmed } as Record<K, string>) : {};
}

function listItems(value: string): string[] {
    return value
        .split(',')
        .map(item => item.trim())
        .filter(Boolean);
}

// The gateway sends the client secret to the token URL, so the server takes https only.
function isHttpsUrl(value: string): boolean {
    try {
        const url = new URL(value);
        return url.protocol === 'https:' && Boolean(url.hostname);
    } catch {
        return false;
    }
}

/** The local `YYYY-MM-DD` of a date, as the draft holds it. */
export function toDateInput(date: Date): string {
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${date.getFullYear()}-${month}-${day}`;
}

function isoToDateInput(isoDate: string | undefined): string {
    return isoDate ? toDateInput(new Date(isoDate)) : '';
}

// The credential stays valid through the chosen day; the backend refuses an expiry that is not in the future.
function fromDateInput(date: string): string {
    return new Date(`${date}T23:59:59.999`).toISOString();
}
