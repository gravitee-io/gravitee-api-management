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
    Alert,
    AlertDescription,
    Badge,
    Button,
    Card,
    CardContent,
    DataTablePagination,
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
    Input,
    Skeleton,
    Table,
    TableBody,
    TableCell,
    TableHead,
    TableHeader,
    TableRow,
} from '@gravitee/graphene-core';
import {
    ArrowRightIcon,
    CircleCheckIcon,
    CircleXIcon,
    DatabaseIcon,
    GlobeIcon,
    KeyRoundIcon,
    LockIcon,
    MoreHorizontalIcon,
    PencilIcon,
    PlusIcon,
    SearchIcon,
    ServerIcon,
    ShieldCheckIcon,
    Trash2Icon,
} from '@gravitee/graphene-core/icons';
import { useMemo, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';

import { ConfirmDialog } from '../../../../../shared/components/ConfirmDialog';
import { notify } from '../../../../../shared/notify';
import { PluginIcon } from '../../../components/PluginIcon';
import { useApiDetail } from '../../../hooks/useApiDetail';
import { useResourcePlugins, useUpdateApiResources } from '../../../hooks/useApiResources';
import type { ApiResource } from '../../../types/resource';

const PAGE_SIZE_OPTIONS = [10, 25, 50];

// ─── Empty landing ────────────────────────────────────────────────────────────

function LearningNode({ icon: Icon, label }: { icon: typeof DatabaseIcon; label: string }) {
    return (
        <div className="flex flex-col items-center gap-1 text-center">
            <div className="rounded-lg bg-muted p-2">
                <Icon className="size-4 text-muted-foreground" />
            </div>
            <p className="text-xs font-medium">{label}</p>
        </div>
    );
}

function ResourcesEmptyLanding() {
    return (
        <Card>
            <CardContent className="space-y-6 pt-6">
                <div>
                    <h3 className="text-base font-semibold">Why add resources?</h3>
                    <p className="mt-0.5 text-xs text-muted-foreground">
                        Resources like caches, OAuth providers, and authentication adapters are shared infrastructure your API policies can
                        use at runtime — without embedding connection details in each policy.
                    </p>
                </div>

                <div className="space-y-3 rounded-xl border-2 border-primary bg-primary/10 p-5">
                    <p className="text-xs font-semibold text-primary">How it works</p>
                    <div className="flex flex-wrap items-center justify-center gap-3">
                        <LearningNode icon={GlobeIcon} label="Client" />
                        <ArrowRightIcon className="size-4 text-muted-foreground" />
                        <div className="flex flex-col items-center rounded-lg border border-border bg-card px-3 py-2 text-center">
                            <div className="rounded-lg bg-primary/10 p-1.5">
                                <ServerIcon className="size-4 text-primary" />
                            </div>
                            <p className="mt-1 text-xs font-semibold">API Gateway</p>
                        </div>
                        <ArrowRightIcon className="size-4 text-muted-foreground" />
                        <div className="flex items-center gap-2 rounded-lg border border-border bg-card px-3 py-2">
                            <LearningNode icon={DatabaseIcon} label="Cache" />
                            <LearningNode icon={KeyRoundIcon} label="OAuth" />
                            <LearningNode icon={ShieldCheckIcon} label="Auth" />
                        </div>
                    </div>
                </div>

                <ul className="space-y-1.5 text-xs text-muted-foreground">
                    <li className="flex items-center gap-1.5">
                        <CircleCheckIcon className="size-3 shrink-0 text-success" />
                        Shared caching for sessions and responses across policies
                    </li>
                    <li className="flex items-center gap-1.5">
                        <CircleCheckIcon className="size-3 shrink-0 text-success" />
                        Centralized OAuth and auth provider configuration
                    </li>
                </ul>
            </CardContent>
        </Card>
    );
}

// ─── Stats cards ──────────────────────────────────────────────────────────────

function StatsCards({ total, apiLevel }: { total: number; apiLevel: number }) {
    return (
        <div className="grid grid-cols-3 gap-4">
            <Card>
                <CardContent className="pt-5 pb-4">
                    <p className="text-2xl font-semibold">{total}</p>
                    <p className="mt-0.5 text-sm text-muted-foreground">Total resources</p>
                    <p className="text-xs text-muted-foreground">configured for this API</p>
                </CardContent>
            </Card>
            <Card>
                <CardContent className="pt-5 pb-4">
                    <p className="flex items-center gap-2 text-2xl font-semibold">
                        {apiLevel}
                        <Badge variant="secondary" className="text-xs font-normal">
                            Local
                        </Badge>
                    </p>
                    <p className="mt-0.5 text-sm text-muted-foreground">API-level</p>
                    <p className="text-xs text-muted-foreground">created specifically for this API</p>
                </CardContent>
            </Card>
            {/* Environment references are not supported by the backend yet — locked until then. */}
            <Card className="opacity-60" aria-disabled>
                <CardContent className="pt-5 pb-4">
                    <p className="flex items-center gap-2 text-2xl font-semibold">
                        0
                        <Badge variant="outline" className="gap-1 text-xs font-normal">
                            <LockIcon className="size-3" aria-hidden />
                            Coming soon
                        </Badge>
                    </p>
                    <p className="mt-0.5 text-sm text-muted-foreground">Environment references</p>
                    <p className="text-xs text-muted-foreground">linked from environment-level resources</p>
                </CardContent>
            </Card>
        </div>
    );
}

// ─── Page ─────────────────────────────────────────────────────────────────────

export function ApiResourcesPage() {
    const { apiId } = useParams<{ apiId: string }>();
    const navigate = useNavigate();

    const canEditPermission = useHasPermission({ anyOf: ['api-definition-u'] });
    const { data: api, isLoading, isError } = useApiDetail(apiId);
    const { data: plugins = [] } = useResourcePlugins();
    const mutation = useUpdateApiResources(apiId);

    const resources = useMemo<ApiResource[]>(() => api?.resources ?? [], [api?.resources]);
    const isKubernetes = api?.definitionContext?.origin === 'KUBERNETES';
    const canModify = canEditPermission && !isKubernetes;

    const [search, setSearch] = useState('');
    const [page, setPage] = useState(1);
    const [pageSize, setPageSize] = useState(10);
    const [removeIndex, setRemoveIndex] = useState<number | null>(null);

    const pluginById = useMemo(() => new Map(plugins.map(p => [p.id, p])), [plugins]);

    const visible = useMemo(() => {
        const q = search.trim().toLowerCase();
        return resources
            .map((resource, index) => ({ resource, index }))
            .filter(({ resource }) => {
                if (!q) return true;
                const typeName = pluginById.get(resource.type)?.name ?? resource.type;
                return resource.name.toLowerCase().includes(q) || typeName.toLowerCase().includes(q);
            });
    }, [resources, search, pluginById]);

    const totalCount = visible.length;
    const pageStart = (page - 1) * pageSize;
    const paginated = visible.slice(pageStart, pageStart + pageSize);

    const handleSearchChange = (value: string) => {
        setSearch(value);
        setPage(1);
    };
    const handlePageSizeChange = (size: number) => {
        setPageSize(size);
        setPage(1);
    };

    const paginationBar = (
        <DataTablePagination
            page={page}
            pageSize={pageSize}
            totalCount={totalCount}
            pageSizeOptions={PAGE_SIZE_OPTIONS}
            onPageChange={setPage}
            onPageSizeChange={handlePageSizeChange}
        />
    );

    const persist = (next: ApiResource[], successMessage: string) => {
        mutation.mutate(next, {
            onSuccess: () => {
                notify.success(successMessage);
                setRemoveIndex(null);
            },
            onError: error => notify.error(error, 'Failed to save resources.'),
        });
    };

    const handleToggle = (index: number) => {
        const target = resources[index];
        const next = resources.map((r, i) => (i === index ? { ...r, enabled: !r.enabled } : r));
        persist(next, target.enabled ? 'Resource disabled.' : 'Resource enabled.');
    };

    const handleRemove = () => {
        if (removeIndex === null) return;
        persist(
            resources.filter((_, i) => i !== removeIndex),
            'Resource removed.',
        );
    };

    if (isLoading) {
        return (
            <div className="space-y-6">
                <div className="space-y-2">
                    <Skeleton className="h-8 w-48 rounded" />
                    <Skeleton className="h-4 w-72 rounded" />
                </div>
                <div className="grid grid-cols-3 gap-4">
                    {[1, 2, 3].map(i => (
                        <Skeleton key={i} className="h-24 rounded-xl" />
                    ))}
                </div>
                <Skeleton className="h-48 w-full rounded-xl" />
            </div>
        );
    }

    if (isError) {
        return (
            <div>
                <Card className="border-destructive/30">
                    <CardContent className="pt-4 pb-4">
                        <p className="text-sm text-destructive">Failed to load API resources. Please try again.</p>
                    </CardContent>
                </Card>
            </div>
        );
    }

    const removeTarget = removeIndex !== null ? resources[removeIndex] : null;

    return (
        <div className="space-y-6">
            <div className="flex items-start justify-between gap-4">
                <div className="space-y-1">
                    <h1 className="text-2xl font-semibold tracking-tight">Resources</h1>
                    <p className="text-sm text-muted-foreground">Create and manage resources your API policies can use at runtime.</p>
                </div>
                {canModify ? (
                    <Button size="sm" onClick={() => navigate('new')}>
                        <PlusIcon className="size-4" />
                        Add Resource
                    </Button>
                ) : null}
            </div>

            {isKubernetes ? (
                <Alert>
                    <AlertDescription>This API is managed by the Kubernetes operator. Resources are read-only.</AlertDescription>
                </Alert>
            ) : null}

            {resources.length === 0 ? (
                <ResourcesEmptyLanding />
            ) : (
                <>
                    <StatsCards total={resources.length} apiLevel={resources.length} />

                    <div className="relative">
                        <SearchIcon
                            className="absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground pointer-events-none"
                            aria-hidden
                        />
                        <Input
                            placeholder="Search resources…"
                            value={search}
                            onChange={e => handleSearchChange(e.target.value)}
                            className="pl-9"
                        />
                    </div>

                    <Card>
                        <CardContent className="p-0">
                            <Table>
                                <TableHeader>
                                    <TableRow>
                                        <TableHead style={{ width: '40%' }}>Resource</TableHead>
                                        <TableHead>Type</TableHead>
                                        <TableHead>Status</TableHead>
                                        <TableHead className="w-14 text-right">Actions</TableHead>
                                    </TableRow>
                                </TableHeader>
                                <TableBody>
                                    {paginated.map(({ resource, index }) => {
                                        const plugin = pluginById.get(resource.type);
                                        return (
                                            <TableRow key={resource.name}>
                                                <TableCell>
                                                    <div className="flex items-center gap-3">
                                                        <div className="flex size-9 shrink-0 items-center justify-center rounded-lg bg-muted">
                                                            <PluginIcon icon={plugin?.icon} className="size-5" />
                                                        </div>
                                                        <span className="font-medium">{resource.name}</span>
                                                    </div>
                                                </TableCell>
                                                <TableCell>{plugin?.name ?? resource.type}</TableCell>
                                                <TableCell>
                                                    {resource.enabled ? (
                                                        <Badge className="gap-1">
                                                            <CircleCheckIcon className="size-3" />
                                                            Enabled
                                                        </Badge>
                                                    ) : (
                                                        <Badge variant="outline" className="gap-1">
                                                            <CircleXIcon className="size-3" />
                                                            Disabled
                                                        </Badge>
                                                    )}
                                                </TableCell>
                                                <TableCell className="text-right" onClick={e => e.stopPropagation()}>
                                                    {canModify ? (
                                                        <DropdownMenu>
                                                            <DropdownMenuTrigger asChild>
                                                                <Button
                                                                    type="button"
                                                                    variant="ghost"
                                                                    size="icon"
                                                                    className="size-8"
                                                                    aria-label={`Actions for ${resource.name}`}
                                                                >
                                                                    <MoreHorizontalIcon className="size-4" />
                                                                </Button>
                                                            </DropdownMenuTrigger>
                                                            <DropdownMenuContent align="end">
                                                                <DropdownMenuItem
                                                                    onSelect={() => navigate(`${encodeURIComponent(resource.name)}/edit`)}
                                                                >
                                                                    <PencilIcon className="size-4" />
                                                                    Edit
                                                                </DropdownMenuItem>
                                                                <DropdownMenuItem
                                                                    onSelect={() => handleToggle(index)}
                                                                    disabled={mutation.isPending}
                                                                >
                                                                    {resource.enabled ? (
                                                                        <>
                                                                            <CircleXIcon className="size-4" />
                                                                            Disable
                                                                        </>
                                                                    ) : (
                                                                        <>
                                                                            <CircleCheckIcon className="size-4" />
                                                                            Enable
                                                                        </>
                                                                    )}
                                                                </DropdownMenuItem>
                                                                <DropdownMenuSeparator />
                                                                <DropdownMenuItem
                                                                    className="text-destructive focus:text-destructive"
                                                                    onSelect={() => setRemoveIndex(index)}
                                                                    disabled={mutation.isPending}
                                                                >
                                                                    <Trash2Icon className="size-4" />
                                                                    Remove
                                                                </DropdownMenuItem>
                                                            </DropdownMenuContent>
                                                        </DropdownMenu>
                                                    ) : null}
                                                </TableCell>
                                            </TableRow>
                                        );
                                    })}
                                    {paginated.length === 0 ? (
                                        <TableRow>
                                            <TableCell colSpan={4} className="py-10 text-center text-muted-foreground">
                                                No resources match your search.
                                            </TableCell>
                                        </TableRow>
                                    ) : null}
                                </TableBody>
                            </Table>
                        </CardContent>
                    </Card>

                    {paginationBar}
                </>
            )}

            <ConfirmDialog
                open={removeIndex !== null}
                onOpenChange={open => !open && setRemoveIndex(null)}
                title="Remove resource"
                description={
                    removeTarget ? `Remove "${removeTarget.name}"? Policies that reference it will fail until updated.` : undefined
                }
                confirmLabel="Remove"
                destructive
                isPending={mutation.isPending}
                icon={<Trash2Icon className="size-4" />}
                onConfirm={handleRemove}
            />
        </div>
    );
}
