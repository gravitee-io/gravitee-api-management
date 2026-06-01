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
/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
import {
    Alert,
    AlertDescription,
    Badge,
    Button,
    Checkbox,
    Input,
    Sheet,
    SheetContent,
    SheetTitle,
    Spinner,
    Tabs,
    TabsContent,
    TabsList,
    TabsTrigger,
    cn,
    toast,
} from '@gravitee/graphene-core';
import { DownloadIcon } from '@gravitee/graphene-core/icons';
import { useQueryClient } from '@tanstack/react-query';
import { useDeferredValue, useMemo, useState } from 'react';
import type { AgentCatalogItem, CatalogItem, McpServerCatalogItem, ModelCatalogItem } from '../../shared/api/aim-catalog.types';
import { authzApiService } from '../../shared/api/authz-api.service';
import { authzQueryKeys } from '../../shared/api/query-keys';
import { useAimCatalogItems, type UseAimCatalogItemsResult } from '../../shared/hooks/useAimCatalogItems';
import { useImportedCatalogIds } from '../../shared/hooks/useImportedCatalogIds';

type TabKey = 'mcp-server' | 'model' | 'agent';

interface TabDef {
    readonly key: TabKey;
    readonly label: string;
    readonly entityKindPrefix: string;
    readonly uiTypeBadge: string;
}

const TABS: readonly TabDef[] = [
    { key: 'mcp-server', label: 'MCP Servers', entityKindPrefix: 'mcp', uiTypeBadge: 'MCPServer' },
    { key: 'model', label: 'AI Models', entityKindPrefix: 'model', uiTypeBadge: 'Model' },
    { key: 'agent', label: 'Agents', entityKindPrefix: 'agent', uiTypeBadge: 'Agent' },
];

const SOURCE_LABEL = 'gravitee-catalog';
const IMPORT_CONCURRENCY = 4;

function slugify(s: string | undefined | null): string {
    if (!s) return '';
    return s
        .toString()
        .toLowerCase()
        .replace(/[^a-z0-9._-]+/g, '-')
        .replace(/^-+|-+$/g, '');
}

function deriveDisplayName(item: CatalogItem): string {
    switch (item.kind) {
        case 'mcp-server':
            return item.definition.serverInfo?.title ?? item.definition.serverInfo?.name ?? item.id;
        case 'model':
            return item.definition.name ?? item.definition.queryName ?? item.id;
        case 'agent':
            return item.definition.name ?? item.id;
    }
}

/**
 * Slug used as the trailing segment(s) of the Authorization entityId.
 *
 *   mcp-servers  → `mcp.{slug}`                e.g. mcp.filesystem
 *   models       → `model.{provider}.{slug}`   e.g. model.openai.gpt-4o
 *   agents       → `agent.{slug}`              e.g. agent.code-reviewer
 *
 * Since aim ≥ 1.0.0-alpha.71 the catalog persists `entityId` server-side via
 * `EntityId.toSlug(...)`, so we prefer it and only fall back to in-app derivation
 * for older rows that haven't been touched by the importer yet.
 */
function deriveSlug(item: CatalogItem): string {
    switch (item.kind) {
        case 'mcp-server': {
            const fromServer = slugify(item.entityId);
            if (fromServer) return fromServer;
            const fromExtensions = slugify(item.extensions?.entityId);
            if (fromExtensions) return fromExtensions;
            return slugify(item.definition.serverInfo?.name) || slugify(item.definition.serverInfo?.title) || item.id;
        }
        case 'model': {
            const provider = providerFromSourceKind(item.sourceKind) || slugify(item.definition.provider);
            const name = slugify(item.entityId) || slugify(item.definition.queryName) || slugify(item.definition.name);
            if (provider && name) return `${provider}.${name}`;
            return name || item.id;
        }
        case 'agent':
            return slugify(item.entityId) || slugify(item.definition.name) || item.id;
    }
}

/** Extract `openai` from `llm.provider.openai` (catalog sourceKind format). */
function providerFromSourceKind(sourceKind: string | null | undefined): string {
    if (!sourceKind) return '';
    const segments = sourceKind.split('.');
    const last = segments[segments.length - 1];
    return slugify(last);
}

function deriveDescription(item: CatalogItem): string | undefined {
    switch (item.kind) {
        case 'mcp-server':
            return item.extensions?.description;
        case 'model':
            return item.definition.description;
        case 'agent':
            return item.definition.description;
    }
}

function buildEntityId(item: CatalogItem): string {
    const tab = TABS.find(t => t.key === item.kind);
    if (!tab) throw new Error(`unknown catalog kind: ${item.kind}`);
    return `${tab.entityKindPrefix}.${deriveSlug(item)}`;
}

/** Run `tasks` with up to `concurrency` in-flight at a time. Preserves order in the returned settled array. */
async function runWithConcurrency<T>(tasks: ReadonlyArray<() => Promise<T>>, concurrency: number): Promise<PromiseSettledResult<T>[]> {
    const results: PromiseSettledResult<T>[] = new Array(tasks.length);
    let cursor = 0;
    const workers = Array.from({ length: Math.min(concurrency, tasks.length) }, async () => {
        while (true) {
            const idx = cursor++;
            if (idx >= tasks.length) return;
            try {
                results[idx] = { status: 'fulfilled', value: await tasks[idx]() };
            } catch (err) {
                results[idx] = { status: 'rejected', reason: err };
            }
        }
    });
    await Promise.all(workers);
    return results;
}

export interface ImportFromCatalogDialogProps {
    readonly open: boolean;
    readonly environmentId: string;
    readonly onOpenChange: (open: boolean) => void;
    readonly onImported: () => void;
}

export function ImportFromCatalogDialog({ open, environmentId, onOpenChange, onImported }: ImportFromCatalogDialogProps) {
    const [activeTab, setActiveTab] = useState<TabKey>('mcp-server');
    const [search, setSearch] = useState('');
    const deferredSearch = useDeferredValue(search);
    const [selected, setSelected] = useState<Map<string, CatalogItem>>(new Map());
    const [submitting, setSubmitting] = useState(false);
    const [progress, setProgress] = useState<{ done: number; total: number } | null>(null);
    const queryClient = useQueryClient();

    const mcpQuery = useAimCatalogItems<McpServerCatalogItem>(environmentId, 'mcp-server', { enabled: open });
    const modelQuery = useAimCatalogItems<ModelCatalogItem>(environmentId, 'model', { enabled: open });
    const agentQuery = useAimCatalogItems<AgentCatalogItem>(environmentId, 'agent', { enabled: open });
    const {
        catalogIds: importedCatalogIds,
        truncated: importedTruncated,
        markImported,
    } = useImportedCatalogIds(environmentId, {
        enabled: open,
    });

    const queryByTab: Record<TabKey, UseAimCatalogItemsResult<CatalogItem>> = useMemo(
        () => ({
            'mcp-server': mcpQuery as UseAimCatalogItemsResult<CatalogItem>,
            model: modelQuery as UseAimCatalogItemsResult<CatalogItem>,
            agent: agentQuery as UseAimCatalogItemsResult<CatalogItem>,
        }),
        [mcpQuery, modelQuery, agentQuery],
    );

    function toggleSelect(item: CatalogItem) {
        if (importedCatalogIds.has(item.id)) return;
        setSelected(prev => {
            const next = new Map(prev);
            if (next.has(item.id)) next.delete(item.id);
            else next.set(item.id, item);
            return next;
        });
    }

    function selectVisible(items: readonly CatalogItem[]) {
        setSelected(prev => {
            const next = new Map(prev);
            items.forEach(it => {
                if (!importedCatalogIds.has(it.id)) next.set(it.id, it);
            });
            return next;
        });
    }

    function clearSelection() {
        setSelected(new Map());
    }

    function resetAndClose(nextOpen: boolean) {
        if (!nextOpen) {
            setSelected(new Map());
            setSearch('');
            setActiveTab('mcp-server');
            setProgress(null);
        }
        onOpenChange(nextOpen);
    }

    async function handleImport() {
        if (selected.size === 0 || submitting) return;
        setSubmitting(true);
        const items = Array.from(selected.values());
        setProgress({ done: 0, total: items.length });

        const successCatalogIds: string[] = [];
        let doneCount = 0;

        const tasks = items.map(item => async () => {
            const entityId = buildEntityId(item);
            const displayName = deriveDisplayName(item);
            const description = deriveDescription(item);
            const attributes: Record<string, unknown> = {
                _displayName: displayName,
                _catalogId: item.id,
                _importedAt: new Date().toISOString(),
            };
            if (description) attributes.description = description;
            try {
                await authzApiService.createEntity(environmentId, {
                    entityId,
                    kind: 'RESOURCE',
                    attributes,
                    parents: [],
                    source: SOURCE_LABEL,
                });
                successCatalogIds.push(item.id);
                return item;
            } finally {
                doneCount++;
                setProgress({ done: doneCount, total: items.length });
            }
        });

        const settled = await runWithConcurrency(tasks, IMPORT_CONCURRENCY);
        const failed = settled.filter(r => r.status === 'rejected') as PromiseRejectedResult[];
        const success = settled.length - failed.length;

        setSubmitting(false);
        setProgress(null);

        if (success > 0) {
            markImported(successCatalogIds);
            toast.success(`Imported ${success} ${success === 1 ? 'entity' : 'entities'}`);
            void queryClient.invalidateQueries({ queryKey: authzQueryKeys.entities.all(environmentId) });
            void queryClient.invalidateQueries({ queryKey: authzQueryKeys.importedCatalogIds(environmentId) });
            onImported();
        }
        if (failed.length > 0) {
            const sample = failed
                .slice(0, 2)
                .map(r => (r.reason instanceof Error ? r.reason.message : String(r.reason)))
                .join('; ');
            toast.error(`Failed to import ${failed.length}: ${sample}`);
            failed.forEach(r => console.error('catalog import failed:', r.reason));
        }
        if (failed.length === 0) {
            resetAndClose(false);
        }
    }

    const selectedCount = selected.size;
    const tabCount = (key: TabKey): number => queryByTab[key].data?.total ?? 0;

    const ariaLabel = 'Import from AI Catalog';

    function renderTabBody(tab: TabDef, query: UseAimCatalogItemsResult<CatalogItem>) {
        const items: readonly CatalogItem[] = query.data?.data ?? [];
        const needle = deferredSearch.trim().toLowerCase();
        const filtered = needle
            ? items.filter(
                  it =>
                      deriveDisplayName(it).toLowerCase().includes(needle) ||
                      deriveSlug(it).toLowerCase().includes(needle) ||
                      buildEntityId(it).toLowerCase().includes(needle),
              )
            : items;

        return (
            <div className="flex min-h-0 flex-1 flex-col">
                <div className="flex items-center gap-2 border-b px-6 py-3">
                    <Input
                        value={search}
                        onChange={e => setSearch(e.target.value)}
                        placeholder={`Search ${tab.label.toLowerCase()}…`}
                        className="min-w-0 flex-1"
                        aria-label="Search catalog entries"
                    />
                    <Button variant="ghost" size="sm" onClick={() => selectVisible(filtered)} disabled={filtered.length === 0}>
                        Select visible
                    </Button>
                    <Button variant="ghost" size="sm" onClick={clearSelection} disabled={selectedCount === 0}>
                        Clear ({selectedCount})
                    </Button>
                </div>

                {(query.truncated || importedTruncated) && (
                    <Alert className="mx-6 mt-3" role="status">
                        <AlertDescription>
                            {query.truncated && (
                                <div>
                                    Showing the first {query.data?.data.length ?? 0} of {query.data?.total ?? 0} catalog entries. Narrow
                                    your search in the Catalog page if the item you need is not visible.
                                </div>
                            )}
                            {importedTruncated && (
                                <div>
                                    More than the displayed imports exist in this environment — some already-imported items may not be
                                    flagged as such.
                                </div>
                            )}
                        </AlertDescription>
                    </Alert>
                )}

                <div className="min-h-0 flex-1 overflow-y-auto">
                    {query.isLoading && (
                        <div className="flex items-center justify-center p-6">
                            <Spinner />
                        </div>
                    )}
                    {query.error && (
                        <Alert variant="destructive" className="m-4">
                            <AlertDescription>{query.error}</AlertDescription>
                        </Alert>
                    )}
                    {!query.isLoading && !query.error && filtered.length === 0 && (
                        <div className="flex h-full min-h-48 items-center justify-center p-6 text-sm text-muted-foreground">
                            {items.length === 0 ? 'No items in this catalog category yet.' : 'No matches for the current search.'}
                        </div>
                    )}
                    {!query.isLoading &&
                        !query.error &&
                        filtered.map(item => {
                            const isSelected = selected.has(item.id);
                            const isImported = importedCatalogIds.has(item.id);
                            const entityId = buildEntityId(item);
                            const description = deriveDescription(item);
                            return (
                                <label
                                    key={item.id}
                                    className={cn(
                                        'flex items-start gap-3 border-b px-6 py-3 transition-colors',
                                        isImported && 'cursor-not-allowed opacity-60',
                                        !isImported && 'cursor-pointer hover:bg-muted/40',
                                        !isImported && isSelected && 'bg-muted/30',
                                    )}
                                >
                                    <Checkbox
                                        checked={isImported || isSelected}
                                        disabled={isImported}
                                        onCheckedChange={() => toggleSelect(item)}
                                        className="mt-0.5"
                                        aria-label={
                                            isImported ? `${deriveDisplayName(item)} already imported` : `Select ${deriveDisplayName(item)}`
                                        }
                                    />
                                    <div className="min-w-0 flex-1">
                                        <div className="flex items-center gap-2">
                                            <span className="truncate text-sm font-medium">{deriveDisplayName(item)}</span>
                                            <Badge variant="outline" className="shrink-0 font-mono text-xs">
                                                {tab.uiTypeBadge}
                                            </Badge>
                                            {isImported && (
                                                <Badge variant="secondary" className="shrink-0 text-xs">
                                                    Imported
                                                </Badge>
                                            )}
                                        </div>
                                        <code className="mt-1 block truncate font-mono text-xs text-muted-foreground" title={entityId}>
                                            {entityId}
                                        </code>
                                        {description && (
                                            <p
                                                className="mt-1 line-clamp-2 text-xs text-muted-foreground"
                                                title={description.length > 500 ? description.slice(0, 500) + '…' : description}
                                            >
                                                {description}
                                            </p>
                                        )}
                                    </div>
                                </label>
                            );
                        })}
                </div>
            </div>
        );
    }

    return (
        <Sheet open={open} onOpenChange={resetAndClose}>
            <SheetContent
                side="right"
                showCloseButton={false}
                aria-label={ariaLabel}
                style={{ width: 'min(820px, 100vw)', maxWidth: 'min(820px, 100vw)' }}
                className="flex h-full flex-col gap-0 p-0"
            >
                <SheetTitle className="sr-only">{ariaLabel}</SheetTitle>

                <div className="border-b px-6 py-4">
                    <h2 className="text-lg font-semibold">Import from AI Catalog</h2>
                    <p className="mt-1 text-sm text-muted-foreground">
                        Select catalog entries to register as read-only resource entities in the Policy Engine. Imported entities keep the
                        same Entity ID as in the Catalog, so policies always refer to one canonical identifier.
                    </p>
                </div>

                <Tabs value={activeTab} onValueChange={value => setActiveTab(value as TabKey)} className="flex min-h-0 flex-1 flex-col">
                    <TabsList className="flex-none gap-1 border-b px-6 pt-2">
                        {TABS.map(tab => (
                            <TabsTrigger key={tab.key} value={tab.key}>
                                {tab.label}
                                <Badge variant="secondary" className="ml-1 text-xs">
                                    {tabCount(tab.key)}
                                </Badge>
                            </TabsTrigger>
                        ))}
                    </TabsList>

                    {TABS.map(tab => (
                        <TabsContent key={tab.key} value={tab.key} className="mt-0 flex min-h-0 flex-1 flex-col">
                            {renderTabBody(tab, queryByTab[tab.key])}
                        </TabsContent>
                    ))}
                </Tabs>

                <div className="flex flex-none items-center justify-between border-t bg-muted/40 px-6 py-3.5">
                    <span className="text-sm text-muted-foreground">
                        {progress
                            ? `Importing ${progress.done} / ${progress.total}…`
                            : selectedCount === 0
                              ? 'No items selected'
                              : `${selectedCount} item${selectedCount === 1 ? '' : 's'} selected`}
                    </span>
                    <div className="flex gap-2">
                        <Button variant="outline" onClick={() => resetAndClose(false)} disabled={submitting}>
                            Cancel
                        </Button>
                        <Button onClick={handleImport} disabled={selectedCount === 0 || submitting}>
                            {submitting ? (
                                <Spinner className="mr-2 size-4" aria-hidden />
                            ) : (
                                <DownloadIcon className="mr-2 size-4" aria-hidden />
                            )}
                            Import
                        </Button>
                    </div>
                </div>
            </SheetContent>
        </Sheet>
    );
}
