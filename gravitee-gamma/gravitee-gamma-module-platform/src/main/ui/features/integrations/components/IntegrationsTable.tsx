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

import { DataTable, type DataTableProps } from '@gravitee/graphene-core';

import { IntegrationProviderLabel } from './IntegrationProviderLabel';
import { IntegrationStatusBadge } from './IntegrationStatusBadge';
import type { ColCell } from '../../../shared/utils/dataTableTypes';
import type { Integration } from '../types/integration';
import { SMALLEST_TABLE_PAGE_SIZE, TABLE_PAGE_SIZE_OPTIONS } from '../utils/paginationConstants';

const COLUMNS: DataTableProps<Integration>['columns'] = [
    {
        id: 'name',
        accessorKey: 'name',
        enableSorting: false,
        header: 'Name',
        cell: ({ row }: ColCell<Integration>) => <span className="text-sm font-medium text-foreground">{row.original.name}</span>,
    },
    {
        id: 'provider',
        accessorKey: 'provider',
        enableSorting: false,
        header: 'Provider',
        cell: ({ row }: ColCell<Integration>) => <IntegrationProviderLabel provider={row.original.provider} />,
    },
    {
        id: 'status',
        enableSorting: false,
        header: 'Status',
        cell: ({ row }: ColCell<Integration>) => <IntegrationStatusBadge agentStatus={row.original.agentStatus} />,
    },
];

interface IntegrationsTableProps {
    readonly integrations: Integration[];
    readonly totalCount: number;
    readonly page: number;
    readonly pageSize: number;
    readonly loading: boolean;
    readonly onPageChange: (page: number) => void;
    readonly onPageSizeChange: (size: number) => void;
}

export function IntegrationsTable({
    integrations,
    totalCount,
    page,
    pageSize,
    loading,
    onPageChange,
    onPageSizeChange,
}: IntegrationsTableProps) {
    const pagination =
        totalCount > SMALLEST_TABLE_PAGE_SIZE
            ? {
                  page,
                  pageSize,
                  totalCount,
                  pageSizeOptions: [...TABLE_PAGE_SIZE_OPTIONS],
                  onPageChange,
                  onPageSizeChange: (size: number) => {
                      onPageSizeChange(size);
                      onPageChange(1);
                  },
              }
            : undefined;

    return (
        <DataTable
            aria-label="Integrations"
            columns={COLUMNS}
            data={integrations}
            loading={loading}
            skeletonCount={pageSize}
            serverSide
            pagination={pagination}
        />
    );
}
