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
import { useEnvironment } from '@gravitee/gamma-modules-sdk';
import {
    Alert,
    AlertDescription,
    AlertTitle,
    Badge,
    Button,
    DataTable,
    DataTablePagination,
    Dialog,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuTrigger,
    Empty,
    EmptyDescription,
    EmptyHeader,
    EmptyTitle,
    Input,
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
    Tabs,
    TabsContent,
    TabsList,
    TabsTrigger,
    toast,
} from '@gravitee/graphene-core';
import {
    BoxesIcon,
    CopyIcon,
    DownloadIcon,
    PencilIcon,
    PlusIcon,
    RefreshCwIcon,
    SettingsIcon,
    ShieldIcon,
    Trash2Icon,
    UsersIcon,
} from '@gravitee/graphene-core/icons';
import { useQueryClient } from '@tanstack/react-query';
import type { ColumnDef } from '@tanstack/react-table';
import { useDeferredValue, useEffect, useMemo, useRef, useState } from 'react';
import { KpiTile } from '../../components/KpiTile';
import { authzApiService, DEFAULT_PER_PAGE, MAX_PER_PAGE } from '../../shared/api/authz-api.service';
import type { PolicyResponse } from '../../shared/api/authz-api.types';
import { authzQueryKeys } from '../../shared/api/query-keys';
import { buildEntitiesJson } from '../../shared/entities-json';
import { formatEntityUid, fromBackend } from '../../shared/entity-adapter';
import { childrenByType, policiesFor } from '../../shared/entity-relationships';
import { useAllEntities } from '../../shared/hooks/useAllEntities';
import { usePolicies } from '../../shared/hooks/usePolicies';
import { useUserSync } from '../../shared/hooks/useUserSync';
import { CreateEntityDialog } from './CreateEntityDialog';
import { EditEntityDialog } from './EditEntityDialog';
import { ImportFromCatalogDialog } from './ImportFromCatalogDialog';
import { EntityDetailSheet } from './entity-detail/EntityDetailSheet';
import { CATEGORIES, getEntityCategoryId, type EntityInstance } from './entity-types';

type AddingKind = 'PRINCIPAL' | 'RESOURCE';

type SourceFilter = 'all' | string;
type TypeFilter = 'all' | string;
type TabKey = 'principals' | 'resources';

const PAGE_SIZE_OPTIONS = [10, 25, 50, 100];
const ACTION_PREFIX = 'action.';

const PRINCIPALS_HELP =
    'Principals (users, groups, service accounts, agent identities) live in this environment. Local principals can be edited or removed; synced ones are read-only.';
const RESOURCES_HELP =
    'Resources are imported from the Context Catalog — MCP servers, AI models, and agents keep the same Entity ID in Authorization. Catalog-sourced entries are read-only; you can remove imported instances but cannot edit them here.';

function displayNameOf(entity: EntityInstance): string {
    if (entity.displayName) return entity.displayName;
    const name = entity.attrs.name;
    if (typeof name === 'string' && name) return name;
    const displayName = entity.attrs.displayName;
    if (typeof displayName === 'string' && displayName) return displayName;
    return entity.uid.id;
}

function sourceLabelOf(entity: EntityInstance): string {
    if (entity.source === 'apim') return 'APIM';
    if (entity.source === 'gravitee-catalog') return 'Gravitee Catalog';
    if (entity.source === 'gravitee_am') return 'AM';
    return 'Local';
}

function categoryTextColorFor(entity: EntityInstance): string | undefined {
    const id = getEntityCategoryId(entity.uid.type);
    if (!id) return undefined;
    return CATEGORIES.find(c => c.id === id)?.textColor;
}

interface EntitiesTableProps {
    readonly tab: TabKey;
    readonly entities: EntityInstance[];
    readonly allEntities: readonly EntityInstance[];
    readonly allPolicies: readonly PolicyResponse[];
    readonly searchValue: string;
    readonly isLoading: boolean;
    readonly page: number;
    readonly perPage: number;
    readonly totalCount: number;
    readonly onPageChange: (page: number) => void;
    readonly onPerPageChange: (perPage: number) => void;
    readonly onView?: (entity: EntityInstance) => void;
    readonly onEdit?: (entity: EntityInstance) => void;
    readonly onDelete?: (entity: EntityInstance) => void;
    readonly canDelete?: (entity: EntityInstance) => boolean;
    readonly deletingEntityId?: string;
}

function copyToClipboard(text: string) {
    navigator.clipboard?.writeText(text)?.catch(() => undefined);
}

function EntitiesTable({
    tab,
    entities,
    allEntities,
    allPolicies,
    searchValue,
    isLoading,
    page,
    perPage,
    totalCount,
    onPageChange,
    onPerPageChange,
    onView,
    onEdit,
    onDelete,
    canDelete,
    deletingEntityId,
}: EntitiesTableProps) {
    const columns = useMemo<ColumnDef<EntityInstance>[]>(() => {
        const baseColumns: ColumnDef<EntityInstance>[] = [
            {
                id: 'type',
                header: 'Type',
                size: 180,
                cell: ({ row }) => {
                    const textColor = categoryTextColorFor(row.original);
                    return (
                        <Badge variant="outline" className={`font-mono ${textColor ?? ''}`}>
                            {row.original.uid.type}
                        </Badge>
                    );
                },
            },
            {
                id: 'entityId',
                header: 'Entity ID',
                size: 320,
                cell: ({ row }) => {
                    const uid = formatEntityUid(row.original.uid);
                    return (
                        <span className="flex items-center gap-1.5">
                            <span className="font-mono text-xs text-foreground">{uid}</span>
                            <Button
                                variant="ghost"
                                size="sm"
                                className="size-6 shrink-0 p-0"
                                aria-label={`Copy ${uid}`}
                                title="Copy Entity ID"
                                onClick={() => copyToClipboard(uid)}
                            >
                                <CopyIcon className="size-3.5 text-muted-foreground" aria-hidden />
                            </Button>
                        </span>
                    );
                },
            },
            {
                id: 'name',
                header: 'Name',
                size: 220,
                cell: ({ row }) =>
                    onView ? (
                        <button
                            type="button"
                            className="block truncate text-left font-medium hover:underline"
                            onClick={() => onView(row.original)}
                        >
                            {displayNameOf(row.original)}
                        </button>
                    ) : (
                        <span className="block truncate font-medium">{displayNameOf(row.original)}</span>
                    ),
            },
            {
                id: 'relationships',
                header: 'Relationships',
                size: 220,
                cell: ({ row }) => {
                    const groups = childrenByType(row.original, allEntities);
                    const parentCount = row.original.parents.length;
                    if (groups.length === 0 && parentCount === 0) {
                        return <span className="text-muted-foreground">—</span>;
                    }
                    return (
                        <div className="flex flex-wrap gap-1">
                            {parentCount > 0 && (
                                <Badge variant="outline" className="text-xs">
                                    in {parentCount}
                                </Badge>
                            )}
                            {groups.map(group => (
                                <Badge key={group.type} variant="secondary" className="text-xs">
                                    contains {group.count} {group.type}
                                </Badge>
                            ))}
                        </div>
                    );
                },
            },
            {
                id: 'policies',
                header: 'Policies',
                size: 100,
                cell: ({ row }) => {
                    const count = policiesFor(row.original, allPolicies).length;
                    return count === 0 ? <span className="text-muted-foreground">—</span> : <Badge variant="secondary">{count}</Badge>;
                },
            },
            {
                id: 'source',
                header: 'Source',
                size: 140,
                cell: ({ row }) => <Badge variant="secondary">{sourceLabelOf(row.original)}</Badge>,
            },
        ];
        if (onEdit || onDelete) {
            baseColumns.push({
                id: 'actions',
                header: '',
                size: 96,
                cell: ({ row }) => {
                    const uid = formatEntityUid(row.original.uid);
                    const isDeleting = deletingEntityId === uid;
                    // Only local entities are editable; catalog/APIM-sourced are read-only.
                    const canEdit = onEdit && row.original.source === 'local';
                    const showDelete = onDelete && (!canDelete || canDelete(row.original));
                    return (
                        <div className="flex items-center justify-end gap-1">
                            {canEdit && (
                                <Button
                                    variant="ghost"
                                    size="sm"
                                    onClick={() => onEdit(row.original)}
                                    aria-label={`Edit ${uid}`}
                                    title="Edit"
                                >
                                    <PencilIcon className="size-4 text-muted-foreground" aria-hidden />
                                </Button>
                            )}
                            {showDelete && (
                                <Button
                                    variant="ghost"
                                    size="sm"
                                    onClick={() => onDelete(row.original)}
                                    disabled={isDeleting}
                                    aria-label={`Delete ${uid}`}
                                    title="Remove from Authorization"
                                >
                                    <Trash2Icon className="size-4 text-muted-foreground" aria-hidden />
                                </Button>
                            )}
                        </div>
                    );
                },
            });
        }
        return baseColumns;
    }, [onView, onEdit, onDelete, canDelete, deletingEntityId, allEntities, allPolicies]);

    if (!isLoading && entities.length === 0) {
        return (
            <Empty>
                <EmptyHeader>
                    <EmptyTitle>{tab === 'principals' ? 'No principals yet' : 'No resources yet'}</EmptyTitle>
                    <EmptyDescription>
                        {searchValue
                            ? 'No matches for the current filters.'
                            : tab === 'principals'
                              ? 'Once principals are added or imported, they will appear here.'
                              : 'Once resources are imported from the Context Catalog, they will appear here.'}
                    </EmptyDescription>
                </EmptyHeader>
            </Empty>
        );
    }

    return (
        <>
            <DataTable<EntityInstance>
                columns={columns}
                data={entities}
                serverSide
                enableColumnResizing
                loading={isLoading}
                skeletonCount={perPage}
            />
            <DataTablePagination
                page={page}
                pageSize={perPage}
                totalCount={totalCount}
                pageSizeOptions={PAGE_SIZE_OPTIONS}
                onPageChange={onPageChange}
                onPageSizeChange={onPerPageChange}
            />
        </>
    );
}

interface FilterBarProps {
    readonly search: string;
    readonly onSearch: (value: string) => void;
    readonly typesPresent: readonly string[];
    readonly typeFilter: TypeFilter;
    readonly onTypeFilter: (value: TypeFilter) => void;
    readonly sourcesPresent: readonly string[];
    readonly sourceFilter: SourceFilter;
    readonly onSourceFilter: (value: SourceFilter) => void;
    readonly searchLabel: string;
}

function EntitiesFilterBar({
    search,
    onSearch,
    typesPresent,
    typeFilter,
    onTypeFilter,
    sourcesPresent,
    sourceFilter,
    onSourceFilter,
    searchLabel,
}: FilterBarProps) {
    return (
        <div className="flex flex-wrap items-center gap-2">
            <Input
                value={search}
                onChange={e => onSearch(e.target.value)}
                placeholder="Search by name, Entity ID, or type…"
                className="max-w-sm"
                aria-label={searchLabel}
            />
            <Select value={typeFilter} onValueChange={onTypeFilter}>
                <SelectTrigger className="w-44" aria-label="Filter by type">
                    <SelectValue placeholder="All types" />
                </SelectTrigger>
                <SelectContent>
                    <SelectItem value="all">All types</SelectItem>
                    {typesPresent.map(t => (
                        <SelectItem key={t} value={t}>
                            {t}
                        </SelectItem>
                    ))}
                </SelectContent>
            </Select>
            <Select value={sourceFilter} onValueChange={onSourceFilter}>
                <SelectTrigger className="w-44" aria-label="Filter by source">
                    <SelectValue placeholder="All sources" />
                </SelectTrigger>
                <SelectContent>
                    <SelectItem value="all">All sources</SelectItem>
                    {sourcesPresent.map(s => (
                        <SelectItem key={s} value={s}>
                            {s}
                        </SelectItem>
                    ))}
                </SelectContent>
            </Select>
        </div>
    );
}

function applyFilters(entities: readonly EntityInstance[], search: string, typeFilter: TypeFilter, sourceFilter: SourceFilter) {
    const needle = search.trim().toLowerCase();
    return entities.filter(e => {
        if (typeFilter !== 'all' && e.uid.type !== typeFilter) return false;
        if (sourceFilter !== 'all' && sourceLabelOf(e) !== sourceFilter) return false;
        if (!needle) return true;
        if (e.uid.id.toLowerCase().includes(needle)) return true;
        if (formatEntityUid(e.uid).toLowerCase().includes(needle)) return true;
        if (e.uid.type.toLowerCase().includes(needle)) return true;
        return displayNameOf(e).toLowerCase().includes(needle);
    });
}

function distinctSorted<T extends string>(values: Iterable<T>): T[] {
    return Array.from(new Set(values)).sort();
}

function paginate<T>(items: readonly T[], page: number, perPage: number): T[] {
    const start = (page - 1) * perPage;
    return items.slice(start, start + perPage);
}

export function EntitiesPage() {
    const env = useEnvironment();
    const environmentId = env?.id ?? '';
    const queryClient = useQueryClient();

    const sync = useUserSync(environmentId);
    const isSyncing = sync.isStarting || sync.status?.status === 'PENDING';

    // While a sync runs, poll the principals list so AM users appear as they're upserted
    // (the backend writes them in batches), rather than only when the sync completes.
    const principalsQuery = useAllEntities(environmentId, { kind: 'PRINCIPAL' }, { refetchInterval: isSyncing ? 2500 : false });
    const resourcesQuery = useAllEntities(environmentId, { kind: 'RESOURCE', excludeEntityIdPrefix: ACTION_PREFIX });
    const policiesQuery = usePolicies(environmentId, { initialPerPage: MAX_PER_PAGE });

    // Reload the principals list once a sync finishes (PENDING → SUCCESS), so the freshly
    // upserted AM users appear without a manual refresh. Fires only on the transition, so a
    // page that loads with an already-completed sync doesn't trigger a spurious refetch.
    const prevSyncStatus = useRef<string | undefined>(undefined);
    useEffect(() => {
        const current = sync.status?.status;
        if (current === 'SUCCESS' && prevSyncStatus.current === 'PENDING') {
            void queryClient.invalidateQueries({ queryKey: authzQueryKeys.entities.all(environmentId) });
            toast.success(`Sync finished, synced ${sync.status?.entitiesUpserted ?? 0} entities`);
        }
        prevSyncStatus.current = current;
    }, [sync.status?.status, sync.status?.entitiesUpserted, environmentId, queryClient]);

    const onSync = () => {
        // A 409 (sync already running) rejects the mutation; the hook suppresses it and the
        // status reflects the in-flight job, so swallow the rejection here.
        sync.start()
            .then(() => toast.info('Syncing entities from Gravitee Access Management…'))
            .catch(() => {});
    };

    const [principalSearch, setPrincipalSearch] = useState('');
    const [principalTypeFilter, setPrincipalTypeFilter] = useState<TypeFilter>('all');
    const [principalSourceFilter, setPrincipalSourceFilter] = useState<SourceFilter>('all');
    const [principalPage, setPrincipalPage] = useState(1);
    const [principalPerPage, setPrincipalPerPage] = useState(DEFAULT_PER_PAGE);
    const [resourceSearch, setResourceSearch] = useState('');
    const [resourceTypeFilter, setResourceTypeFilter] = useState<TypeFilter>('all');
    const [resourceSourceFilter, setResourceSourceFilter] = useState<SourceFilter>('all');
    const [resourcePage, setResourcePage] = useState(1);
    const [resourcePerPage, setResourcePerPage] = useState(DEFAULT_PER_PAGE);
    const [importOpen, setImportOpen] = useState(false);
    const [addingKind, setAddingKind] = useState<AddingKind | null>(null);
    const [editing, setEditing] = useState<{ entity: EntityInstance; kind: AddingKind } | null>(null);
    const [pendingDelete, setPendingDelete] = useState<EntityInstance | null>(null);
    const [deletingEntityId, setDeletingEntityId] = useState<string | undefined>();
    const [viewing, setViewing] = useState<EntityInstance | null>(null);

    const pendingDeleteUid = pendingDelete ? formatEntityUid(pendingDelete.uid) : '';
    const pendingDeleteName = pendingDelete ? displayNameOf(pendingDelete) : '';
    const pendingDeleteIsLocal = pendingDelete?.source === 'local';

    async function confirmDelete() {
        if (!pendingDelete) return;
        const uid = formatEntityUid(pendingDelete.uid);
        const friendly = displayNameOf(pendingDelete);
        setDeletingEntityId(uid);
        try {
            await authzApiService.deleteEntity(environmentId, uid);
            toast.success(`Removed ${friendly}`);
            setDeletingEntityId(undefined);
            setPendingDelete(null);
            void queryClient.invalidateQueries({ queryKey: authzQueryKeys.entities.all(environmentId) });
            void queryClient.invalidateQueries({ queryKey: authzQueryKeys.importedCatalogIds(environmentId) });
        } catch (err) {
            toast.error(`Failed to remove: ${err instanceof Error ? err.message : String(err)}`);
            setDeletingEntityId(undefined);
        }
    }

    function handleImported() {
        void queryClient.invalidateQueries({ queryKey: authzQueryKeys.entities.all(environmentId) });
    }

    function openEntitiesJson() {
        const url = URL.createObjectURL?.(new Blob([buildEntitiesJson(allEntities)], { type: 'application/json' }));
        if (!url) return;
        window.open?.(url, '_blank', 'noopener');
        setTimeout(() => URL.revokeObjectURL(url), 10_000);
    }

    const deferredPrincipalSearch = useDeferredValue(principalSearch);
    const deferredResourceSearch = useDeferredValue(resourceSearch);

    // Full sets (every page) so search/filter/paginate operate over all entities,
    // not just one server page.
    const principals = useMemo(() => principalsQuery.data.map(fromBackend), [principalsQuery.data]);
    const resources = useMemo(() => resourcesQuery.data.map(fromBackend), [resourcesQuery.data]);
    const allEntities = useMemo(() => [...principals, ...resources], [principals, resources]);
    const allPolicies = useMemo(() => policiesQuery.data?.data ?? [], [policiesQuery.data]);
    const policyLinkedCount = useMemo(
        () => allEntities.filter(e => policiesFor(e, allPolicies).length > 0).length,
        [allEntities, allPolicies],
    );

    const principalTotal = principalsQuery.total;
    const resourceTotal = resourcesQuery.total;

    const principalTypes = useMemo(() => distinctSorted(principals.map(e => e.uid.type)), [principals]);
    const resourceTypes = useMemo(() => distinctSorted(resources.map(e => e.uid.type)), [resources]);
    const principalSources = useMemo(() => distinctSorted(principals.map(sourceLabelOf)), [principals]);
    const resourceSources = useMemo(() => distinctSorted(resources.map(sourceLabelOf)), [resources]);

    const filteredPrincipals = useMemo(
        () => applyFilters(principals, deferredPrincipalSearch, principalTypeFilter, principalSourceFilter),
        [principals, deferredPrincipalSearch, principalTypeFilter, principalSourceFilter],
    );
    const filteredResources = useMemo(
        () => applyFilters(resources, deferredResourceSearch, resourceTypeFilter, resourceSourceFilter),
        [resources, deferredResourceSearch, resourceTypeFilter, resourceSourceFilter],
    );

    // Reset to page 1 whenever the filtered result set changes shape, so we never
    // sit on a now-empty page after filtering, resizing, or deleting.
    useEffect(() => {
        setPrincipalPage(1);
    }, [deferredPrincipalSearch, principalTypeFilter, principalSourceFilter, principalPerPage]);
    useEffect(() => {
        setResourcePage(1);
    }, [deferredResourceSearch, resourceTypeFilter, resourceSourceFilter, resourcePerPage]);

    const principalPageCount = Math.max(1, Math.ceil(filteredPrincipals.length / principalPerPage));
    const resourcePageCount = Math.max(1, Math.ceil(filteredResources.length / resourcePerPage));
    const safePrincipalPage = Math.min(principalPage, principalPageCount);
    const safeResourcePage = Math.min(resourcePage, resourcePageCount);
    const pagedPrincipals = useMemo(
        () => paginate(filteredPrincipals, safePrincipalPage, principalPerPage),
        [filteredPrincipals, safePrincipalPage, principalPerPage],
    );
    const pagedResources = useMemo(
        () => paginate(filteredResources, safeResourcePage, resourcePerPage),
        [filteredResources, safeResourcePage, resourcePerPage],
    );

    const kpis = useMemo(() => {
        const types = new Set<string>();
        principals.forEach(e => types.add(e.uid.type));
        resources.forEach(e => types.add(e.uid.type));
        return {
            total: principalTotal + resourceTotal,
            types: types.size,
            principals: principalTotal,
            resources: resourceTotal,
        };
    }, [principals, resources, principalTotal, resourceTotal]);

    const isLoading = principalsQuery.isLoading || resourcesQuery.isLoading;
    const error = principalsQuery.error ?? resourcesQuery.error;

    return (
        <div className="flex flex-col gap-4">
            <header className="flex items-start justify-between gap-3">
                <div className="flex items-start gap-3">
                    <BoxesIcon className="mt-1 size-5 text-muted-foreground" aria-hidden />
                    <div>
                        <h1 className="text-xl font-semibold">Entities</h1>
                        <p className="mt-1 max-w-3xl text-sm text-muted-foreground">
                            Principals and resources the policy engine evaluates. Resource entities are imported from the Context Catalog so
                            every policy refers to the same canonical Entity ID.
                        </p>
                    </div>
                </div>
                <DropdownMenu>
                    <DropdownMenuTrigger asChild>
                        <Button variant="ghost" size="sm" aria-label="Entities settings">
                            <SettingsIcon className="size-4" aria-hidden />
                        </Button>
                    </DropdownMenuTrigger>
                    <DropdownMenuContent align="end">
                        <DropdownMenuItem onClick={openEntitiesJson}>
                            <BoxesIcon className="mr-2 size-4" aria-hidden />
                            Open entities.json
                        </DropdownMenuItem>
                    </DropdownMenuContent>
                </DropdownMenu>
            </header>

            <div className="grid grid-cols-2 gap-3 md:grid-cols-5" aria-label="Key metrics">
                <KpiTile label="Total entities" value={kpis.total} loading={isLoading} />
                <KpiTile label="Types" value={kpis.types} loading={isLoading} />
                <KpiTile label="Principals" value={kpis.principals} loading={isLoading} />
                <KpiTile label="Resources" value={kpis.resources} loading={isLoading} />
                <KpiTile label="Policy-Linked" value={policyLinkedCount} loading={isLoading} />
            </div>

            {error !== undefined && (
                <Alert variant="destructive">
                    <AlertTitle>Could not load entities</AlertTitle>
                    <AlertDescription>{error}</AlertDescription>
                </Alert>
            )}

            <Tabs defaultValue="principals" className="flex flex-col gap-3">
                <TabsList variant="line">
                    <TabsTrigger value="principals">
                        <UsersIcon className="size-4" aria-hidden />
                        Principals
                        <Badge variant="secondary">{principalTotal}</Badge>
                    </TabsTrigger>
                    <TabsTrigger value="resources">
                        <ShieldIcon className="size-4" aria-hidden />
                        Resources
                        <Badge variant="secondary">{resourceTotal}</Badge>
                    </TabsTrigger>
                </TabsList>

                <TabsContent value="principals" className="flex flex-col gap-3">
                    <Alert>
                        <AlertDescription>{PRINCIPALS_HELP}</AlertDescription>
                    </Alert>
                    <div className="flex flex-wrap items-center gap-2">
                        <EntitiesFilterBar
                            search={principalSearch}
                            onSearch={setPrincipalSearch}
                            typesPresent={principalTypes}
                            typeFilter={principalTypeFilter}
                            onTypeFilter={setPrincipalTypeFilter}
                            sourcesPresent={principalSources}
                            sourceFilter={principalSourceFilter}
                            onSourceFilter={setPrincipalSourceFilter}
                            searchLabel="Search principals"
                        />
                        <div className="ml-auto flex items-center gap-2">
                            <Button variant="outline" onClick={onSync} disabled={isSyncing}>
                                <RefreshCwIcon className={`mr-2 size-4 ${isSyncing ? 'animate-spin' : ''}`} aria-hidden />
                                {isSyncing ? 'Syncing from Gravitee Access Management…' : 'Sync From Gravitee Access Management'}
                            </Button>
                            <Button onClick={() => setAddingKind('PRINCIPAL')}>
                                <PlusIcon className="mr-2 size-4" aria-hidden />
                                Add principal
                            </Button>
                        </div>
                        {sync.startError ? (
                            <Alert variant="destructive" className="w-full">
                                <AlertTitle>Could not start AM sync</AlertTitle>
                                <AlertDescription>{sync.startError}</AlertDescription>
                            </Alert>
                        ) : null}
                        {sync.statusError ? (
                            <Alert variant="destructive" className="w-full">
                                <AlertTitle>Could not load AM sync status</AlertTitle>
                                <AlertDescription>{sync.statusError}</AlertDescription>
                            </Alert>
                        ) : null}
                    </div>
                    <EntitiesTable
                        tab="principals"
                        entities={pagedPrincipals}
                        allEntities={allEntities}
                        allPolicies={allPolicies}
                        searchValue={deferredPrincipalSearch}
                        isLoading={principalsQuery.isLoading}
                        page={safePrincipalPage}
                        perPage={principalPerPage}
                        totalCount={filteredPrincipals.length}
                        onPageChange={setPrincipalPage}
                        onPerPageChange={setPrincipalPerPage}
                        onView={setViewing}
                        onEdit={entity => setEditing({ entity, kind: 'PRINCIPAL' })}
                        onDelete={setPendingDelete}
                        canDelete={entity => entity.source === 'local'}
                        deletingEntityId={deletingEntityId}
                    />
                </TabsContent>

                <TabsContent value="resources" className="flex flex-col gap-3">
                    <Alert>
                        <AlertDescription>{RESOURCES_HELP}</AlertDescription>
                    </Alert>
                    <div className="flex flex-wrap items-center gap-2">
                        <EntitiesFilterBar
                            search={resourceSearch}
                            onSearch={setResourceSearch}
                            typesPresent={resourceTypes}
                            typeFilter={resourceTypeFilter}
                            onTypeFilter={setResourceTypeFilter}
                            sourcesPresent={resourceSources}
                            sourceFilter={resourceSourceFilter}
                            onSourceFilter={setResourceSourceFilter}
                            searchLabel="Search resources"
                        />
                        <div className="ml-auto flex items-center gap-2">
                            <Button variant="outline" onClick={() => setAddingKind('RESOURCE')}>
                                <PlusIcon className="mr-2 size-4" aria-hidden />
                                Add resource
                            </Button>
                            <Button onClick={() => setImportOpen(true)}>
                                <DownloadIcon className="mr-2 size-4" aria-hidden />
                                Import from AI Catalog
                            </Button>
                        </div>
                    </div>
                    <EntitiesTable
                        tab="resources"
                        entities={pagedResources}
                        allEntities={allEntities}
                        allPolicies={allPolicies}
                        searchValue={deferredResourceSearch}
                        isLoading={resourcesQuery.isLoading}
                        page={safeResourcePage}
                        perPage={resourcePerPage}
                        totalCount={filteredResources.length}
                        onPageChange={setResourcePage}
                        onPerPageChange={setResourcePerPage}
                        onView={setViewing}
                        onEdit={entity => setEditing({ entity, kind: 'RESOURCE' })}
                        onDelete={setPendingDelete}
                        deletingEntityId={deletingEntityId}
                    />
                </TabsContent>
            </Tabs>

            <ImportFromCatalogDialog
                open={importOpen}
                environmentId={environmentId}
                onOpenChange={setImportOpen}
                onImported={handleImported}
            />

            <CreateEntityDialog
                open={addingKind !== null}
                kind={addingKind ?? 'PRINCIPAL'}
                environmentId={environmentId}
                onOpenChange={open => {
                    if (!open) setAddingKind(null);
                }}
                onCreated={handleImported}
            />

            <EditEntityDialog
                open={editing !== null}
                entity={editing?.entity ?? null}
                kind={editing?.kind ?? 'PRINCIPAL'}
                environmentId={environmentId}
                onOpenChange={open => {
                    if (!open) setEditing(null);
                }}
                onUpdated={handleImported}
            />

            <EntityDetailSheet
                entity={viewing}
                allEntities={allEntities}
                policies={allPolicies}
                open={viewing !== null}
                onOpenChange={open => {
                    if (!open) setViewing(null);
                }}
                onEdit={entity => {
                    setViewing(null);
                    setEditing({ entity, kind: principals.includes(entity) ? 'PRINCIPAL' : 'RESOURCE' });
                }}
            />

            <Dialog
                open={pendingDelete !== null}
                onOpenChange={open => {
                    if (!open && !deletingEntityId) setPendingDelete(null);
                }}
            >
                <DialogContent>
                    <DialogHeader>
                        <DialogTitle>Remove entity from Authorization?</DialogTitle>
                        <DialogDescription>
                            {!pendingDelete
                                ? ''
                                : pendingDeleteIsLocal
                                  ? `"${pendingDeleteName}" (${pendingDeleteUid}) will be permanently removed from Authorization. This can't be undone.`
                                  : `"${pendingDeleteName}" (${pendingDeleteUid}) will be removed from Authorization. This won't delete it from the Context Catalog — you can re-import it later.`}
                        </DialogDescription>
                    </DialogHeader>
                    <DialogFooter>
                        <Button
                            type="button"
                            variant="outline"
                            onClick={() => setPendingDelete(null)}
                            disabled={deletingEntityId !== undefined}
                        >
                            Cancel
                        </Button>
                        <Button
                            type="button"
                            variant="destructive"
                            onClick={confirmDelete}
                            disabled={deletingEntityId !== undefined}
                            aria-label={pendingDelete ? `Confirm remove ${pendingDeleteName}` : 'Confirm remove'}
                        >
                            {deletingEntityId !== undefined ? 'Removing…' : 'Remove'}
                        </Button>
                    </DialogFooter>
                </DialogContent>
            </Dialog>
        </div>
    );
}
