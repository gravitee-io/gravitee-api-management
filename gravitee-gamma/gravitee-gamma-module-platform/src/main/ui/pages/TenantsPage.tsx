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

import { useHasPermission } from '@gravitee/gamma-modules-sdk';
import { Alert, AlertDescription, Button, Skeleton } from '@gravitee/graphene-core';
import { InfoIcon, PlusIcon } from '@gravitee/graphene-core/icons';
import { useState } from 'react';

import { TenantDeleteDialog } from '../features/tenants/components/TenantDeleteDialog';
import { TenantFormSheet } from '../features/tenants/components/TenantFormSheet';
import { TenantsEmptyState } from '../features/tenants/components/TenantsEmptyState';
import { TenantsTable } from '../features/tenants/components/TenantsTable';
import { useCreateTenant, useDeleteTenant, useUpdateTenant } from '../features/tenants/hooks/useTenantMutations';
import { useTenants } from '../features/tenants/hooks/useTenants';
import type { NewTenantPayload, Tenant, UpdateTenantPayload } from '../features/tenants/types/tenant';
import { useForbiddenResourceRedirect } from '../shared/hooks/useForbiddenResourceRedirect';
import { notify } from '../shared/notify';
import { isForbiddenApiError } from '../shared/utils/apiErrors';

type SheetState = { type: 'closed' } | { type: 'create' } | { type: 'edit'; tenant: Tenant } | { type: 'delete'; tenant: Tenant };

export function TenantsPage() {
    const canCreate = useHasPermission({ anyOf: ['organization-tenant-c', 'environment-tenant-c'] });
    const canEdit = useHasPermission({ anyOf: ['organization-tenant-u', 'environment-tenant-u'] });
    const canDelete = useHasPermission({ anyOf: ['organization-tenant-d', 'environment-tenant-d'] });

    const { data: tenants = [], isLoading, isError, error } = useTenants();
    const createMutation = useCreateTenant();
    const updateMutation = useUpdateTenant();
    const deleteMutation = useDeleteTenant();

    const isForbidden = isForbiddenApiError(isError, error);
    useForbiddenResourceRedirect({
        isForbidden,
        permissionPrefix: ['organization-tenant-', 'environment-tenant-'],
        redirectTo: '../applications',
    });

    const [sheet, setSheet] = useState<SheetState>({ type: 'closed' });

    function closeSheet() {
        setSheet({ type: 'closed' });
    }

    async function handleCreate(data: NewTenantPayload) {
        await createMutation.mutateAsync(data);
        notify.success('Tenant successfully created!');
        closeSheet();
    }

    async function handleUpdate(data: UpdateTenantPayload) {
        // The API skips entries whose key no longer exists and still answers 200 with an empty list.
        const updated = await updateMutation.mutateAsync(data);
        if (updated.length === 0) {
            throw new Error('This tenant no longer exists. Refresh the page and try again.');
        }
        notify.success('Tenant successfully updated!');
        closeSheet();
    }

    async function handleDelete() {
        if (sheet.type !== 'delete') return;
        try {
            await deleteMutation.mutateAsync(sheet.tenant.key);
            notify.success('Tenant successfully deleted!');
            closeSheet();
        } catch (deleteError) {
            notify.error(deleteError, 'Failed to delete tenant');
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

        if (isForbidden) {
            return null;
        }

        if (isError) {
            return (
                <div className="flex items-center justify-center p-8">
                    <p className="text-sm text-muted-foreground">Failed to load tenants. Please refresh and try again.</p>
                </div>
            );
        }

        if (tenants.length === 0) {
            return <TenantsEmptyState canCreate={canCreate} />;
        }

        return (
            <TenantsTable
                rows={tenants}
                canEdit={canEdit}
                canDelete={canDelete}
                onEdit={tenant => setSheet({ type: 'edit', tenant })}
                onDelete={tenant => setSheet({ type: 'delete', tenant })}
            />
        );
    }

    return (
        <div className="space-y-6">
            <div className="flex items-start justify-between gap-4">
                <div className="space-y-1">
                    <h1 className="text-2xl font-semibold tracking-tight">Tenants</h1>
                    <p className="text-sm text-muted-foreground">
                        Segment which API endpoints a gateway will proxy to, so one API can stay local to a region.
                    </p>
                </div>
                {canCreate ? (
                    <Button className="shrink-0" onClick={() => setSheet({ type: 'create' })}>
                        <PlusIcon className="size-4" aria-hidden />
                        Add a tenant
                    </Button>
                ) : null}
            </div>

            {tenants.length > 0 ? (
                <Alert>
                    <InfoIcon className="size-4" aria-hidden />
                    <AlertDescription>
                        Copy the tenant key into the API gateway configuration file so the gateway only receives endpoints tagged with that
                        tenant.
                    </AlertDescription>
                </Alert>
            ) : null}

            {renderContent()}

            <TenantFormSheet
                open={sheet.type === 'create'}
                mode="create"
                existingTenants={tenants}
                onClose={closeSheet}
                onSubmit={handleCreate}
                isSaving={createMutation.isPending}
            />

            <TenantFormSheet
                open={sheet.type === 'edit'}
                mode="edit"
                tenant={sheet.type === 'edit' ? sheet.tenant : undefined}
                existingTenants={tenants}
                onClose={closeSheet}
                onSubmit={handleUpdate}
                isSaving={updateMutation.isPending}
            />

            <TenantDeleteDialog
                open={sheet.type === 'delete'}
                tenant={sheet.type === 'delete' ? sheet.tenant : undefined}
                onClose={closeSheet}
                onConfirm={handleDelete}
                isDeleting={deleteMutation.isPending}
            />
        </div>
    );
}
