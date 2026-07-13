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
import { DataTable } from '@gravitee/graphene-core';
import { ShieldCheckIcon } from '@gravitee/graphene-core/icons';
import type { ColumnDef } from '@tanstack/react-table';
import { useMemo } from 'react';

import { PLAN_SECURITY_CATALOG, type PlanSecurityType } from './planSecurityCatalog';

export function PlanSecurityCatalogPage() {
    const columns: ColumnDef<PlanSecurityType>[] = useMemo(
        () => [
            {
                id: 'id',
                accessorKey: 'id',
                enableSorting: false,
                header: () => <span className="text-sm font-medium text-muted-foreground">Security Type ID</span>,
                cell: ({ row }) => <span className="font-mono text-xs">{row.original.id}</span>,
            },
            {
                id: 'name',
                accessorKey: 'name',
                enableSorting: false,
                header: () => <span className="text-sm font-medium text-muted-foreground">Name</span>,
                cell: ({ row }) => <span className="text-sm font-medium">{row.original.name}</span>,
            },
            {
                id: 'policyName',
                accessorKey: 'policyName',
                enableSorting: false,
                header: () => <span className="text-sm font-medium text-muted-foreground">Policy Name</span>,
                cell: ({ row }) => <span className="font-mono text-xs rounded bg-muted px-1.5 py-0.5">{row.original.policyName}</span>,
            },
        ],
        [],
    );

    return (
        <div className="space-y-6">
            <div className="space-y-1">
                <h1 className="text-2xl font-semibold tracking-tight">Plan Security Type Catalog</h1>
                <p className="text-sm text-muted-foreground">
                    Reference catalog of available plan security types supported by the platform.
                </p>
            </div>

            <div className="rounded-lg border">
                <div className="flex items-center gap-2 border-b px-4 py-3">
                    <ShieldCheckIcon className="size-5 text-muted-foreground" aria-hidden />
                    <h2 className="text-base font-semibold">Security Types</h2>
                </div>

                <DataTable
                    columns={columns}
                    data={PLAN_SECURITY_CATALOG}
                    emptyMessage="No security types available."
                    className="**:data-[slot=table-cell]:py-3"
                />
            </div>
        </div>
    );
}
