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
import { Badge, DataTable, DataTableEmptyState, DateCell, type DataTableProps } from '@gravitee/graphene-core';
import { SearchIcon } from '@gravitee/graphene-core/icons';
import { useNavigate } from 'react-router-dom';

import { SubscriptionStatusBadge } from './SubscriptionStatusBadge';
import type { Subscription } from '../../../types/subscription';

type ColCell<T> = { row: { original: T } };

function buildColumns(navigate: ReturnType<typeof useNavigate>): DataTableProps<Subscription>['columns'] {
    return [
        {
            id: 'Application',
            accessorFn: (row: Subscription) => row.application.name,
            header: 'Application',
            enableSorting: false,
            cell: ({ row }: ColCell<Subscription>) => {
                const sub = row.original;
                return (
                    <div>
                        <button type="button" className="text-left font-medium hover:underline" onClick={() => navigate(sub.id)}>
                            {sub.application.name}
                        </button>
                        {sub.application.primaryOwner?.displayName ? (
                            <div className="text-xs text-muted-foreground">{sub.application.primaryOwner.displayName}</div>
                        ) : null}
                    </div>
                );
            },
        },
        {
            id: 'Plan',
            accessorFn: (row: Subscription) => row.plan.name,
            header: 'Plan',
            enableSorting: false,
            cell: ({ row }: ColCell<Subscription>) => row.original.plan.name,
        },
        {
            id: 'Security',
            header: 'Security',
            enableSorting: false,
            cell: ({ row }: ColCell<Subscription>) => {
                const type = row.original.plan.security?.type;
                return type ? (
                    <Badge variant="secondary" className="text-xs font-mono">
                        {type === 'KEY_LESS' ? 'Keyless' : type}
                    </Badge>
                ) : (
                    <span className="text-muted-foreground">—</span>
                );
            },
        },
        {
            id: 'Status',
            header: 'Status',
            enableSorting: false,
            cell: ({ row }: ColCell<Subscription>) => <SubscriptionStatusBadge status={row.original.status} />,
        },
        {
            id: 'Created',
            accessorFn: (row: Subscription) => row.createdAt,
            header: 'Created',
            enableSorting: false,
            cell: ({ row }: ColCell<Subscription>) => <DateCell value={row.original.createdAt} format="absolute" />,
        },
    ];
}

interface ConsumersTableProps {
    subscriptions: Subscription[];
    totalCount: number;
    page: number;
    perPage: number;
    isLoading: boolean;
    onPage: (p: number) => void;
    onPerPageChange: (perPage: number) => void;
}

export function ConsumersTable({
    subscriptions,
    totalCount,
    page,
    perPage,
    isLoading,
    onPage,
    onPerPageChange,
}: Readonly<ConsumersTableProps>) {
    const navigate = useNavigate();
    const columns = buildColumns(navigate);

    return (
        <DataTable
            aria-label="Consumers"
            columns={columns}
            data={subscriptions}
            loading={isLoading}
            skeletonCount={perPage}
            serverSide
            pagination={{
                page,
                pageSize: perPage,
                totalCount,
                pageSizeOptions: [10, 25, 50, 100],
                onPageChange: onPage,
                onPageSizeChange: onPerPageChange,
            }}
            emptyMessage={
                <DataTableEmptyState
                    variant="no-results"
                    icon={<SearchIcon />}
                    title="No subscriptions match the current filters"
                    description="Try adjusting or clearing your filters."
                />
            }
        />
    );
}
