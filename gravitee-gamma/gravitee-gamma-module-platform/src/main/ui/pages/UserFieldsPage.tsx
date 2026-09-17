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
import { Alert, AlertDescription, Button, Skeleton } from '@gravitee/graphene-core';
import { PlusIcon, TriangleAlertIcon } from '@gravitee/graphene-core/icons';
import { useState } from 'react';

import { useHasPermission } from '@gravitee/gamma-modules-sdk';

import { UserFieldDeleteDialog } from '../features/user-fields/components/UserFieldDeleteDialog';
import { UserFieldsEmptyLanding } from '../features/user-fields/components/UserFieldsEmptyLanding';
import { UserFieldSheet } from '../features/user-fields/components/UserFieldSheet';
import { UserFieldsTable } from '../features/user-fields/components/UserFieldsTable';
import { useCreateUserField, useDeleteUserField, useUpdateUserField } from '../features/user-fields/hooks/useUserFieldMutations';
import { useUserFields } from '../features/user-fields/hooks/useUserFields';
import type { UserField, UserFieldPayload } from '../features/user-fields/types/userField';
import {
    ORGANIZATION_CUSTOM_USER_FIELD_CREATE_PERMISSION,
    ORGANIZATION_CUSTOM_USER_FIELD_DELETE_PERMISSION,
    ORGANIZATION_CUSTOM_USER_FIELD_UPDATE_PERMISSION,
} from '../features/user-fields/utils/userFieldPermissions';
import { notify } from '../shared/notify';

type SheetState = { type: 'closed' } | { type: 'create' } | { type: 'edit'; field: UserField } | { type: 'delete'; field: UserField };

export function UserFieldsPage() {
    const canCreate = useHasPermission({ anyOf: [ORGANIZATION_CUSTOM_USER_FIELD_CREATE_PERMISSION] });
    const canEdit = useHasPermission({ anyOf: [ORGANIZATION_CUSTOM_USER_FIELD_UPDATE_PERMISSION] });
    const canDelete = useHasPermission({ anyOf: [ORGANIZATION_CUSTOM_USER_FIELD_DELETE_PERMISSION] });

    const { data: fields = [], isLoading, isError, refetch } = useUserFields();
    const createMutation = useCreateUserField();
    const updateMutation = useUpdateUserField();
    const deleteMutation = useDeleteUserField();

    const [sheet, setSheet] = useState<SheetState>({ type: 'closed' });
    // Kept across close so the sheet keeps its edit content while its closing animation runs,
    // instead of flipping to the create layout for a frame.
    const [editingField, setEditingField] = useState<UserField | undefined>(undefined);

    function openCreateSheet() {
        setEditingField(undefined);
        setSheet({ type: 'create' });
    }

    function openEditSheet(field: UserField) {
        setEditingField(field);
        setSheet({ type: 'edit', field });
    }

    function closeSheet() {
        setSheet({ type: 'closed' });
    }

    async function handleCreate(payload: UserFieldPayload) {
        try {
            const created = await createMutation.mutateAsync(payload);
            notify.success(`Field ${created.key} created successfully`);
            closeSheet();
        } catch (error) {
            notify.error(error, 'Error during field creation!');
        }
    }

    async function handleUpdate(payload: UserFieldPayload) {
        try {
            await updateMutation.mutateAsync(payload);
            notify.success(`Field ${payload.key} updated successfully`);
            closeSheet();
        } catch (error) {
            notify.error(error, 'Error during field update!');
        }
    }

    async function handleDelete() {
        if (sheet.type !== 'delete') return;
        const { key } = sheet.field;
        try {
            await deleteMutation.mutateAsync(key);
            notify.success(`Field ${key} deleted successfully`);
            closeSheet();
        } catch (error) {
            notify.error(error, 'Error during field deletion!');
        }
    }

    function renderContent() {
        if (isLoading) {
            return (
                <div className="space-y-2">
                    {Array.from({ length: 4 }).map((_, i) => (
                        <Skeleton key={i} className="h-12 w-full rounded-md" />
                    ))}
                </div>
            );
        }

        if (isError) {
            return (
                <Alert variant="destructive">
                    <TriangleAlertIcon className="size-4" aria-hidden />
                    <AlertDescription className="flex flex-wrap items-center gap-3">
                        Could not load user fields.
                        <Button size="sm" variant="outline" onClick={() => void refetch()}>
                            Try again
                        </Button>
                    </AlertDescription>
                </Alert>
            );
        }

        if (fields.length === 0) {
            return <UserFieldsEmptyLanding />;
        }

        return (
            <UserFieldsTable
                fields={fields}
                canEdit={canEdit}
                canDelete={canDelete}
                onEdit={openEditSheet}
                onDelete={field => setSheet({ type: 'delete', field })}
            />
        );
    }

    return (
        <div className="space-y-6">
            <div className="flex items-start justify-between gap-3">
                <div className="space-y-1">
                    <h1 className="text-2xl font-semibold tracking-tight">User Fields</h1>
                    <p className="text-sm text-muted-foreground">
                        Extra questions asked when someone signs up in the APIM console or the developer portal.
                    </p>
                </div>
                {canCreate && (
                    <Button className="shrink-0" onClick={openCreateSheet}>
                        <PlusIcon className="size-4" aria-hidden />
                        Add custom field
                    </Button>
                )}
            </div>

            {renderContent()}

            <UserFieldSheet
                open={sheet.type === 'create' || sheet.type === 'edit'}
                mode={editingField ? 'edit' : 'create'}
                field={editingField}
                onClose={closeSheet}
                onSubmit={editingField ? handleUpdate : handleCreate}
                isSaving={createMutation.isPending || updateMutation.isPending}
            />

            <UserFieldDeleteDialog
                open={sheet.type === 'delete'}
                field={sheet.type === 'delete' ? sheet.field : undefined}
                onClose={closeSheet}
                onConfirm={handleDelete}
                isDeleting={deleteMutation.isPending}
            />
        </div>
    );
}
