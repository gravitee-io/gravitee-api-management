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
    Badge,
    Button,
    DataTable,
    Tooltip,
    TooltipContent,
    TooltipProvider,
    TooltipTrigger,
    type DataTableProps,
} from '@gravitee/graphene-core';
import { CircleXIcon, EyeIcon } from '@gravitee/graphene-core/icons';
import { useMemo, type ReactNode } from 'react';

import type { ApplicationSubscriptionTableRow } from '../../types/applicationSubscription';
import { formatApplicationDateTime } from '../../utils/applicationFormatters';
import { SUBSCRIPTION_PAGE_SIZE_OPTIONS } from '../../utils/applicationSubscriptionConstants';
import { canCloseSubscription } from '../../utils/applicationSubscriptionMapper';
import { NON_SORTABLE_COLUMN } from '../../utils/dataTableHeaders';
import type { ColCell } from '../../utils/dataTableTypes';

function SubscriptionStatusBadge({ status }: { status: ApplicationSubscriptionTableRow['status'] }) {
    const variant =
        status === 'ACCEPTED'
            ? 'default'
            : status === 'PENDING' || status === 'REJECTED'
              ? 'secondary'
              : status === 'CLOSED'
                ? 'destructive'
                : 'outline';

    return <Badge variant={variant}>{status}</Badge>;
}

function buildColumns({
    readOnly,
    canViewDetail,
    canClose,
    onView,
    onClose,
}: {
    readOnly: boolean;
    canViewDetail: boolean;
    canClose: boolean;
    onView: (row: ApplicationSubscriptionTableRow) => void;
    onClose: (row: ApplicationSubscriptionTableRow) => void;
}): DataTableProps<ApplicationSubscriptionTableRow>['columns'] {
    return [
        {
            accessorKey: 'securityType',
            header: 'Security type',
            ...NON_SORTABLE_COLUMN,
            cell: ({ row }: ColCell<ApplicationSubscriptionTableRow>) => (
                <>
                    <span className="text-sm">{row.original.securityType}</span>
                    {row.original.isSharedApiKey ? (
                        <Badge variant="outline" className="ml-2 text-[10px]">
                            Shared
                        </Badge>
                    ) : null}
                </>
            ),
        },
        {
            accessorKey: 'planName',
            header: 'Plan',
            ...NON_SORTABLE_COLUMN,
            cell: ({ row }: ColCell<ApplicationSubscriptionTableRow>) => <span className="text-sm">{row.original.planName}</span>,
        },
        {
            accessorKey: 'apiName',
            header: 'API',
            ...NON_SORTABLE_COLUMN,
            cell: ({ row }: ColCell<ApplicationSubscriptionTableRow>) => (
                <>
                    <p className="text-sm font-medium">{row.original.apiName}</p>
                    <p className="text-[11px] text-muted-foreground">
                        {row.original.referenceTypeLabel}
                        {row.original.apiVersion ? ` · v${row.original.apiVersion}` : ''}
                    </p>
                </>
            ),
        },
        {
            accessorKey: 'createdAt',
            header: 'Created at',
            ...NON_SORTABLE_COLUMN,
            cell: ({ row }: ColCell<ApplicationSubscriptionTableRow>) => (
                <span className="whitespace-nowrap text-sm text-muted-foreground">{formatApplicationDateTime(row.original.createdAt)}</span>
            ),
        },
        {
            accessorKey: 'processedAt',
            header: 'Processed at',
            ...NON_SORTABLE_COLUMN,
            cell: ({ row }: ColCell<ApplicationSubscriptionTableRow>) => (
                <span className="whitespace-nowrap text-sm text-muted-foreground">
                    {formatApplicationDateTime(row.original.processedAt)}
                </span>
            ),
        },
        {
            accessorKey: 'startingAt',
            header: 'Started at',
            ...NON_SORTABLE_COLUMN,
            cell: ({ row }: ColCell<ApplicationSubscriptionTableRow>) => (
                <span className="whitespace-nowrap text-sm text-muted-foreground">
                    {formatApplicationDateTime(row.original.startingAt)}
                </span>
            ),
        },
        {
            accessorKey: 'endAt',
            header: 'Ended at',
            ...NON_SORTABLE_COLUMN,
            cell: ({ row }: ColCell<ApplicationSubscriptionTableRow>) => (
                <span className="whitespace-nowrap text-sm text-muted-foreground">{formatApplicationDateTime(row.original.endAt)}</span>
            ),
        },
        {
            accessorKey: 'status',
            header: 'Status',
            ...NON_SORTABLE_COLUMN,
            cell: ({ row }: ColCell<ApplicationSubscriptionTableRow>) => <SubscriptionStatusBadge status={row.original.status} />,
        },
        {
            id: 'actions',
            header: () => <span className="sr-only">Actions</span>,
            size: 88,
            cell: ({ row }: ColCell<ApplicationSubscriptionTableRow>) => (
                <div className="flex items-center justify-end gap-0.5">
                    {canViewDetail ? (
                        <Button
                            type="button"
                            variant="ghost"
                            size="icon"
                            className="size-8"
                            aria-label="Subscription details"
                            onClick={() => onView(row.original)}
                        >
                            <EyeIcon className="size-4" aria-hidden />
                        </Button>
                    ) : null}
                    {!readOnly && canClose && canCloseSubscription(row.original.status) && row.original.origin !== 'KUBERNETES' ? (
                        <TooltipProvider>
                            <Tooltip>
                                <TooltipTrigger asChild>
                                    <Button
                                        type="button"
                                        variant="ghost"
                                        size="icon"
                                        className="size-8 text-destructive hover:text-destructive"
                                        aria-label="Close subscription"
                                        onClick={() => onClose(row.original)}
                                    >
                                        <CircleXIcon className="size-4" aria-hidden />
                                    </Button>
                                </TooltipTrigger>
                                <TooltipContent>Close subscription</TooltipContent>
                            </Tooltip>
                        </TooltipProvider>
                    ) : null}
                </div>
            ),
            enableSorting: false,
            enableHiding: false,
        },
    ];
}

export function ApplicationSubscriptionsTable({
    rows,
    isLoading,
    skeletonRowCount,
    readOnly,
    canViewDetail,
    canClose,
    onView,
    onClose,
    page,
    pageSize,
    totalCount,
    onPageChange,
    onPageSizeChange,
    toolbar,
}: Readonly<{
    rows: ApplicationSubscriptionTableRow[];
    isLoading: boolean;
    skeletonRowCount: number;
    readOnly: boolean;
    canViewDetail: boolean;
    canClose: boolean;
    onView: (row: ApplicationSubscriptionTableRow) => void;
    onClose: (row: ApplicationSubscriptionTableRow) => void;
    page: number;
    pageSize: number;
    totalCount: number;
    onPageChange: (page: number) => void;
    onPageSizeChange: (pageSize: number) => void;
    toolbar?: ReactNode;
}>) {
    const columns = useMemo(
        () => buildColumns({ readOnly, canViewDetail, canClose, onView, onClose }),
        [readOnly, canViewDetail, canClose, onView, onClose],
    );

    return (
        <DataTable
            aria-label="Application subscriptions"
            columns={columns}
            data={rows}
            enableColumnVisibility
            loading={isLoading}
            skeletonCount={skeletonRowCount}
            serverSide
            toolbar={toolbar}
            pagination={{
                page,
                pageSize,
                totalCount,
                pageSizeOptions: SUBSCRIPTION_PAGE_SIZE_OPTIONS,
                onPageChange,
                onPageSizeChange,
            }}
            emptyMessage="There is no subscription (yet)."
        />
    );
}
