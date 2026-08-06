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
import { Badge, Button, DataTable, DataTablePagination, Input, type DataTableProps } from '@gravitee/graphene-core';
import { SearchIcon } from '@gravitee/graphene-core/icons';
import { useId, useEffect, useMemo, useState } from 'react';
import { Link, useLocation } from 'react-router-dom';

import { NON_SORTABLE_COLUMN } from '../../applications/utils/dataTableHeaders';
import type { ColCell } from '../../applications/utils/dataTableTypes';
import { TABLE_PAGE_SIZE_OPTIONS } from '../../applications/utils/paginationConstants';
import { useOrganizationUserApiProducts, useOrganizationUserApis, useOrganizationUserApplications } from '../hooks/useOrganizationUser';
import type { OrganizationEnvironment, UserInheritedApi, UserInheritedApiProduct, UserInheritedApplication } from '../types/user';
import {
    buildInheritedApiDetailPath,
    buildInheritedApiProductDetailPath,
    buildInheritedApplicationDetailPath,
} from '../utils/crossModuleResourcePath';
import { formatResourceVisibility } from '../utils/userGroupDisplay';
import {
    clampPage,
    filterInheritedResources,
    INHERITED_RESOURCES_DEFAULT_PAGE_SIZE,
    paginateInheritedResources,
} from '../utils/userInheritedResources';

interface UserInheritedPermissionsSectionProps {
    readonly userId: string;
    readonly environmentId: string;
    readonly environments: OrganizationEnvironment[];
}

interface InheritedResourcePathBuilder {
    readonly api: (resourceEnvironmentId: string, apiId: string) => string;
    readonly apiProduct: (resourceEnvironmentId: string, apiProductId: string) => string;
    readonly application: (resourceEnvironmentId: string, applicationId: string) => string;
}

function ResourceNameLink({
    name,
    to,
}: Readonly<{
    name: string;
    to: string;
}>) {
    return (
        <Button asChild variant="link" className="h-auto p-0 font-medium">
            <Link to={to}>{name}</Link>
        </Button>
    );
}

function VisibilityCell({ visibility }: Readonly<{ visibility?: string }>) {
    if (!visibility) {
        return <span className="text-muted-foreground">—</span>;
    }
    return (
        <Badge variant="outline" className="font-normal">
            {formatResourceVisibility(visibility)}
        </Badge>
    );
}

function InheritedResourcesTable<T extends { id: string; name?: string }>({
    title,
    ariaLabel,
    loading,
    isError,
    items,
    emptyLabel,
    searchPlaceholder,
    searchIgnoreKeys,
    columns,
}: Readonly<{
    title: string;
    ariaLabel: string;
    loading: boolean;
    isError?: boolean;
    items: T[];
    emptyLabel: string;
    searchPlaceholder: string;
    searchIgnoreKeys: readonly string[];
    columns: DataTableProps<T>['columns'];
}>) {
    const searchInputId = useId();
    const [search, setSearch] = useState('');
    const [page, setPage] = useState(1);
    const [pageSize, setPageSize] = useState(INHERITED_RESOURCES_DEFAULT_PAGE_SIZE);

    const filteredItems = useMemo(() => filterInheritedResources(items, search, searchIgnoreKeys), [items, search, searchIgnoreKeys]);
    const totalCount = filteredItems.length;
    const currentPage = clampPage(page, totalCount, pageSize);
    const paginatedItems = useMemo(
        () => paginateInheritedResources(filteredItems, currentPage, pageSize),
        [filteredItems, currentPage, pageSize],
    );
    const hasActiveSearch = search.trim().length > 0;

    useEffect(() => {
        if (page !== currentPage) {
            setPage(currentPage);
        }
    }, [page, currentPage]);

    function handleSearchChange(value: string) {
        setSearch(value);
        setPage(1);
    }

    function handlePageSizeChange(size: number) {
        setPageSize(size);
        setPage(1);
    }

    return (
        <section className="space-y-3" aria-label={ariaLabel}>
            <h3 className="text-sm font-medium">{title}</h3>
            <DataTablePagination
                page={currentPage}
                pageSize={pageSize}
                totalCount={totalCount}
                pageSizeOptions={[...TABLE_PAGE_SIZE_OPTIONS]}
                onPageChange={setPage}
                onPageSizeChange={handlePageSizeChange}
            >
                <div className="relative w-64">
                    <SearchIcon
                        className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground"
                        aria-hidden
                    />
                    <label htmlFor={searchInputId} className="sr-only">
                        Search {title.toLowerCase()}
                    </label>
                    <Input
                        id={searchInputId}
                        placeholder={searchPlaceholder}
                        value={search}
                        onChange={event => handleSearchChange(event.target.value)}
                        className="h-8 w-64 pl-9"
                    />
                </div>
            </DataTablePagination>
            <DataTable
                columns={columns}
                data={paginatedItems}
                loading={loading}
                skeletonCount={pageSize}
                serverSide
                emptyMessage={
                    isError
                        ? `Failed to load ${title.toLowerCase()}. Please refresh and try again.`
                        : hasActiveSearch
                          ? `No ${title.toLowerCase()} match your search.`
                          : emptyLabel
                }
            />
        </section>
    );
}

function buildApiColumns(environmentId: string, pathBuilder: InheritedResourcePathBuilder): DataTableProps<UserInheritedApi>['columns'] {
    return [
        {
            id: 'name',
            accessorFn: (api: UserInheritedApi) => api.name ?? api.id,
            header: 'Name',
            ...NON_SORTABLE_COLUMN,
            cell: ({ row }: ColCell<UserInheritedApi>) => {
                const api = row.original;
                const label = api.name ?? api.id;
                const envId = api.environmentId ?? environmentId;
                if (!envId) {
                    return <span className="font-medium">{label}</span>;
                }
                return <ResourceNameLink name={label} to={pathBuilder.api(envId, api.id)} />;
            },
        },
        {
            id: 'version',
            accessorFn: (api: UserInheritedApi) => api.version ?? '',
            header: 'Version',
            ...NON_SORTABLE_COLUMN,
            cell: ({ row }: ColCell<UserInheritedApi>) => <span className="text-muted-foreground">{row.original.version ?? '—'}</span>,
        },
        {
            id: 'visibility',
            accessorFn: (api: UserInheritedApi) => api.visibility ?? '',
            header: 'Visibility',
            ...NON_SORTABLE_COLUMN,
            cell: ({ row }: ColCell<UserInheritedApi>) => <VisibilityCell visibility={row.original.visibility} />,
        },
    ];
}

function buildApiProductColumns(
    environmentId: string,
    pathBuilder: InheritedResourcePathBuilder,
): DataTableProps<UserInheritedApiProduct>['columns'] {
    return [
        {
            id: 'name',
            accessorFn: (apiProduct: UserInheritedApiProduct) => apiProduct.name ?? apiProduct.id,
            header: 'Name',
            ...NON_SORTABLE_COLUMN,
            cell: ({ row }: ColCell<UserInheritedApiProduct>) => {
                const apiProduct = row.original;
                const label = apiProduct.name ?? apiProduct.id;
                const envId = apiProduct.environmentId ?? environmentId;
                if (!envId) {
                    return <span className="font-medium">{label}</span>;
                }
                return <ResourceNameLink name={label} to={pathBuilder.apiProduct(envId, apiProduct.id)} />;
            },
        },
        {
            id: 'version',
            accessorFn: (apiProduct: UserInheritedApiProduct) => apiProduct.version ?? '',
            header: 'Version',
            ...NON_SORTABLE_COLUMN,
            cell: ({ row }: ColCell<UserInheritedApiProduct>) => (
                <span className="text-muted-foreground">{row.original.version ?? '—'}</span>
            ),
        },
        {
            id: 'visibility',
            accessorFn: (apiProduct: UserInheritedApiProduct) => apiProduct.visibility ?? '',
            header: 'Visibility',
            ...NON_SORTABLE_COLUMN,
            cell: ({ row }: ColCell<UserInheritedApiProduct>) => <VisibilityCell visibility={row.original.visibility} />,
        },
    ];
}

function buildApplicationColumns(
    environmentId: string,
    pathBuilder: InheritedResourcePathBuilder,
): DataTableProps<UserInheritedApplication>['columns'] {
    return [
        {
            id: 'name',
            accessorFn: (application: UserInheritedApplication) => application.name ?? application.id,
            header: 'Name',
            ...NON_SORTABLE_COLUMN,
            cell: ({ row }: ColCell<UserInheritedApplication>) => {
                const application = row.original;
                const label = application.name ?? application.id;
                const envId = application.environmentId ?? environmentId;
                if (!envId) {
                    return <span className="font-medium">{label}</span>;
                }
                return <ResourceNameLink name={label} to={pathBuilder.application(envId, application.id)} />;
            },
        },
    ];
}

export function UserInheritedPermissionsSection({ userId, environmentId, environments }: UserInheritedPermissionsSectionProps) {
    const { pathname } = useLocation();
    const { data: apisResponse, isLoading: apisLoading, isError: apisError } = useOrganizationUserApis(userId, environmentId);
    const {
        data: apiProductsResponse,
        isLoading: apiProductsLoading,
        isError: apiProductsError,
    } = useOrganizationUserApiProducts(userId, environmentId);
    const {
        data: applicationsResponse,
        isLoading: applicationsLoading,
        isError: applicationsError,
    } = useOrganizationUserApplications(userId, environmentId);

    const apis = apisResponse?.data ?? [];
    const apiProducts = apiProductsResponse?.data ?? [];
    const applications = applicationsResponse?.data ?? [];

    const pathBuilder = useMemo<InheritedResourcePathBuilder>(
        () => ({
            api: (resourceEnvironmentId, apiId) => buildInheritedApiDetailPath(apiId, resourceEnvironmentId, environments, pathname),
            apiProduct: (resourceEnvironmentId, apiProductId) =>
                buildInheritedApiProductDetailPath(apiProductId, resourceEnvironmentId, environments, pathname),
            application: (resourceEnvironmentId, applicationId) =>
                buildInheritedApplicationDetailPath(applicationId, resourceEnvironmentId, environments, pathname),
        }),
        [environments, pathname],
    );

    const apiColumns = useMemo(() => buildApiColumns(environmentId, pathBuilder), [environmentId, pathBuilder]);
    const apiProductColumns = useMemo(() => buildApiProductColumns(environmentId, pathBuilder), [environmentId, pathBuilder]);
    const applicationColumns = useMemo(() => buildApplicationColumns(environmentId, pathBuilder), [environmentId, pathBuilder]);

    return (
        <div className="space-y-6 pt-6">
            <InheritedResourcesTable
                title="APIs"
                ariaLabel="Inherited APIs table"
                loading={apisLoading}
                isError={apisError}
                items={apis}
                emptyLabel="No API"
                searchPlaceholder="Search APIs…"
                searchIgnoreKeys={['id', 'visibility', 'environmentId']}
                columns={apiColumns}
            />
            <InheritedResourcesTable
                title="API Products"
                ariaLabel="Inherited API Products table"
                loading={apiProductsLoading}
                isError={apiProductsError}
                items={apiProducts}
                emptyLabel="No API Product"
                searchPlaceholder="Search API products…"
                searchIgnoreKeys={['id', 'visibility', 'environmentId']}
                columns={apiProductColumns}
            />
            <InheritedResourcesTable
                title="Applications"
                ariaLabel="Inherited Applications table"
                loading={applicationsLoading}
                isError={applicationsError}
                items={applications}
                emptyLabel="No application"
                searchPlaceholder="Search applications…"
                searchIgnoreKeys={['id', 'environmentId']}
                columns={applicationColumns}
            />
        </div>
    );
}
