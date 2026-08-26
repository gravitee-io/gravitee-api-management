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
import { Alert, AlertDescription, Button, Card, CardContent, Field, FieldLabel, Input, Switch } from '@gravitee/graphene-core';
import { useState, type FormEvent } from 'react';

import { RolePermissionsTable } from './RolePermissionsTable';
import { extractErrorMessage } from '../../../shared/notify/extractErrorMessage';
import type { Role, RoleScope } from '../types/role';
import { fromFormPermissionsToPermissions, toFormPermissions, type RolePermissionsForm } from '../utils/rolePermissions';

export interface RoleFormSubmitValues {
    name: string;
    description?: string;
    default: boolean;
    permissions: Role['permissions'];
}

interface RoleFormState {
    name: string;
    description: string;
    default: boolean;
    permissions: RolePermissionsForm;
}

function initialFormState(role: Role | undefined, permissionNames: readonly string[]): RoleFormState {
    return {
        name: role?.name ?? '',
        description: role?.description ?? '',
        default: role?.default ?? false,
        permissions: toFormPermissions(role, [...permissionNames]),
    };
}

export function RoleForm({
    scope,
    role,
    permissionNames,
    isReadOnly,
    isSaving,
    onSubmit,
    onCancel,
}: Readonly<{
    scope: RoleScope;
    role?: Role;
    permissionNames: readonly string[];
    isReadOnly: boolean;
    isSaving: boolean;
    onSubmit: (values: RoleFormSubmitValues) => Promise<void>;
    onCancel: () => void;
}>) {
    const isEditMode = Boolean(role);
    const [initialForm] = useState<RoleFormState>(() => initialFormState(role, permissionNames));
    const [form, setForm] = useState<RoleFormState>(initialForm);
    const [submitError, setSubmitError] = useState<string | null>(null);
    const [nameTouched, setNameTouched] = useState(false);

    // Mirrors OrgSettingsRoleComponent: description/default follow role.system directly, independent of the
    // systemRoleEdition-aware isReadOnly getter, which only governs the permission matrix and the banner below.
    const areBasicFieldsDisabled = isSaving || Boolean(role?.system);
    const nameValid = form.name.trim().length > 0;
    // A pristine, untouched field shouldn't show "required" — only once the user has interacted with it
    // (matches Angular Material's default ErrorStateMatcher, which gates on touched/dirty).
    const showNameError = nameTouched && !nameValid;
    // Mirrors gio-save-bar's dirty-gated Save button (creationMode skips this — a filled-in create form is
    // always submittable, there is nothing to be "unchanged" from).
    const isDirty = JSON.stringify(form) !== JSON.stringify(initialForm);
    const canSubmit = nameValid && !isSaving && !isReadOnly && (!isEditMode || isDirty);

    async function handleSubmit(event: FormEvent) {
        event.preventDefault();
        if (!canSubmit) return;

        try {
            setSubmitError(null);
            await onSubmit({
                name: form.name.trim().toUpperCase(),
                description: form.description.trim() || undefined,
                default: form.default,
                permissions: fromFormPermissionsToPermissions(form.permissions),
            });
        } catch (error) {
            setSubmitError(extractErrorMessage(error, isEditMode ? 'Failed to update role' : 'Failed to create role'));
        }
    }

    return (
        <form onSubmit={handleSubmit} className="space-y-6" aria-label={isEditMode ? 'Update role' : 'Create role'}>
            {isReadOnly ? (
                <Alert>
                    <AlertDescription>System role are not editable</AlertDescription>
                </Alert>
            ) : null}

            <Card>
                <CardContent className="space-y-4">
                    <Field orientation="vertical" className="gap-1.5">
                        <FieldLabel htmlFor="role-name">Role name</FieldLabel>
                        <Input
                            id="role-name"
                            value={form.name}
                            onChange={event => {
                                setNameTouched(true);
                                setSubmitError(null);
                                setForm(prev => ({ ...prev, name: event.target.value }));
                            }}
                            disabled={isEditMode || isSaving}
                            required
                            aria-invalid={showNameError}
                            aria-describedby="role-name-hint"
                        />
                        <p id="role-name-hint" className="text-xs text-muted-foreground">
                            The name cannot be changed after it has been created.
                        </p>
                        {showNameError ? (
                            <p className="text-sm text-destructive" role="alert">
                                Name is required.
                            </p>
                        ) : null}
                    </Field>

                    <Field orientation="vertical" className="gap-1.5">
                        <FieldLabel htmlFor="role-description">Role description</FieldLabel>
                        <Input
                            id="role-description"
                            value={form.description}
                            onChange={event => setForm(prev => ({ ...prev, description: event.target.value }))}
                            disabled={areBasicFieldsDisabled}
                        />
                    </Field>

                    <div className="flex items-center gap-2">
                        <Switch
                            id="role-default"
                            checked={form.default}
                            onCheckedChange={checked => setForm(prev => ({ ...prev, default: checked }))}
                            disabled={areBasicFieldsDisabled}
                            aria-label="Default role toggle"
                        />
                        <FieldLabel htmlFor="role-default">Default role</FieldLabel>
                    </div>
                </CardContent>
            </Card>

            <Card>
                <CardContent>
                    <RolePermissionsTable
                        scope={scope}
                        permissionNames={permissionNames}
                        value={form.permissions}
                        onChange={permissions => setForm(prev => ({ ...prev, permissions }))}
                        disabled={isReadOnly || isSaving}
                    />
                </CardContent>
            </Card>

            {submitError ? (
                <p className="text-sm text-destructive" role="alert">
                    {submitError}
                </p>
            ) : null}

            <div className="flex justify-end gap-2">
                <Button type="button" variant="outline" onClick={onCancel} disabled={isSaving}>
                    Cancel
                </Button>
                <Button type="submit" disabled={!canSubmit}>
                    {isSaving ? 'Saving...' : isEditMode ? 'Save' : 'Create role'}
                </Button>
            </div>
        </form>
    );
}
