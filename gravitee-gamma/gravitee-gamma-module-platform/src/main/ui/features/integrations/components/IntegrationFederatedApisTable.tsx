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

import { Button, DataTable, type DataTableProps } from '@gravitee/graphene-core';
import { PencilIcon } from '@gravitee/graphene-core/icons';
import { Link } from 'react-router-dom';

import type { ColCell } from '../../../shared/utils/dataTableTypes';
import type { IntegrationFederatedApi } from '../types/integration';
import { SMALLEST_TABLE_PAGE_SIZE, TABLE_PAGE_SIZE_OPTIONS } from '../utils/paginationConstants';

function buildColumns(apiHref: (apiId: string) => string, canConfigure: boolean): DataTableProps<IntegrationFederatedApi>['columns'] {
    return [
        {
            id: 'name',
            accessorKey: 'name',
            enableSorting: false,
            header: 'API Name',
            cell: ({ row }: ColCell<IntegrationFederatedApi>) => (
                <Button asChild variant="link" className="h-auto p-0 text-left text-sm font-medium text-foreground hover:underline">
                    <Link to={apiHref(row.original.id)}>
                        {row.original.name} ({row.original.version})
                    </Link>
                </Button>
            ),
        },
        {
            id: 'actions',
            enableSorting: false,
            header: canConfigure ? 'Actions' : '',
            cell: ({ row }: ColCell<IntegrationFederatedApi>) =>
                canConfigure ? (
                    <Button asChild variant="ghost" size="icon" className="size-8" aria-label="Configure federated API">
                        <Link to={apiHref(row.original.id)}>
                            <PencilIcon className="size-4" aria-hidden />
                        </Link>
                    </Button>
                ) : null,
        },
    ];
}

interface IntegrationFederatedApisTableProps {
    readonly apis: IntegrationFederatedApi[];
    readonly totalCount: number;
    readonly page: number;
    readonly pageSize: number;
    readonly loading: boolean;
    readonly canConfigure: boolean;
    readonly apiHref: (apiId: string) => string;
    readonly onPageChange: (page: number) => void;
    readonly onPageSizeChange: (size: number) => void;
}

export function IntegrationFederatedApisTable({
    apis,
    totalCount,
    page,
    pageSize,
    loading,
    canConfigure,
    apiHref,
    onPageChange,
    onPageSizeChange,
}: IntegrationFederatedApisTableProps) {
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
            aria-label="Federated APIs"
            columns={buildColumns(apiHref, canConfigure)}
            data={apis}
            loading={loading}
            skeletonCount={pageSize}
            serverSide
            pagination={pagination}
        />
    );
}
