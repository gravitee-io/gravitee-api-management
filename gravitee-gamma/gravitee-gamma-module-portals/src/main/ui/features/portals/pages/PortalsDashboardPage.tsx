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
import {
    Button,
    Card,
    CardContent,
    CardDescription,
    CardFooter,
    CardHeader,
    Empty,
    EmptyDescription,
    EmptyHeader,
    EmptyTitle,
    Skeleton,
    ToggleGroup,
    ToggleGroupItem,
} from '@gravitee/graphene-core';
import { LayoutGridIcon, ListIcon } from '@gravitee/graphene-core/icons';
import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';

import { useHasPermission } from '@gravitee/gamma-modules-sdk';

import { PORTALS_ROUTE_CONFIG } from '../../../config/routes';
import { NewPortalButton } from '../components/NewPortalButton';
import { PortalQuota } from '../components/PortalQuota';
import { PortalSearchInput } from '../components/PortalSearchInput';
import { PortalTable } from '../components/PortalTable';
import { PortalTile } from '../components/PortalTile';
import { PublishedApisSummary } from '../components/PublishedApisSummary';
import { usePortals } from '../hooks/usePortals';
import { usePortalsStats } from '../hooks/usePortalsStats';
import { hasMorePortals } from '../utils/dashboardPreview';

export function PortalsDashboardPage() {
    const [view, setView] = useState<'tiles' | 'table'>('tiles');
    const [search, setSearch] = useState('');
    const [debouncedSearch, setDebouncedSearch] = useState('');
    const routing = useModuleRouting(PORTALS_ROUTE_CONFIG);
    const canConfigure = useHasPermission({ anyOf: ['environment-documentation-u'] });
    const portals = usePortals({ query: debouncedSearch, page: 1, perPage: 10 });
    const stats = usePortalsStats();

    useEffect(() => {
        const timeout = setTimeout(() => setDebouncedSearch(search.trim()), 300);
        return () => clearTimeout(timeout);
    }, [search]);

    const denied = [portals.error, stats.error].some(error => error instanceof ApimApiError && error.status === 403);
    const visibleLimit = view === 'tiles' ? 3 : 5;
    const visiblePortals = portals.data?.data.slice(0, visibleLimit) ?? [];
    const getOverviewPath = (id: string) => `${routing.pathForKey('dashboard').replace(/\/$/, '')}/${encodeURIComponent(id)}/overview`;

    if (denied) {
        return (
            <div className="space-y-6">
                <h1 className="text-2xl font-semibold">Dashboard</h1>
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
                <h1 className="text-2xl font-semibold tracking-tight">Dashboard</h1>
                <p className="text-sm text-muted-foreground">Manage and publish developer portals.</p>
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
                <div className="grid gap-4 sm:grid-cols-2">
                    <PortalQuota quota={stats.data?.quota} />
                    <PublishedApisSummary count={stats.data?.publishedApisCount} />
                </div>
            )}
            <section aria-label="Portals">
                <Card className="min-w-0 gap-4">
                    <CardHeader className="flex flex-wrap items-center justify-between gap-4">
                        <div className="space-y-1">
                            <h2 className="text-base font-medium">Portals</h2>
                            <CardDescription>Portals in this environment</CardDescription>
                        </div>
                        <div role="group" aria-label="Portal controls" className="flex min-w-0 flex-wrap items-center gap-2">
                            <PortalSearchInput value={search} onChange={setSearch} />
                            <ToggleGroup
                                type="single"
                                value={view}
                                onValueChange={value => {
                                    if (value === 'tiles' || value === 'table') setView(value);
                                }}
                                aria-label="Portal view"
                                variant="outline"
                            >
                                <ToggleGroupItem value="table" aria-label="Table">
                                    <ListIcon aria-hidden="true" />
                                </ToggleGroupItem>
                                <ToggleGroupItem value="tiles" aria-label="Tiles">
                                    <LayoutGridIcon aria-hidden="true" />
                                </ToggleGroupItem>
                            </ToggleGroup>
                            <NewPortalButton />
                        </div>
                    </CardHeader>
                    <CardContent className="min-w-0">
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
                        ) : portals.isPending ? (
                            <div role="status" aria-label="Loading portals" className="grid gap-3 md:grid-cols-2 lg:grid-cols-3">
                                {Array.from({ length: 3 }, (_, index) => (
                                    <Skeleton key={index} className="h-40 w-full" />
                                ))}
                            </div>
                        ) : visiblePortals.length === 0 ? (
                            <Empty>
                                <EmptyHeader>
                                    <EmptyTitle>{debouncedSearch ? 'No portals found' : 'No portals yet'}</EmptyTitle>
                                    <EmptyDescription>
                                        {debouncedSearch ? 'Try a different search.' : 'There are no portals in this environment.'}
                                    </EmptyDescription>
                                </EmptyHeader>
                            </Empty>
                        ) : view === 'tiles' ? (
                            <div className="grid gap-3 md:grid-cols-2 lg:grid-cols-3">
                                {visiblePortals.map(portal => (
                                    <PortalTile
                                        key={portal.id}
                                        portal={portal}
                                        canConfigure={canConfigure}
                                        overviewPath={getOverviewPath(portal.id)}
                                    />
                                ))}
                            </div>
                        ) : (
                            <div className="min-w-0 overflow-x-auto">
                                <PortalTable portals={visiblePortals} canConfigure={canConfigure} getOverviewPath={getOverviewPath} />
                            </div>
                        )}
                    </CardContent>
                    {!portals.isError && portals.data && hasMorePortals(portals.data, visibleLimit) ? (
                        <CardFooter className="justify-end">
                            <Button variant="link" asChild>
                                <Link to={routing.pathForKey('portals')}>View all Portals</Link>
                            </Button>
                        </CardFooter>
                    ) : null}
                </Card>
            </section>
        </div>
    );
}
