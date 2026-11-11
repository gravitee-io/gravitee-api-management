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
    DatePicker,
    Field,
    FieldDescription,
    FieldError,
    FieldLabel,
    Input,
    PasswordInput,
    ToggleGroup,
    ToggleGroupItem,
} from '@gravitee/graphene-core';

import type { CredentialDraft, CredentialDraftErrors, CredentialSecretMode } from '../utils/credentialDraft';
import { CREDENTIAL_STRATEGY_DEFINITIONS, type CredentialField } from '../utils/credentialStrategies';

interface CredentialAuthenticationFieldsProps {
    readonly idPrefix: string;
    readonly draft: CredentialDraft;
    readonly errors: CredentialDraftErrors;
    readonly onChange: (draft: CredentialDraft) => void;
    /** Shows the choice between a stored value and a reference. Only a new credential can choose. */
    readonly showModeChoice?: boolean;
    readonly canStoreValues?: boolean;
    /** A managed credential being edited: blank values keep the stored ones. */
    readonly valuesOptional?: boolean;
    /** Why the secret inputs are read only, when they are. */
    readonly secretsDisabledReason?: string;
}

export function CredentialAuthenticationFields({
    idPrefix,
    draft,
    errors,
    onChange,
    showModeChoice = false,
    canStoreValues = true,
    valuesOptional = false,
    secretsDisabledReason,
}: CredentialAuthenticationFieldsProps) {
    const definition = CREDENTIAL_STRATEGY_DEFINITIONS[draft.strategy];
    const modeLabelId = `${idPrefix}-mode-label`;

    function setSetting(key: string, value: string) {
        onChange({ ...draft, settings: { ...draft.settings, [key]: value } });
    }

    function setSecret(key: string, value: string) {
        onChange({ ...draft, secrets: { ...draft.secrets, [key]: value } });
    }

    function setMode(mode: CredentialSecretMode) {
        onChange({ ...draft, mode, secrets: {} });
    }

    return (
        <div className="space-y-4">
            {definition.settings.map(field => (
                <TextField
                    key={field.key}
                    id={`${idPrefix}-setting-${field.key}`}
                    field={field}
                    value={draft.settings[field.key] ?? ''}
                    error={errors[`setting.${field.key}`]}
                    onChange={value => setSetting(field.key, value)}
                />
            ))}

            {showModeChoice ? (
                <Field orientation="vertical" className="gap-1.5">
                    <FieldLabel id={modeLabelId}>Secret source</FieldLabel>
                    <ToggleGroup
                        type="single"
                        value={draft.mode}
                        onValueChange={value => value && setMode(value as CredentialSecretMode)}
                        aria-labelledby={modeLabelId}
                    >
                        <ToggleGroupItem value="value" disabled={!canStoreValues}>
                            Store a value
                        </ToggleGroupItem>
                        <ToggleGroupItem value="reference">Reference a secret</ToggleGroupItem>
                    </ToggleGroup>
                    <FieldDescription>
                        {canStoreValues
                            ? 'Store a value: Gravitee keeps it as a secret. Reference a secret: point at a secret that already exists, in Gravitee or in a vault.'
                            : 'Storing a value needs the permission to create secrets. Reference a secret that already exists instead.'}
                    </FieldDescription>
                </Field>
            ) : null}

            {definition.secrets.map(field => {
                const id = `${idPrefix}-secret-${field.key}`;
                const error = errors[`secret.${field.key}`];
                const value = draft.secrets[field.key] ?? '';
                const disabled = Boolean(secretsDisabledReason);
                return (
                    <Field key={field.key} orientation="vertical" className="gap-1.5">
                        <FieldLabel htmlFor={id} required={!valuesOptional}>
                            {field.label}
                        </FieldLabel>
                        {draft.mode === 'value' ? (
                            <PasswordInput
                                id={id}
                                value={value}
                                autoComplete="new-password"
                                placeholder={valuesOptional ? 'Leave empty to keep the stored value' : field.placeholder}
                                disabled={disabled}
                                aria-invalid={Boolean(error)}
                                onChange={e => setSecret(field.key, e.target.value)}
                            />
                        ) : (
                            <Input
                                id={id}
                                value={value}
                                placeholder="/vault/secret/data/my-service:key"
                                disabled={disabled}
                                aria-invalid={Boolean(error)}
                                onChange={e => setSecret(field.key, e.target.value)}
                            />
                        )}
                        {error ? <FieldError>{error}</FieldError> : null}
                    </Field>
                );
            })}
            {secretsDisabledReason ? <p className="text-sm text-muted-foreground">{secretsDisabledReason}</p> : null}
        </div>
    );
}

export function CredentialExpiryField({
    id,
    value,
    error,
    onChange,
}: Readonly<{ id: string; value: string; error?: string; onChange: (value: string) => void }>) {
    return (
        <Field orientation="vertical" className="gap-1.5">
            <FieldLabel htmlFor={id}>Expires (optional)</FieldLabel>
            <DatePicker
                id={id}
                aria-label="Expires"
                placeholder="Never expires"
                value={value ? new Date(`${value}T00:00:00`) : undefined}
                onChange={date => onChange(date ? toDateInput(date) : '')}
            />
            <FieldDescription>A reminder to replace the credential. It does not stop the credential from being used.</FieldDescription>
            {error ? <FieldError>{error}</FieldError> : null}
        </Field>
    );
}

function TextField({
    id,
    field,
    value,
    error,
    onChange,
}: Readonly<{ id: string; field: CredentialField; value: string; error?: string; onChange: (value: string) => void }>) {
    return (
        <Field orientation="vertical" className="gap-1.5">
            <FieldLabel htmlFor={id} required={!field.optional}>
                {field.label}
            </FieldLabel>
            <Input
                id={id}
                value={value}
                placeholder={field.placeholder}
                aria-invalid={Boolean(error)}
                onChange={e => onChange(e.target.value)}
            />
            {field.list ? <FieldDescription>Separate several values with commas.</FieldDescription> : null}
            {error ? <FieldError>{error}</FieldError> : null}
        </Field>
    );
}

function toDateInput(date: Date): string {
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${date.getFullYear()}-${month}-${day}`;
}
