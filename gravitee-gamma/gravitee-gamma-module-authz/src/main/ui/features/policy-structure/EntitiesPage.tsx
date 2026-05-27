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
import { BoxesIcon, ShieldIcon, UsersIcon } from '@gravitee/graphene-core/icons';
import type { ColumnDef } from '@tanstack/react-table';
import { useDeferredValue, useMemo, useState } from 'react';
import { KpiTile } from '../../components/KpiTile';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '../../components/Tabs';
import { formatEntityUid, fromBackend } from '../../shared/entity-adapter';
import { useEntities } from '../../shared/hooks/useEntities';
import type { EntityInstance } from './entity-types';

type SourceFilter = 'all' | string;
type TypeFilter = 'all' | string;
type TabKey = 'principals' | 'resources';
type EntityKind = 'PRINCIPAL' | 'RESOURCE';

const PAGE_SIZE_OPTIONS = [10, 25, 50, 100];
const ACTION_PREFIX = 'action.';

const PRINCIPALS_HELP =
    'Principals (users, groups, service accounts, agent identities) live in this environment. Edit and import flows are in a follow-up PR.';
const RESOURCES_HELP =
    'Resources are imported from the Context Catalog — MCP servers, tools, prompts, APIs, agents, and LLM models. Edit and import flows are in a follow-up PR.';

function displayNameOf(entity: EntityInstance): string {
    if (entity.displayName) return entity.displayName;
    const attrName = entity.attrs.name;
    if (typeof attrName === 'string' && attrName) return attrName;
    const attrDisplayName = entity.attrs.displayName;
    if (typeof attrDisplayName === 'string' && attrDisplayName) return attrDisplayName;
    return entity.uid.id;
}

function sourceLabelOf(entity: EntityInstance): string {
    return entity.source === 'apim' ? 'APIM' : 'Local';
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

interface UseEntitiesTabOptions {
    readonly environmentId: string;
    readonly kind: EntityKind;
    readonly excludeEntityIdPrefix?: string;
}

function useEntitiesTab({ environmentId, kind, excludeEntityIdPrefix }: UseEntitiesTabOptions) {
    const query = useEntities(environmentId, undefined, { kind, excludeEntityIdPrefix });
    const [search, setSearch] = useState('');
    const [typeFilter, setTypeFilter] = useState<TypeFilter>('all');
    const [sourceFilter, setSourceFilter] = useState<SourceFilter>('all');
    const deferredSearch = useDeferredValue(search);

    // TODO(authz-ui): server-side filters for search/type/source — these stay
    // client-side and operate on the currently visible page only. The warning
    // banner below alerts users when a filtered view may be hiding matches.
    const entities = useMemo(() => (query.data?.data ?? []).map(fromBackend), [query.data]);
    const types = useMemo(() => distinctSorted(entities.map(e => e.uid.type)), [entities]);
    const sources = useMemo(() => distinctSorted(entities.map(sourceLabelOf)), [entities]);
    const filtered = useMemo(
        () => applyFilters(entities, deferredSearch, typeFilter, sourceFilter),
        [entities, deferredSearch, typeFilter, sourceFilter],
    );

    const total = query.data?.total ?? 0;
    const isFiltering = deferredSearch.trim() !== '' || typeFilter !== 'all' || sourceFilter !== 'all';
    const hasMoreThanCurrentPage = total > entities.length;

    return {
        entities,
        filtered,
        total,
        types,
        sources,
        search,
        setSearch,
        deferredSearch,
        typeFilter,
        setTypeFilter,
        sourceFilter,
        setSourceFilter,
        isFiltering,
        hasMoreThanCurrentPage,
        isLoading: query.isLoading,
        error: query.error,
        page: query.page,
        perPage: query.perPage,
        setPage: query.setPage,
        setPerPage: query.setPerPage,
    };
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
    const columns = useMemo<ColumnDef<EntityInstance>[]>(
        () => [
            {
                id: 'type',
                header: 'Type',
                size: 180,
                cell: ({ row }) => (
                    <Badge variant="outline" className="font-mono text-xs">
                        {row.original.uid.type}
                    </Badge>
                ),
            },
            {
                id: 'entityId',
                header: 'Entity ID',
                size: 360,
                cell: ({ row }) => <span className="font-mono text-xs text-foreground">{formatEntityUid(row.original.uid)}</span>,
            },
            {
                id: 'name',
                header: 'Name',
                size: 280,
                cell: ({ row }) => <span className="block truncate font-medium">{displayNameOf(row.original)}</span>,
            },
            {
                id: 'source',
                header: 'Source',
                size: 140,
                cell: ({ row }) => (
                    <Badge variant="secondary" className="text-xs">
                        {sourceLabelOf(row.original)}
                    </Badge>
                ),
            },
        ],
        [],
    );

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
                data={entities as EntityInstance[]}
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

function ClientFilterWarning({ visible, total }: { readonly visible: number; readonly total: number }) {
    return (
        <Alert role="status" aria-live="polite">
            <AlertDescription>
                Filters apply only to the {visible} entities on this page — {total - visible} more entities are not yet searched. Increase
                the page size or paginate to broaden the view.
            </AlertDescription>
        </Alert>
    );
}

export function EntitiesPage() {
    const env = useEnvironment();
    const environmentId = env?.id ?? '';

    const principals = useEntitiesTab({ environmentId, kind: 'PRINCIPAL' });
    const resources = useEntitiesTab({ environmentId, kind: 'RESOURCE', excludeEntityIdPrefix: ACTION_PREFIX });

    const isLoading = principals.isLoading || resources.isLoading;
    const error = principals.error ?? resources.error;

    const kpis = useMemo(() => {
        const visibleTypes = new Set<string>();
        principals.entities.forEach(e => visibleTypes.add(e.uid.type));
        resources.entities.forEach(e => visibleTypes.add(e.uid.type));
        return {
            total: principals.total + resources.total,
            typesOnPage: visibleTypes.size,
            principals: principals.total,
            resources: resources.total,
        };
    }, [principals.entities, resources.entities, principals.total, resources.total]);

    return (
        <div className="flex flex-col gap-4">
            <header className="flex items-start gap-3">
                <BoxesIcon className="mt-1 size-5 text-muted-foreground" aria-hidden />
                <div>
                    <h1 className="text-xl font-semibold">Entities</h1>
                    <p className="mt-1 max-w-3xl text-sm text-muted-foreground">
                        Principals and resources the policy engine evaluates. Resource entities are imported from the Context Catalog so
                        every policy refers to the same canonical Entity ID.
                    </p>
                </div>
            </header>

            <div className="grid grid-cols-2 gap-3 md:grid-cols-4" aria-label="Key metrics">
                <KpiTile label="Total entities" value={kpis.total} loading={isLoading} />
                <KpiTile label="Types on page" value={kpis.typesOnPage} loading={isLoading} />
                <KpiTile label="Principals" value={kpis.principals} loading={isLoading} />
                <KpiTile label="Resources" value={kpis.resources} loading={isLoading} />
            </div>

            {error && (
                <Alert variant="destructive">
                    <AlertTitle>Could not load entities</AlertTitle>
                    <AlertDescription>{error}</AlertDescription>
                </Alert>
            )}

            <Tabs defaultValue="principals" className="flex flex-col gap-3">
                <TabsList>
                    <TabsTrigger value="principals">
                        <UsersIcon className="size-4" aria-hidden />
                        Principals
                        <Badge variant="secondary">{principals.total}</Badge>
                    </TabsTrigger>
                    <TabsTrigger value="resources">
                        <ShieldIcon className="size-4" aria-hidden />
                        Resources
                        <Badge variant="secondary">{resources.total}</Badge>
                    </TabsTrigger>
                </TabsList>

                <TabsContent value="principals" className="flex flex-col gap-3">
                    <Alert>
                        <AlertDescription>{PRINCIPALS_HELP}</AlertDescription>
                    </Alert>
                    <EntitiesFilterBar
                        search={principals.search}
                        onSearch={principals.setSearch}
                        typesPresent={principals.types}
                        typeFilter={principals.typeFilter}
                        onTypeFilter={principals.setTypeFilter}
                        sourcesPresent={principals.sources}
                        sourceFilter={principals.sourceFilter}
                        onSourceFilter={principals.setSourceFilter}
                        searchLabel="Search principals"
                    />
                    {principals.isFiltering && principals.hasMoreThanCurrentPage && (
                        <ClientFilterWarning visible={principals.entities.length} total={principals.total} />
                    )}
                    <EntitiesTable
                        tab="principals"
                        entities={principals.filtered}
                        searchValue={principals.deferredSearch}
                        isLoading={principals.isLoading}
                        page={principals.page}
                        perPage={principals.perPage}
                        totalCount={principals.total}
                        onPageChange={principals.setPage}
                        onPerPageChange={principals.setPerPage}
                    />
                </TabsContent>

                <TabsContent value="resources" className="flex flex-col gap-3">
                    <Alert>
                        <AlertDescription>{RESOURCES_HELP}</AlertDescription>
                    </Alert>
                    <EntitiesFilterBar
                        search={resources.search}
                        onSearch={resources.setSearch}
                        typesPresent={resources.types}
                        typeFilter={resources.typeFilter}
                        onTypeFilter={resources.setTypeFilter}
                        sourcesPresent={resources.sources}
                        sourceFilter={resources.sourceFilter}
                        onSourceFilter={resources.setSourceFilter}
                        searchLabel="Search resources"
                    />
                    {resources.isFiltering && resources.hasMoreThanCurrentPage && (
                        <ClientFilterWarning visible={resources.entities.length} total={resources.total} />
                    )}
                    <EntitiesTable
                        tab="resources"
                        entities={resources.filtered}
                        searchValue={resources.deferredSearch}
                        isLoading={resources.isLoading}
                        page={resources.page}
                        perPage={resources.perPage}
                        totalCount={resources.total}
                        onPageChange={resources.setPage}
                        onPerPageChange={resources.setPerPage}
                    />
                </TabsContent>
            </Tabs>
        </div>
    );
}
