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
import { Trash2Icon } from '@gravitee/graphene-core/icons';

import { ConfirmDialog } from '../../../shared/components/ConfirmDialog';
import type { PortalTenant } from '../types/portal-tenant.types';

interface DeletePortalTenantDialogProps {
    readonly tenant: PortalTenant | null;
    readonly open: boolean;
    readonly isPending: boolean;
    readonly onOpenChange: (open: boolean) => void;
    readonly onConfirm: () => void;
}

export function DeletePortalTenantDialog({
    tenant,
    open,
    isPending,
    onOpenChange,
    onConfirm,
}: DeletePortalTenantDialogProps) {
    if (!tenant) {
        return null;
    }

    return (
        <ConfirmDialog
            open={open}
            onOpenChange={onOpenChange}
            title={`Delete "${tenant.name}"`}
            description="This tenant and all member assignments will be permanently removed. Applications are not deleted."
            confirmLabel="Delete tenant"
            pendingLabel="Deleting…"
            destructive
            isPending={isPending}
            confirmKeyword={tenant.name}
            icon={<Trash2Icon className="size-4" aria-hidden="true" />}
            onConfirm={onConfirm}
        />
    );
}
