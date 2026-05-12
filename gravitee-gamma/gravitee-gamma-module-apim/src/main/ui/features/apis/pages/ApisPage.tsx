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
import { useHasPermission } from '@gravitee/gamma-modules-sdk';
import { useModuleRouting } from '@gravitee/gamma-modules-sdk/routing';
import {
    Badge,
    Button,
    Card,
    CardContent,
    DataTablePagination,
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuTrigger,
    Empty,
    EmptyContent,
    EmptyDescription,
    EmptyHeader,
    EmptyTitle,
    Input,
    Separator,
    Skeleton,
    Table,
    TableBody,
    TableHead,
    TableHeader,
    TableRow,
    TableCell,
} from '@gravitee/graphene-core';
import {
    AlertCircleIcon,
    ArrowRightIcon,
    BarChart3Icon,
    CircleCheckIcon,
    CircleXIcon,
    FileTextIcon,
    LayoutGridIcon,
    MonitorIcon,
    MoreHorizontalIcon,
    PlusIcon,
    RadioIcon,
    SearchIcon,
    ServerIcon,
    ShieldIcon,
    SlidersHorizontalIcon,
    XIcon,
} from '@gravitee/graphene-core/icons';
import type { LucideIcon } from 'lucide-react';
import { useCallback, useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';

import { APIM_ROUTE_CONFIG } from '../../../config/routes';
import { useApisSearch } from '../hooks/useApisSearch';

// ── State → Graphene primitive mappings ──────────────────────────────────────
// Icons marked TODO will be swapped to the correct Graphene barrel icons once
// Jourdi ships: PlayIcon (STARTED), CircleStopIcon (STOPPED),
// RefreshCwIcon (IN_SYNC), RefreshCwOffIcon (OUT_OF_SYNC).

type StateBadgeConfig = { icon: LucideIcon; label: string; className: string };

const RUNTIME_STATE_CONFIG: Record<string, StateBadgeConfig> = {
    STARTED: { icon: CircleCheckIcon, label: 'Started', className: 'bg-success/10 text-success border-transparent' }, // TODO: PlayIcon
    STOPPED: { icon: CircleXIcon, label: 'Stopped', className: 'bg-destructive/10 text-destructive border-transparent' }, // TODO: CircleStopIcon
};

const SYNC_STATE_CONFIG: Record<string, StateBadgeConfig> = {
    DEPLOYED: { icon: CircleCheckIcon, label: 'Synced', className: 'bg-success/10 text-success border-transparent' }, // TODO: RefreshCwIcon
    NEED_REDEPLOY: { icon: AlertCircleIcon, label: 'Out of sync', className: 'bg-warning/10 text-warning border-transparent' }, // TODO: RefreshCwOffIcon
};

function RuntimeStatusBadge({ state }: Readonly<{ state?: string }>) {
    const cfg = (state ? RUNTIME_STATE_CONFIG[state] : undefined) ?? RUNTIME_STATE_CONFIG['STOPPED'];
    return (
        <Badge className={cfg.className}>
            <cfg.icon className="size-3" aria-hidden="true" />
            {cfg.label}
        </Badge>
    );
}

function SyncStatusBadge({ deploymentState }: Readonly<{ deploymentState?: string }>) {
    if (!deploymentState) return <span className="text-muted-foreground text-sm">—</span>;
    const cfg = SYNC_STATE_CONFIG[deploymentState] ?? SYNC_STATE_CONFIG['NEED_REDEPLOY'];
    return (
        <Badge className={cfg.className}>
            <cfg.icon className="size-3" aria-hidden="true" />
            {cfg.label}
        </Badge>
    );
}

const PER_PAGE_OPTIONS = [25, 50, 100, 200];

export function ApisPage() {
    const canCreateApi = useHasPermission({ anyOf: ['environment-api-c'] });
    const navigate = useNavigate();
    const { rootPath, navigateToKey } = useModuleRouting(APIM_ROUTE_CONFIG);
    const handleCreateProxy = useCallback(() => {
        navigate(`${rootPath}/new`);
    }, [navigate, rootPath]);

    const [query, setQuery] = useState('');
    const [page, setPage] = useState(1);
    const [perPage, setPerPage] = useState(25);
    const [bannerDismissed, setBannerDismissed] = useState(false);
    const apisSearch = useApisSearch({ query, page, perPage, sortBy: 'name' });

    const { isInitialLoading, isError, hasApis, isFirstApi, isSearchEmpty, isTrueEmpty } = useMemo(() => {
        const isLoaded = apisSearch.data !== null;
        const isError = apisSearch.status === 'error';
        const count = apisSearch.totalCount;
        const searchActive = query.trim() !== '';

        return {
            isInitialLoading: !isLoaded && !isError,
            isError,
            hasApis: count > 0,
            isFirstApi: count === 1 && !searchActive,
            isSearchEmpty: isLoaded && count === 0 && searchActive,
            isTrueEmpty: isLoaded && !isError && count === 0 && !searchActive,
        };
    }, [query, apisSearch.data, apisSearch.status, apisSearch.totalCount]);

    useEffect(() => {
        setPage(1);
    }, [query, perPage]);

    const stats = useMemo(() => {
        const total = apisSearch.totalCount;
        const published = apisSearch.apis.filter(api => api.lifecycleState === 'PUBLISHED').length;
        return { total, published, draft: Math.max(0, total - published) };
    }, [apisSearch.apis, apisSearch.totalCount]);

    const handleSelectApi = useCallback((apiId: string) => navigate(`${rootPath}/${apiId}`), [navigate, rootPath]);
    const handlePageChange = useCallback((next: number) => setPage(next), []);
    const handlePerPageChange = useCallback((next: number) => setPerPage(next), []);
    const handleDismissBanner = useCallback(() => setBannerDismissed(true), []);

    return (
        <div className="space-y-6">
            <div className="flex items-center justify-between gap-4">
                <div>
                    <h1 className="text-2xl font-semibold tracking-tight">API Proxies</h1>
                    <p className="text-sm text-muted-foreground mt-1">Manage and monitor your API proxies</p>
                </div>

                {canCreateApi ? (
                    <Button type="button" size="sm" onClick={handleCreateProxy}>
                        <PlusIcon className="mr-2 size-4" aria-hidden="true" />
                        Create New Proxy
                    </Button>
                ) : null}
            </div>

            {isInitialLoading ? (
                <>
                    <div className="flex gap-4">
                        {Array.from({ length: 4 }).map((_, index) => (
                            <Card key={index} className="flex-1">
                                <CardContent className="p-4 space-y-2">
                                    <Skeleton className="h-4 w-24" />
                                    <Skeleton className="h-8 w-12" />
                                </CardContent>
                            </Card>
                        ))}
                    </div>
                    <Skeleton className="h-9 w-64" />
                    <div className="rounded-lg border overflow-hidden divide-y">
                        <div className="flex gap-6 px-4 py-3">
                            {['w-32', 'w-16', 'w-16', 'w-20', 'w-24', 'w-16'].map((widthClass, index) => (
                                <Skeleton key={index} className={`h-4 ${widthClass}`} />
                            ))}
                        </div>
                        {Array.from({ length: 5 }).map((_, index) => (
                            <div key={index} className="flex gap-6 items-center px-4 py-3">
                                <Skeleton className="h-4 w-32" />
                                <Skeleton className="h-4 w-16" />
                                <Skeleton className="h-4 w-16" />
                                <Skeleton className="h-5 w-20 rounded-full" />
                                <Skeleton className="h-4 w-12" />
                                <Skeleton className="h-4 w-12" />
                            </div>
                        ))}
                    </div>
                </>
            ) : isError && !hasApis ? (
                <Card className="rounded-xl border border-destructive/30 bg-destructive/5 p-6">
                    <div className="space-y-1">
                        <p className="text-sm font-semibold text-destructive">Could not load API list</p>
                        <p className="text-xs text-muted-foreground">
                            {apisSearch.error?.message ?? 'Check that the Management API is reachable and you are authenticated.'}
                        </p>
                    </div>
                </Card>
            ) : hasApis ? (
                <>
                    {isFirstApi && !bannerDismissed ? (
                        <Card className="p-5 space-y-4 bg-primary/5 border border-primary/20">
                            <div className="flex items-start justify-between gap-4">
                                <div className="flex items-start gap-3 min-w-0">
                                    <div className="size-9 rounded-lg flex items-center justify-center shrink-0 bg-primary/10">
                                        <RadioIcon className="size-4.5 text-primary" aria-hidden="true" />
                                    </div>
                                    <div className="min-w-0">
                                        <p className="text-sm font-medium">First proxy is ready</p>
                                        <p className="text-xs text-muted-foreground mt-0.5">
                                            Add a plan, tighten security, and watch traffic as you connect clients.
                                        </p>
                                    </div>
                                </div>

                                <Button
                                    type="button"
                                    size="icon"
                                    variant="ghost"
                                    className="shrink-0"
                                    onClick={handleDismissBanner}
                                    aria-label="Dismiss"
                                >
                                    <XIcon className="size-4" aria-hidden="true" />
                                </Button>
                            </div>

                            <div className="grid gap-2 sm:grid-cols-2 lg:grid-cols-3">
                                <button
                                    type="button"
                                    className="group flex items-start gap-3 rounded-lg border bg-card p-3 text-left transition-all hover:shadow-sm hover:border-foreground/20"
                                >
                                    <div className="size-8 rounded-md bg-muted/50 flex items-center justify-center shrink-0">
                                        <SlidersHorizontalIcon className="size-4 text-muted-foreground" aria-hidden="true" />
                                    </div>
                                    <div className="flex-1 min-w-0">
                                        <p className="text-sm font-medium truncate">Add a plan</p>
                                        <p className="text-xs text-muted-foreground mt-0.5 line-clamp-2">
                                            Rate limits and quotas for this API
                                        </p>
                                    </div>
                                    <ArrowRightIcon
                                        className="size-3 text-muted-foreground mt-1 shrink-0 transition-transform group-hover:translate-x-0.5"
                                        aria-hidden="true"
                                    />
                                </button>

                                <button
                                    type="button"
                                    className="group flex items-start gap-3 rounded-lg border bg-card p-3 text-left transition-all hover:shadow-sm hover:border-foreground/20"
                                >
                                    <div className="size-8 rounded-md bg-muted/50 flex items-center justify-center shrink-0">
                                        <ShieldIcon className="size-4 text-muted-foreground" aria-hidden="true" />
                                    </div>
                                    <div className="flex-1 min-w-0">
                                        <p className="text-sm font-medium truncate">Configure authorization</p>
                                        <p className="text-xs text-muted-foreground mt-0.5 line-clamp-2">Enforce who can call your proxy</p>
                                    </div>
                                    <ArrowRightIcon
                                        className="size-3 text-muted-foreground mt-1 shrink-0 transition-transform group-hover:translate-x-0.5"
                                        aria-hidden="true"
                                    />
                                </button>

                                <button
                                    type="button"
                                    className="group flex items-start gap-3 rounded-lg border bg-card p-3 text-left transition-all hover:shadow-sm hover:border-foreground/20"
                                >
                                    <div className="size-8 rounded-md bg-muted/50 flex items-center justify-center shrink-0">
                                        <FileTextIcon className="size-4 text-muted-foreground" aria-hidden="true" />
                                    </div>
                                    <div className="flex-1 min-w-0">
                                        <p className="text-sm font-medium truncate">View API logs</p>
                                        <p className="text-xs text-muted-foreground mt-0.5 line-clamp-2">Inspect requests in context</p>
                                    </div>
                                    <ArrowRightIcon
                                        className="size-3 text-muted-foreground mt-1 shrink-0 transition-transform group-hover:translate-x-0.5"
                                        aria-hidden="true"
                                    />
                                </button>
                            </div>
                        </Card>
                    ) : null}

                    <div className="flex gap-4">
                        {(
                            [
                                { label: 'Total APIs', value: stats.total },
                                { label: 'Published', value: stats.published },
                                { label: 'Draft', value: stats.draft },
                                { label: 'Total Calls (24h)', value: 0 },
                            ] as const
                        ).map(({ label, value }) => (
                            <Card key={label} className="flex-1">
                                <CardContent className="p-4">
                                    <p className="text-sm font-medium text-muted-foreground">{label}</p>
                                    <p className="text-2xl font-bold mt-1">{value}</p>
                                </CardContent>
                            </Card>
                        ))}
                    </div>

                    <div className="flex items-center justify-between gap-4">
                        <div className="relative max-w-sm flex-1">
                            <SearchIcon
                                className="absolute left-3 top-1/2 -translate-y-1/2 size-4 text-muted-foreground"
                                aria-hidden="true"
                            />
                            <Input
                                className="pl-9"
                                placeholder="Search APIs..."
                                value={query}
                                onChange={event => setQuery(event.target.value)}
                                aria-label="Search APIs"
                            />
                        </div>
                        <DataTablePagination
                            page={page}
                            pageSize={perPage}
                            totalCount={apisSearch.totalCount}
                            pageSizeOptions={PER_PAGE_OPTIONS}
                            onPageChange={handlePageChange}
                            onPageSizeChange={handlePerPageChange}
                        />
                    </div>

                    <div className="rounded-lg border overflow-hidden">
                        <div className="relative w-full overflow-x-auto">
                            <Table>
                                <TableHeader>
                                    <TableRow className="border-b transition-colors">
                                        <TableHead className="whitespace-nowrap">API Name</TableHead>
                                        <TableHead className="whitespace-nowrap">Runtime Status</TableHead>
                                        <TableHead className="whitespace-nowrap">Sync Status</TableHead>
                                        <TableHead className="whitespace-nowrap">Access</TableHead>
                                        <TableHead className="whitespace-nowrap">Owner</TableHead>
                                        <TableHead className="whitespace-nowrap">Calls (24h)</TableHead>
                                        <TableHead className="w-10 text-right" />
                                    </TableRow>
                                </TableHeader>
                                <TableBody>
                                    {apisSearch.apis.map(api => (
                                        <TableRow
                                            key={api.id}
                                            className="cursor-pointer hover:bg-muted/50"
                                            onClick={() => handleSelectApi(api.id)}
                                        >
                                            <TableCell className="font-medium whitespace-nowrap">{api.name}</TableCell>
                                            <TableCell className="whitespace-nowrap">
                                                <RuntimeStatusBadge state={api.state} />
                                            </TableCell>
                                            <TableCell className="whitespace-nowrap">
                                                <SyncStatusBadge deploymentState={api.deploymentState} />
                                            </TableCell>
                                            <TableCell className="whitespace-nowrap">
                                                {api.contextPath ? (
                                                    <Badge variant="outline" className="font-normal font-mono text-xs">
                                                        {api.contextPath}
                                                    </Badge>
                                                ) : (
                                                    <span className="text-muted-foreground text-sm">—</span>
                                                )}
                                            </TableCell>
                                            <TableCell className="whitespace-nowrap text-sm text-muted-foreground">
                                                {api.primaryOwnerDisplayName ?? '—'}
                                            </TableCell>
                                            <TableCell className="whitespace-nowrap">0</TableCell>
                                            <TableCell className="whitespace-nowrap text-right" onClick={e => e.stopPropagation()}>
                                                <DropdownMenu>
                                                    <DropdownMenuTrigger asChild>
                                                        <Button
                                                            type="button"
                                                            variant="ghost"
                                                            size="icon"
                                                            className="size-8"
                                                            aria-label="More actions"
                                                        >
                                                            <MoreHorizontalIcon className="size-4" />
                                                        </Button>
                                                    </DropdownMenuTrigger>
                                                    <DropdownMenuContent align="end">
                                                        <DropdownMenuItem onClick={() => navigate(`${rootPath}/${api.id}/overview`)}>
                                                            <LayoutGridIcon className="size-4" />
                                                            View Details
                                                        </DropdownMenuItem>
                                                        <DropdownMenuItem onClick={() => navigate(`${rootPath}/${api.id}/general`)}>
                                                            <SlidersHorizontalIcon className="size-4" />
                                                            Edit Configuration
                                                        </DropdownMenuItem>
                                                        <DropdownMenuItem onClick={() => navigateToKey('analytics')}>
                                                            <BarChart3Icon className="size-4" />
                                                            View Analytics
                                                        </DropdownMenuItem>
                                                    </DropdownMenuContent>
                                                </DropdownMenu>
                                            </TableCell>
                                        </TableRow>
                                    ))}
                                </TableBody>
                            </Table>
                        </div>
                    </div>

                    <DataTablePagination
                        page={page}
                        pageSize={perPage}
                        totalCount={apisSearch.totalCount}
                        pageSizeOptions={PER_PAGE_OPTIONS}
                        onPageChange={handlePageChange}
                        onPageSizeChange={handlePerPageChange}
                    />
                </>
            ) : isSearchEmpty ? (
                <>
                    <div className="relative max-w-sm">
                        <SearchIcon className="absolute left-3 top-1/2 -translate-y-1/2 size-4 text-muted-foreground" aria-hidden="true" />
                        <Input
                            className="pl-9"
                            placeholder="Search APIs..."
                            value={query}
                            onChange={event => setQuery(event.target.value)}
                            aria-label="Search APIs"
                        />
                    </div>
                    <Card>
                        <CardContent className="p-10">
                            <Empty>
                                <EmptyHeader>
                                    <EmptyTitle>No APIs match your search</EmptyTitle>
                                    <EmptyDescription>
                                        No results for <strong>&ldquo;{query}&rdquo;</strong>. Try a different name or clear the search.
                                    </EmptyDescription>
                                </EmptyHeader>
                                <EmptyContent>
                                    <Button type="button" variant="outline" size="sm" onClick={() => setQuery('')}>
                                        Clear search
                                    </Button>
                                </EmptyContent>
                            </Empty>
                        </CardContent>
                    </Card>
                </>
            ) : isTrueEmpty ? (
                <Card>
                    <CardContent className="p-6">
                        <Empty>
                            <EmptyHeader>
                                <EmptyTitle>Why add an API proxy?</EmptyTitle>
                                <EmptyDescription>
                                    A proxy sits in front of your backend so you can secure traffic, control usage, and observe
                                    requests—without changing upstream services.
                                </EmptyDescription>
                            </EmptyHeader>

                            <EmptyContent className="space-y-4">
                                <div className="flex items-start gap-4">
                                    <div className="flex-1 rounded-lg border border-primary bg-primary/10 p-4">
                                        <p className="text-xs font-semibold text-primary mb-3">Direct access (no proxy)</p>
                                        <div className="flex items-center justify-center gap-4">
                                            <div className="flex flex-col items-center gap-1 text-center">
                                                <div className="rounded-lg border bg-card size-10 flex items-center justify-center">
                                                    <MonitorIcon className="size-5 text-muted-foreground" aria-hidden="true" />
                                                </div>
                                                <p className="text-xs font-semibold">Client</p>
                                            </div>
                                            <ArrowRightIcon className="size-5 text-primary shrink-0" aria-hidden="true" />
                                            <div className="flex flex-col items-center gap-1 text-center">
                                                <div className="rounded-lg border bg-card size-10 flex items-center justify-center">
                                                    <ServerIcon className="size-5 text-muted-foreground" aria-hidden="true" />
                                                </div>
                                                <p className="text-xs font-semibold">Backend</p>
                                            </div>
                                        </div>
                                        <ul className="mt-4 space-y-1 text-xs text-muted-foreground">
                                            <li>Authentication and throttling must be handled by the backend</li>
                                            <li>No centralized traffic shaping and analytics</li>
                                            <li>Harder to add policies without touching upstream code</li>
                                        </ul>
                                    </div>

                                    <div className="self-stretch shrink-0 flex items-center justify-center">
                                        <ArrowRightIcon className="size-5 text-primary shrink-0" aria-hidden="true" />
                                    </div>

                                    <div className="flex-1 rounded-lg border border-primary p-4">
                                        <p className="text-xs font-semibold text-primary mb-3">With an API proxy</p>
                                        <div className="flex items-center justify-center gap-3">
                                            <div className="flex flex-col items-center gap-1 text-center">
                                                <div className="rounded-lg border bg-card size-10 flex items-center justify-center">
                                                    <MonitorIcon className="size-5 text-muted-foreground" aria-hidden="true" />
                                                </div>
                                                <p className="text-xs font-semibold">Client</p>
                                            </div>
                                            <ArrowRightIcon className="size-5 text-primary shrink-0" aria-hidden="true" />
                                            <div className="flex flex-col items-center gap-1 text-center">
                                                <div className="rounded-lg bg-primary/10 size-10 flex items-center justify-center">
                                                    <RadioIcon className="size-5 text-primary" aria-hidden="true" />
                                                </div>
                                                <p className="text-xs font-semibold">API proxy</p>
                                            </div>
                                            <ArrowRightIcon className="size-5 text-primary shrink-0" aria-hidden="true" />
                                            <div className="flex flex-col items-center gap-1 text-center">
                                                <div className="rounded-lg border bg-card size-10 flex items-center justify-center">
                                                    <ServerIcon className="size-5 text-muted-foreground" aria-hidden="true" />
                                                </div>
                                                <p className="text-xs font-semibold">Backend</p>
                                            </div>
                                        </div>
                                        <ul className="mt-4 space-y-1 text-xs text-muted-foreground">
                                            <li>Authentication, authorization, and key management</li>
                                            <li>Rate limits, quotas, and request shaping</li>
                                            <li>Logs, metrics, and end-to-end tracing</li>
                                        </ul>
                                    </div>
                                </div>

                                <Separator />

                                <div className="grid gap-4 grid-cols-3">
                                    <div className="space-y-2">
                                        <div className="flex items-center gap-2">
                                            <div className="rounded-lg bg-primary/10 p-1">
                                                <ShieldIcon className="size-3.5 text-primary" aria-hidden="true" />
                                            </div>
                                            <p className="text-sm font-semibold">Security</p>
                                        </div>
                                        <p className="text-xs text-muted-foreground">
                                            Enforce who can call your API, validate tokens, and add encryption at the edge.
                                        </p>
                                    </div>

                                    <div className="space-y-2">
                                        <div className="flex items-center gap-2">
                                            <div className="rounded-lg bg-primary/10 p-1">
                                                <SlidersHorizontalIcon className="size-3.5 text-primary" aria-hidden="true" />
                                            </div>
                                            <p className="text-sm font-semibold">Traffic control</p>
                                        </div>
                                        <p className="text-xs text-muted-foreground">
                                            Throttle traffic, set quotas, and protect backends from abuse or misbehaving clients.
                                        </p>
                                    </div>

                                    <div className="space-y-2">
                                        <div className="flex items-center gap-2">
                                            <div className="rounded-lg bg-primary/10 p-1">
                                                <BarChart3Icon className="size-3.5 text-primary" aria-hidden="true" />
                                            </div>
                                            <p className="text-sm font-semibold">Observability</p>
                                        </div>
                                        <p className="text-xs text-muted-foreground">
                                            Capture every request, debug failures fast, and measure health across environments.
                                        </p>
                                    </div>
                                </div>
                            </EmptyContent>
                        </Empty>
                    </CardContent>
                </Card>
            ) : null}
        </div>
    );
}
