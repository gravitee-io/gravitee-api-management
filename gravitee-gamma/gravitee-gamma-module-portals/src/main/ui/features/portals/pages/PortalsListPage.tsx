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
import { useModuleRouting } from '@gravitee/gamma-modules-sdk/routing';
import { ApimApiError } from '@gravitee/gamma-ui-shared/api';
import { Button, Card, CardContent, CardHeader, Empty, EmptyDescription, EmptyHeader, EmptyTitle, Skeleton } from '@gravitee/graphene-core';
import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';

import { useEnvironment, useHasPermission } from '@gravitee/gamma-modules-sdk';

import { PORTALS_ROUTE_CONFIG } from '../../../config/routes';
import { NewPortalButton } from '../components/NewPortalButton';
import { PortalQuota } from '../components/PortalQuota';
import { PortalSearchInput } from '../components/PortalSearchInput';
import { PortalTable } from '../components/PortalTable';
import { usePortals } from '../hooks/usePortals';
import { usePortalsStats } from '../hooks/usePortalsStats';
import { getListPagination } from '../utils/listPagination';

const PER_PAGE = 10;

export function PortalsListPage() {
    const environmentId = useEnvironment()?.id ?? '';
    return <PortalsListContent key={environmentId} />;
}

function PortalsListContent() {
    const [search, setSearch] = useState('');
    const [params, setParams] = useState({ query: '', page: 1 });
    const routing = useModuleRouting(PORTALS_ROUTE_CONFIG);
    const navigate = useNavigate();
    const canConfigure = useHasPermission({ anyOf: ['environment-documentation-u'] });
    const portals = usePortals({ ...params, perPage: PER_PAGE }, { keepPreviousData: true });
    const stats = usePortalsStats();

    useEffect(() => {
        const timeout = setTimeout(() => {
            const query = search.trim();
            setParams(previous => (previous.query === query ? previous : { query, page: 1 }));
        }, 300);
        return () => clearTimeout(timeout);
    }, [search]);

    const pagination = portals.data ? getListPagination(portals.data, params.page, PER_PAGE) : undefined;
    const denied = [portals.error, stats.error].some(error => error instanceof ApimApiError && error.status === 403);
    const getOverviewPath = (id: string) => `${routing.pathForKey('dashboard').replace(/\/$/, '')}/${encodeURIComponent(id)}/overview`;

    if (denied) {
        return (
            <div className="space-y-6">
                <h1 className="text-2xl font-semibold">Portals</h1>
                <Empty role="alert">
                    <EmptyHeader>
                        <EmptyTitle>Access denied</EmptyTitle>
                        <EmptyDescription>You do not have permission to view portals in this environment.</EmptyDescription>
                    </EmptyHeader>
                </Empty>
            </div>
        );
    }

    return (
        <div className="space-y-6">
            <div className="space-y-1">
                <h1 className="text-2xl font-semibold tracking-tight">Portals</h1>
                <p className="text-sm text-muted-foreground">Find and manage developer portals in this environment.</p>
            </div>
            {stats.isError ? (
                <Empty role="alert">
                    <EmptyHeader>
                        <EmptyTitle>Unable to load portal statistics</EmptyTitle>
                    </EmptyHeader>
                    <Button variant="outline" onClick={() => void stats.refetch()} disabled={stats.isFetching}>
                        Retry
                    </Button>
                </Empty>
            ) : (
                <div className="sm:w-64">
                    <PortalQuota quota={stats.data?.quota} />
                </div>
            )}
            <section aria-label="Portals">
                <Card className="min-w-0 gap-4">
                    <CardHeader className="flex flex-wrap items-center justify-between gap-4">
                        <h2 className="text-base font-medium">All portals</h2>
                        <div role="group" aria-label="Portal controls" className="flex min-w-0 flex-wrap items-center gap-2">
                            <PortalSearchInput value={search} onChange={setSearch} />
                            <NewPortalButton />
                        </div>
                    </CardHeader>
                    <CardContent className="min-w-0 space-y-4" aria-busy={portals.isFetching}>
                        {portals.isError ? (
                            <Empty role="alert">
                                <EmptyHeader>
                                    <EmptyTitle>Unable to load portals</EmptyTitle>
                                    <EmptyDescription>Please try again.</EmptyDescription>
                                </EmptyHeader>
                                <Button variant="outline" onClick={() => void portals.refetch()} disabled={portals.isFetching}>
                                    Retry
                                </Button>
                            </Empty>
                        ) : portals.isPending || (portals.isPlaceholderData && portals.data.data.length === 0) ? (
                            <div role="status" aria-label="Loading portals" className="space-y-3">
                                {Array.from({ length: 5 }, (_, index) => (
                                    <Skeleton key={index} className="h-16 w-full" />
                                ))}
                            </div>
                        ) : portals.data.data.length === 0 ? (
                            <Empty>
                                <EmptyHeader>
                                    <EmptyTitle>
                                        {params.page > 1 ? 'No portals on this page' : params.query ? 'No portals found' : 'No portals yet'}
                                    </EmptyTitle>
                                    <EmptyDescription>
                                        {params.page > 1
                                            ? 'Go back to a previous page.'
                                            : params.query
                                              ? 'Try a different search.'
                                              : 'There are no portals in this environment.'}
                                    </EmptyDescription>
                                </EmptyHeader>
                            </Empty>
                        ) : (
                            <div className="min-w-0 overflow-x-auto">
                                <PortalTable
                                    portals={portals.data.data}
                                    canConfigure={false}
                                    getOverviewPath={getOverviewPath}
                                    onRowClick={canConfigure ? portal => navigate(getOverviewPath(portal.id)) : undefined}
                                />
                            </div>
                        )}
                        {!portals.isError && pagination ? (
                            <nav aria-label="Portal pagination" className="flex flex-wrap items-center justify-end gap-2">
                                <Button
                                    variant="outline"
                                    size="sm"
                                    aria-label="Previous page"
                                    disabled={!pagination.hasPrevious || portals.isFetching}
                                    onClick={() => setParams(previous => ({ ...previous, page: pagination.page - 1 }))}
                                >
                                    Previous
                                </Button>
                                <span role="status" className="text-sm text-muted-foreground">
                                    {portals.isPlaceholderData
                                        ? 'Loading portals…'
                                        : pagination.pageCount
                                          ? `Page ${pagination.page} of ${pagination.pageCount}`
                                          : `Page ${pagination.page}`}
                                </span>
                                <Button
                                    variant="outline"
                                    size="sm"
                                    aria-label="Next page"
                                    disabled={!pagination.hasNext || portals.isFetching}
                                    onClick={() => setParams(previous => ({ ...previous, page: pagination.page + 1 }))}
                                >
                                    Next
                                </Button>
                            </nav>
                        ) : null}
                    </CardContent>
                </Card>
            </section>
        </div>
    );
}
