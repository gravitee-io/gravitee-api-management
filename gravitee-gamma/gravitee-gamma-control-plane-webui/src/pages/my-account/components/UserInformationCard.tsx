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
import {
    Button,
    Card,
    CardContent,
    Field,
    FieldError,
    FieldGroup,
    FieldLabel,
    Input,
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
} from '@gravitee/graphene-core';

import { AvatarPicker } from './AvatarPicker';
import type { CurrentUser } from '../../../features/auth/auth.types';
import type { CustomUserField } from '../../../features/auth/services/registration.service';
import { formatGroupsByEnvironment, formatRoles, hasMissingRequiredCustomFields } from '../myAccount.mapping';
import type { NamedEnvironment, ProfileDraft } from '../myAccount.types';

export function UserInformationCard({
    user,
    draft,
    fieldDefs,
    environments,
    avatarPreview,
    internal,
    dirty,
    saving,
    onDraftChange,
    onSave,
    onCancel,
}: Readonly<{
    user: CurrentUser;
    draft: ProfileDraft;
    fieldDefs: readonly CustomUserField[];
    environments: readonly NamedEnvironment[];
    avatarPreview?: string;
    internal: boolean;
    dirty: boolean;
    saving: boolean;
    onDraftChange: (draft: ProfileDraft) => void;
    onSave: () => void;
    onCancel: () => void;
}>) {
    const identityInvalid = internal && (!draft.firstname.trim() || !draft.lastname.trim() || !draft.email.trim());
    const formInvalid = identityInvalid || hasMissingRequiredCustomFields(draft.customFields, fieldDefs);

    return (
        <Card>
            <CardContent className="space-y-6 pt-6">
                <div>
                    <h2 className="text-lg font-semibold">User information</h2>
                    <p className="text-sm text-muted-foreground">{user.displayName}</p>
                </div>
                <form
                    className="space-y-6"
                    onSubmit={event => {
                        event.preventDefault();
                        if (!formInvalid) {
                            onSave();
                        }
                    }}
                >
                    <div className="flex flex-col gap-6 sm:flex-row sm:items-start">
                        <div className="min-w-0 flex-1 space-y-4">
                            <FieldGroup>
                                <div className="grid gap-4 sm:grid-cols-2">
                                    <Field orientation="vertical" className="gap-2">
                                        <FieldLabel htmlFor="my-account-firstname">First name</FieldLabel>
                                        <Input
                                            id="my-account-firstname"
                                            value={draft.firstname}
                                            required={internal}
                                            disabled={!internal}
                                            autoComplete="given-name"
                                            onChange={event => onDraftChange({ ...draft, firstname: event.target.value })}
                                        />
                                    </Field>
                                    <Field orientation="vertical" className="gap-2">
                                        <FieldLabel htmlFor="my-account-lastname">Last name</FieldLabel>
                                        <Input
                                            id="my-account-lastname"
                                            value={draft.lastname}
                                            required={internal}
                                            disabled={!internal}
                                            autoComplete="family-name"
                                            onChange={event => onDraftChange({ ...draft, lastname: event.target.value })}
                                        />
                                    </Field>
                                </div>
                                <Field orientation="vertical" className="gap-2">
                                    <FieldLabel htmlFor="my-account-email">Email</FieldLabel>
                                    <Input
                                        id="my-account-email"
                                        type="email"
                                        value={draft.email}
                                        required={internal}
                                        disabled={!internal}
                                        autoComplete="email"
                                        onChange={event => onDraftChange({ ...draft, email: event.target.value })}
                                    />
                                </Field>
                            </FieldGroup>
                            <div className="space-y-1">
                                <p className="text-sm text-muted-foreground">Roles</p>
                                <p className="text-sm font-medium">{formatRoles(user.roles)}</p>
                            </div>
                            <div className="space-y-1">
                                <p className="text-sm text-muted-foreground">Groups</p>
                                <p className="text-sm font-medium">{formatGroupsByEnvironment(user.groupsByEnvironment, environments)}</p>
                            </div>
                            {fieldDefs.length > 0 ? (
                                <FieldGroup>
                                    {fieldDefs.map(field => (
                                        <CustomFieldInput
                                            key={field.key}
                                            field={field}
                                            value={draft.customFields[field.key] ?? ''}
                                            onChange={value =>
                                                onDraftChange({
                                                    ...draft,
                                                    customFields: { ...draft.customFields, [field.key]: value },
                                                })
                                            }
                                        />
                                    ))}
                                </FieldGroup>
                            ) : null}
                        </div>
                        <AvatarPicker
                            preview={avatarPreview}
                            onSelect={dataUrl => onDraftChange({ ...draft, pictureDataUrl: dataUrl, resetToDefault: false })}
                            onUseDefault={() => onDraftChange({ ...draft, pictureDataUrl: null, resetToDefault: true })}
                        />
                    </div>
                    <div className="flex gap-2">
                        <Button type="submit" disabled={!dirty || saving || formInvalid}>
                            {saving ? 'Updating…' : 'Update'}
                        </Button>
                        <Button type="button" variant="outline" disabled={!dirty || saving} onClick={onCancel}>
                            Cancel
                        </Button>
                    </div>
                </form>
            </CardContent>
        </Card>
    );
}

function CustomFieldInput({
    field,
    value,
    onChange,
}: Readonly<{
    field: CustomUserField;
    value: string;
    onChange: (value: string) => void;
}>) {
    const id = `my-account-custom-${field.key}`;
    const choices = field.values ?? [];
    return (
        <Field orientation="vertical" className="gap-2">
            <FieldLabel htmlFor={id} required={field.required}>
                {field.label}
            </FieldLabel>
            {choices.length > 0 ? (
                <Select value={value} onValueChange={onChange} required={field.required}>
                    <SelectTrigger id={id}>
                        <SelectValue placeholder="Select a value" />
                    </SelectTrigger>
                    <SelectContent>
                        {choices.map(choice => (
                            <SelectItem key={choice} value={choice}>
                                {choice}
                            </SelectItem>
                        ))}
                    </SelectContent>
                </Select>
            ) : (
                <Input id={id} value={value} required={field.required} onChange={event => onChange(event.target.value)} />
            )}
            {field.required && !value.trim() ? <FieldError>This field is required.</FieldError> : null}
        </Field>
    );
}
