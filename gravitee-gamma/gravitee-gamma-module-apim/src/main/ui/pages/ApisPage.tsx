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
import {
    Badge,
    Button,
    Card,
    Input,
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
    Table,
    TableBody,
    TableHead,
    TableHeader,
    TableRow,
    TableCell,
} from '@gravitee/graphene-core';
import {
    ArrowRightIcon,
    BarChart3Icon,
    FileTextIcon,
    MonitorIcon,
    PlusIcon,
    RadioIcon,
    SearchIcon,
    ServerIcon,
    ShieldIcon,
    SlidersHorizontalIcon,
    XIcon,
} from '@gravitee/graphene-core/icons';
import { useCallback, useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useModuleRouting } from '@gravitee/gamma-modules-sdk/routing';
import { APIM_ROUTE_CONFIG } from '../config/routes';

import { useApisSearch } from './apis/useApisSearch';

export function ApisPage() {
    const canCreateApi = useHasPermission({ anyOf: ['environment-api-c'] });
    const navigate = useNavigate();
    const { rootPath } = useModuleRouting(APIM_ROUTE_CONFIG);
    const handleCreateProxy = useCallback(() => {
        navigate(`${rootPath}/new`);
    }, [navigate, rootPath]);

    const [query, setQuery] = useState('');
    const [page, setPage] = useState(1);
    const [perPage, setPerPage] = useState(25);
    const [bannerDismissed, setBannerDismissed] = useState(false);
    const apisSearch = useApisSearch({ query, page, perPage, sortBy: 'name' });

    const hasApis = apisSearch.status === 'success' && apisSearch.totalCount > 0;

    useEffect(() => {
        setPage(1);
    }, [query, perPage]);

    const stats = useMemo(() => {
        const total = apisSearch.totalCount;
        const published = apisSearch.apis.filter((a) => a.state === 'STARTED').length;
        const draft = Math.max(0, total - published);
        return { total, published, draft };
    }, [apisSearch.apis, apisSearch.totalCount]);

    const handleSelectApi = useCallback(
        (apiId: string) => {
            navigate(`${rootPath}/${apiId}`);
        },
        [navigate, rootPath],
    );

    return (
        <div className="space-y-6">
            <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
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

            {hasApis ? (
                <>
                    {!bannerDismissed ? (
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
                                    onClick={() => setBannerDismissed(true)}
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
                                        <p className="text-xs text-muted-foreground mt-0.5 line-clamp-2">Rate limits and quotas for this API</p>
                                    </div>
                                    <ArrowRightIcon className="size-3 text-muted-foreground mt-1 shrink-0 transition-transform group-hover:translate-x-0.5" aria-hidden="true" />
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
                                    <ArrowRightIcon className="size-3 text-muted-foreground mt-1 shrink-0 transition-transform group-hover:translate-x-0.5" aria-hidden="true" />
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
                                    <ArrowRightIcon className="size-3 text-muted-foreground mt-1 shrink-0 transition-transform group-hover:translate-x-0.5" aria-hidden="true" />
                                </button>
                            </div>
                        </Card>
                    ) : null}

                    <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
                        <Card className="p-4">
                            <p className="text-sm font-medium text-muted-foreground">Total APIs</p>
                            <p className="text-2xl font-bold mt-1">{stats.total}</p>
                        </Card>
                        <Card className="p-4">
                            <p className="text-sm font-medium text-muted-foreground">Published</p>
                            <p className="text-2xl font-bold mt-1">{stats.published}</p>
                        </Card>
                        <Card className="p-4">
                            <p className="text-sm font-medium text-muted-foreground">Draft</p>
                            <p className="text-2xl font-bold mt-1">{stats.draft}</p>
                        </Card>
                        <Card className="p-4">
                            <p className="text-sm font-medium text-muted-foreground">Total Calls (24h)</p>
                            <p className="text-2xl font-bold mt-1">0</p>
                        </Card>
                    </div>

                    <div className="relative max-w-sm">
                        <SearchIcon className="absolute left-3 top-1/2 -translate-y-1/2 size-4 text-muted-foreground" aria-hidden="true" />
                        <Input
                            className="pl-9"
                            placeholder="Search APIs..."
                            value={query}
                            onChange={(e) => setQuery(e.target.value)}
                            aria-label="Search APIs"
                        />
                    </div>

                    <div className="rounded-lg border overflow-hidden">
                        <div className="relative w-full overflow-x-auto">
                            <Table>
                                <TableHeader>
                                    <TableRow className="border-b transition-colors">
                                        <TableHead className="whitespace-nowrap">API Name</TableHead>
                                        <TableHead className="whitespace-nowrap">Protocol</TableHead>
                                        <TableHead className="whitespace-nowrap">Version</TableHead>
                                        <TableHead className="whitespace-nowrap">Status</TableHead>
                                        <TableHead className="whitespace-nowrap">Calls (24h)</TableHead>
                                        <TableHead className="whitespace-nowrap">Uptime</TableHead>
                                        <TableHead className="w-10" />
                                    </TableRow>
                                </TableHeader>
                                <TableBody>
                                    {apisSearch.apis.map((api) => {
                                        const published = api.state === 'STARTED';
                                        return (
                                            <TableRow
                                                key={api.id}
                                                className="cursor-pointer hover:bg-muted/50"
                                                onClick={() => handleSelectApi(api.id)}
                                            >
                                                <TableCell className="font-medium whitespace-nowrap">{api.name}</TableCell>
                                                <TableCell className="whitespace-nowrap">REST</TableCell>
                                                <TableCell className="whitespace-nowrap">{api.version ?? '-'}</TableCell>
                                                <TableCell className="whitespace-nowrap">
                                                    <Badge
                                                        variant="secondary"
                                                        className={
                                                            published
                                                                ? 'border border-primary/25 bg-primary/10 text-primary'
                                                                : 'border border-muted-foreground/25 bg-muted/40 text-muted-foreground'
                                                        }
                                                    >
                                                        {published ? 'Published' : 'Draft'}
                                                    </Badge>
                                                </TableCell>
                                                <TableCell className="whitespace-nowrap">0</TableCell>
                                                <TableCell className="whitespace-nowrap">-</TableCell>
                                                <TableCell className="whitespace-nowrap" />
                                            </TableRow>
                                        );
                                    })}
                                </TableBody>
                            </Table>
                        </div>
                    </div>

                    <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
                        <div className="flex items-center gap-2 text-sm text-muted-foreground">
                            <span>Rows per page</span>
                            <Select value={String(perPage)} onValueChange={(v) => setPerPage(Number(v))}>
                                <SelectTrigger className="h-8 w-[92px]">
                                    <SelectValue />
                                </SelectTrigger>
                                <SelectContent>
                                    <SelectItem value="10">10</SelectItem>
                                    <SelectItem value="25">25</SelectItem>
                                    <SelectItem value="50">50</SelectItem>
                                </SelectContent>
                            </Select>
                        </div>

                        <div className="flex items-center gap-2">
                            <span className="text-sm text-muted-foreground">
                                Page {apisSearch.status === 'success' ? apisSearch.data.pagination.page : page} of{' '}
                                {apisSearch.status === 'success' ? apisSearch.data.pagination.pageCount : 1}
                            </span>
                            <Button
                                type="button"
                                size="sm"
                                variant="outline"
                                onClick={() => setPage((p) => Math.max(1, p - 1))}
                                disabled={apisSearch.status !== 'success' || apisSearch.data.pagination.page <= 1}
                            >
                                Previous
                            </Button>
                            <Button
                                type="button"
                                size="sm"
                                variant="outline"
                                onClick={() => setPage((p) => p + 1)}
                                disabled={apisSearch.status !== 'success' || apisSearch.data.pagination.page >= apisSearch.data.pagination.pageCount}
                            >
                                Next
                            </Button>
                        </div>
                    </div>
                </>
            ) : (
                <Card className="p-6">
                    <div className="space-y-4">
                        <div className="space-y-1">
                            <h2 className="text-sm font-semibold">Why add an API proxy?</h2>
                            <p className="text-xs text-muted-foreground">
                                A proxy sits in front of your backend so you can secure traffic, control usage, and observe requests—without changing
                                upstream services.
                            </p>
                        </div>

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

                        <div className="grid gap-4 grid-cols-3 border-t pt-4">
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
                    </div>
                </Card>
            )}
        </div>
    );
}
