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
import { Button, Input } from '@gravitee/graphene-core';
import { PlusIcon, Trash2Icon } from '@gravitee/graphene-core/icons';

import { PORTAL_TENANT_MANAGEMENT_MODE_LABELS } from '../../tenants/types/portal-tenant.types';
import type { PermissionsTenantSummary } from '../hooks/usePermissionsDirectory';

interface TenantListPaneProps {
    readonly tenants: readonly PermissionsTenantSummary[];
    readonly selectedTenantId: string | null;
    readonly canCreate: boolean;
    readonly canDelete: boolean;
    readonly query: string;
    readonly onQueryChange: (query: string) => void;
    readonly onSelect: (tenantId: string) => void;
    readonly onCreate: () => void;
    readonly onDelete: (tenantId: string) => void;
}

export function TenantListPane({
    tenants,
    selectedTenantId,
    canCreate,
    canDelete,
    query,
    onQueryChange,
    onSelect,
    onCreate,
    onDelete,
}: TenantListPaneProps) {
    return (
        <div className="flex h-full min-h-0 flex-col overflow-hidden rounded-lg border bg-card">
            <div className="flex items-center justify-between gap-2 border-b bg-muted/40 px-3 py-2">
                <span className="text-sm font-medium">Tenants</span>
                <Button
                    type="button"
                    variant="ghost"
                    size="icon-sm"
                    aria-label="Create tenant"
                    disabled={!canCreate}
                    title={canCreate ? 'Create tenant' : undefined}
                    onClick={onCreate}
                >
                    <PlusIcon className="size-4" aria-hidden />
                </Button>
            </div>

            <div className="border-b p-2">
                <Input
                    placeholder="Search tenants"
                    aria-label="Search tenants"
                    value={query}
                    onChange={event => onQueryChange(event.target.value)}
                />
            </div>

            <ul className="min-h-0 flex-1 overflow-y-auto">
                {tenants.length === 0 ? (
                    <li className="px-3 py-6 text-center text-sm text-muted-foreground">No tenants yet.</li>
                ) : (
                    tenants.map(tenant => {
                        const isSelected = tenant.id === selectedTenantId;
                        const mode = tenant.managementMode
                            ? PORTAL_TENANT_MANAGEMENT_MODE_LABELS[tenant.managementMode]
                            : undefined;

                        return (
                            <li
                                key={tenant.id}
                                className={`group/tenant flex items-center border-b border-l-2 last:border-b-0 ${
                                    isSelected
                                        ? 'border-l-primary bg-accent'
                                        : 'border-l-transparent hover:bg-muted/50'
                                }`}
                            >
                                <button
                                    type="button"
                                    aria-current={isSelected}
                                    onClick={() => onSelect(tenant.id)}
                                    className="min-w-0 flex-1 px-3 py-2 text-left"
                                >
                                    <span
                                        className={`block truncate text-sm ${
                                            isSelected ? 'font-medium text-primary' : 'font-medium'
                                        }`}
                                    >
                                        {tenant.name}
                                    </span>
                                    <span className="mt-0.5 block truncate text-xs text-muted-foreground">
                                        {mode && (
                                            <span title={mode.long} className="font-medium">
                                                {mode.short}
                                            </span>
                                        )}
                                        {mode && ' · '}
                                        {tenant.groupCount} {tenant.groupCount === 1 ? 'group' : 'groups'} ·{' '}
                                        {tenant.userCount} {tenant.userCount === 1 ? 'user' : 'users'}
                                    </span>
                                </button>
                                {canDelete && (
                                    <Button
                                        type="button"
                                        variant="ghost"
                                        size="icon-sm"
                                        aria-label={`Delete ${tenant.name}`}
                                        className="mr-1 opacity-0 focus-visible:opacity-100 group-hover/tenant:opacity-100"
                                        onClick={() => onDelete(tenant.id)}
                                    >
                                        <Trash2Icon className="size-4" aria-hidden />
                                    </Button>
                                )}
                            </li>
                        );
                    })
                )}
            </ul>
        </div>
    );
}
