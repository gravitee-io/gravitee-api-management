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
} from '@gravitee/graphene-core';
import { BoxesIcon, RefreshCwIcon } from '@gravitee/graphene-core/icons';
import type { ColumnDef } from '@tanstack/react-table';
import { useDeferredValue, useMemo, useState } from 'react';
import { KpiTile } from '../../components/KpiTile';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '../../components/Tabs';
import { formatEntityUid, fromBackend } from '../../shared/entity-adapter';
import { useEntities, type UseEntitiesResult } from '../../shared/hooks/useEntities';
import { CATEGORIES, getEntityCategoryId, type EntityInstance } from './entity-types';

type SourceFilter = 'all' | string;
type TypeFilter = 'all' | string;
type TabKey = 'principals' | 'resources';

const PAGE_SIZE_OPTIONS = [10, 25, 50, 100];
const ACTION_PREFIX = 'action.';

const PRINCIPALS_HELP =
    'Principals (users, groups, service accounts, agent identities) live in this environment. Edit and import flows are in a follow-up PR.';
const RESOURCES_HELP =
    'Resources are imported from the Context Catalog — MCP servers, tools, prompts, APIs, agents, and LLM models. Edit and import flows are in a follow-up PR.';

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
    return 'Local';
}

function categoryTextColorFor(entity: EntityInstance): string | undefined {
    const id = getEntityCategoryId(entity.uid.type);
    if (!id) return undefined;
    return CATEGORIES.find(c => c.id === id)?.textColor;
}

interface EntitiesTableProps {
    readonly tab: TabKey;
    readonly entities: readonly EntityInstance[];
    readonly searchValue: string;
    readonly isLoading: boolean;
    readonly page: number;
    readonly perPage: number;
    readonly totalCount: number;
    readonly onPageChange: (page: number) => void;
    readonly onPerPageChange: (perPage: number) => void;
}

function EntitiesTable({
    tab,
    entities,
    searchValue,
    isLoading,
    page,
    perPage,
    totalCount,
    onPageChange,
    onPerPageChange,
}: EntitiesTableProps) {
    const data = useMemo(() => [...entities], [entities]);

    const columns = useMemo<ColumnDef<EntityInstance>[]>(
        () => [
            {
                id: 'type',
                header: 'Type',
                cell: ({ row }) => {
                    const textColor = categoryTextColorFor(row.original);
                    return (
                        <Badge variant="outline" className={`font-mono text-[11px] ${textColor ?? ''}`}>
                            {row.original.uid.type}
                        </Badge>
                    );
                },
            },
            {
                id: 'entityId',
                header: 'Entity ID',
                cell: ({ row }) => (
                    <div className="font-mono text-xs">
                        <div className="text-muted-foreground">{row.original.uid.type}</div>
                        <div className="text-foreground">{row.original.uid.id}</div>
                    </div>
                ),
            },
            {
                id: 'name',
                header: 'Name',
                cell: ({ row }) => <span className="block truncate font-medium">{displayNameOf(row.original)}</span>,
            },
            {
                id: 'source',
                header: 'Source',
                cell: ({ row }) => (
                    <Badge variant="secondary" className="text-xs">
                        {sourceLabelOf(row.original)}
                    </Badge>
                ),
            },
        ],
        [],
    );

    if (!isLoading && data.length === 0) {
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
            <DataTable<EntityInstance> columns={columns} data={data} serverSide loading={isLoading} skeletonCount={perPage} />
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

function pageEntities(query: UseEntitiesResult): readonly EntityInstance[] {
    if (!query.data) return [];
    return query.data.data.map(fromBackend);
}

export function EntitiesPage() {
    const env = useEnvironment();
    const environmentId = env?.id ?? '';
    const principalsQuery = useEntities(environmentId, undefined, { kind: 'PRINCIPAL' });
    const resourcesQuery = useEntities(environmentId, undefined, { kind: 'RESOURCE', excludeEntityIdPrefix: ACTION_PREFIX });

    const [principalSearch, setPrincipalSearch] = useState('');
    const [principalTypeFilter, setPrincipalTypeFilter] = useState<TypeFilter>('all');
    const [principalSourceFilter, setPrincipalSourceFilter] = useState<SourceFilter>('all');
    const [resourceSearch, setResourceSearch] = useState('');
    const [resourceTypeFilter, setResourceTypeFilter] = useState<TypeFilter>('all');
    const [resourceSourceFilter, setResourceSourceFilter] = useState<SourceFilter>('all');

    const deferredPrincipalSearch = useDeferredValue(principalSearch);
    const deferredResourceSearch = useDeferredValue(resourceSearch);

    const principals = useMemo(() => pageEntities(principalsQuery), [principalsQuery]);
    const resources = useMemo(() => pageEntities(resourcesQuery), [resourcesQuery]);

    const principalTotal = principalsQuery.data?.total ?? 0;
    const resourceTotal = resourcesQuery.data?.total ?? 0;

    const principalTypes = useMemo(() => distinctSorted(principals.map(e => e.uid.type)), [principals]);
    const resourceTypes = useMemo(() => distinctSorted(resources.map(e => e.uid.type)), [resources]);
    const principalSources = useMemo(() => distinctSorted(principals.map(sourceLabelOf)), [principals]);
    const resourceSources = useMemo(() => distinctSorted(resources.map(sourceLabelOf)), [resources]);

    // TODO(authz-ui): server-side filters for search/type/source — these stay
    // client-side and operate on the currently visible page only.
    const filteredPrincipals = useMemo(
        () => applyFilters(principals, deferredPrincipalSearch, principalTypeFilter, principalSourceFilter),
        [principals, deferredPrincipalSearch, principalTypeFilter, principalSourceFilter],
    );
    const filteredResources = useMemo(
        () => applyFilters(resources, deferredResourceSearch, resourceTypeFilter, resourceSourceFilter),
        [resources, deferredResourceSearch, resourceTypeFilter, resourceSourceFilter],
    );

    const kpis = useMemo(() => {
        const visibleTypes = new Set<string>();
        principals.forEach(e => visibleTypes.add(e.uid.type));
        resources.forEach(e => visibleTypes.add(e.uid.type));
        return {
            total: principalTotal + resourceTotal,
            types: visibleTypes.size,
            principals: principalTotal,
            resources: resourceTotal,
        };
    }, [principals, resources, principalTotal, resourceTotal]);

    const isLoading = principalsQuery.isLoading || resourcesQuery.isLoading;
    const error = principalsQuery.error ?? resourcesQuery.error;

    const reloadAll = () => {
        principalsQuery.reload();
        resourcesQuery.reload();
    };

    return (
        <div className="flex flex-col gap-4">
            <header className="flex flex-wrap items-start justify-between gap-3">
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
                <div className="flex items-center gap-2">
                    <Button type="button" variant="ghost" size="icon" onClick={reloadAll} aria-label="Refresh">
                        <RefreshCwIcon aria-hidden className="size-4" />
                    </Button>
                </div>
            </header>

            <div className="grid grid-cols-2 gap-3 md:grid-cols-4" aria-label="Key metrics">
                <KpiTile label="Total entities" value={kpis.total} loading={isLoading} />
                <KpiTile label="Entity types" value={kpis.types} loading={isLoading} />
                <KpiTile label="Principals" value={kpis.principals} loading={isLoading} />
                <KpiTile label="Resources" value={kpis.resources} loading={isLoading} />
            </div>

            {error !== undefined && (
                <Alert variant="destructive">
                    <AlertTitle>Could not load entities</AlertTitle>
                    <AlertDescription>{error}</AlertDescription>
                </Alert>
            )}

            <Tabs defaultValue="principals" className="flex flex-col gap-3">
                <TabsList>
                    <TabsTrigger value="principals">
                        Principals
                        <Badge variant="secondary" className="ml-2 h-4 px-1 text-[10px]">
                            {principalTotal}
                        </Badge>
                    </TabsTrigger>
                    <TabsTrigger value="resources">
                        Resources
                        <Badge variant="secondary" className="ml-2 h-4 px-1 text-[10px]">
                            {resourceTotal}
                        </Badge>
                    </TabsTrigger>
                </TabsList>

                <TabsContent value="principals" className="flex flex-col gap-3">
                    <Alert>
                        <AlertDescription>{PRINCIPALS_HELP}</AlertDescription>
                    </Alert>
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
                    <EntitiesTable
                        tab="principals"
                        entities={filteredPrincipals}
                        searchValue={deferredPrincipalSearch}
                        isLoading={principalsQuery.isLoading}
                        page={principalsQuery.page}
                        perPage={principalsQuery.perPage}
                        totalCount={principalTotal}
                        onPageChange={principalsQuery.setPage}
                        onPerPageChange={principalsQuery.setPerPage}
                    />
                </TabsContent>

                <TabsContent value="resources" className="flex flex-col gap-3">
                    <Alert>
                        <AlertDescription>{RESOURCES_HELP}</AlertDescription>
                    </Alert>
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
                    <EntitiesTable
                        tab="resources"
                        entities={filteredResources}
                        searchValue={deferredResourceSearch}
                        isLoading={resourcesQuery.isLoading}
                        page={resourcesQuery.page}
                        perPage={resourcesQuery.perPage}
                        totalCount={resourceTotal}
                        onPageChange={resourcesQuery.setPage}
                        onPerPageChange={resourcesQuery.setPerPage}
                    />
                </TabsContent>
            </Tabs>
        </div>
    );
}
