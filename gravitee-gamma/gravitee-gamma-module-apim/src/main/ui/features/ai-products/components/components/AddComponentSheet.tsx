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
import { Button, Input, ScrollArea, Sheet, SheetContent, SheetFooter, SheetHeader, SheetTitle, Skeleton } from '@gravitee/graphene-core';
import { BrainCircuitIcon, InfoIcon, SearchIcon, XIcon } from '@gravitee/graphene-core/icons';
import { useCallback, useEffect, useMemo, useState } from 'react';

import type { ApiListItem } from '../../../apis/types';
import { useLlmComponentSearch } from '../../hooks/useAiProductHooks';
import { ComponentTypeBadge } from '../ComponentTypeBadge';

interface AddComponentSheetProps {
    open: boolean;
    existingApiIds: string[];
    onClose: () => void;
    onAdd: (apiIds: string[]) => void;
    isAdding: boolean;
}

function ComponentRow({ api, selected, onToggle }: { api: ApiListItem; selected: boolean; onToggle: () => void }) {
    const path = api.listeners?.find(l => l.type === 'HTTP')?.paths?.[0]?.path ?? '';
    return (
        <button
            type="button"
            onClick={onToggle}
            className={`w-full flex items-start gap-3 rounded-lg border px-3 py-2.5 text-left transition-colors ${
                selected ? 'border-primary/30 bg-primary/5' : 'hover:bg-muted'
            }`}
        >
            <div className="rounded-md bg-primary/10 p-1.5 shrink-0 mt-0.5">
                <BrainCircuitIcon className="size-3.5 text-primary" aria-hidden />
            </div>
            <div className="flex-1">
                <p className="text-sm font-medium break-all">{api.name}</p>
                {path ? <p className="text-xs text-muted-foreground font-mono">{path}</p> : null}
            </div>
            <ComponentTypeBadge type={api.type} />
        </button>
    );
}

function SelectedChip({ name, onRemove }: { name: string; onRemove: () => void }) {
    return (
        <span
            className="inline-flex items-center gap-1 rounded-full border bg-secondary text-secondary-foreground px-2 py-0.5 text-xs font-medium"
            style={{ maxWidth: '16rem' }}
            title={name}
        >
            <span style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{name}</span>
            <button type="button" onClick={onRemove} className="hover:text-destructive transition-colors" aria-label={`Remove ${name}`}>
                <XIcon className="size-3" aria-hidden />
            </button>
        </span>
    );
}

/**
 * Search & multi-select LLM proxies to attach as AI Product components.
 * Mirrors the AddApiToProduct sheet pattern, scoped to LLM_PROXY APIs.
 */
export function AddComponentSheet({ open, existingApiIds, onClose, onAdd, isAdding }: AddComponentSheetProps) {
    const [search, setSearch] = useState('');
    const [debouncedSearch, setDebouncedSearch] = useState('');
    const [selectedApis, setSelectedApis] = useState<Map<string, ApiListItem>>(new Map());
    const [prevOpen, setPrevOpen] = useState(open);
    if (prevOpen !== open) {
        setPrevOpen(open);
        if (!open) {
            setSearch('');
            setDebouncedSearch('');
            setSelectedApis(new Map());
        }
    }

    useEffect(() => {
        const timer = setTimeout(() => setDebouncedSearch(search), 300);
        return () => clearTimeout(timer);
    }, [search]);

    const hasSearch = debouncedSearch.trim().length > 0;
    const { data, isLoading } = useLlmComponentSearch(debouncedSearch, 1, 50);

    const existingSet = useMemo(() => new Set(existingApiIds), [existingApiIds]);
    const availableApis = useMemo(() => (data?.data ?? []).filter(api => !existingSet.has(api.id)), [data, existingSet]);

    const toggleApi = useCallback((api: ApiListItem) => {
        setSelectedApis(prev => {
            const next = new Map(prev);
            if (next.has(api.id)) next.delete(api.id);
            else next.set(api.id, api);
            return next;
        });
    }, []);

    const handleAdd = useCallback(() => onAdd([...selectedApis.keys()]), [onAdd, selectedApis]);
    const handleOpenChange = useCallback(
        (isOpen: boolean) => {
            if (!isOpen) onClose();
        },
        [onClose],
    );

    return (
        <Sheet open={open} onOpenChange={handleOpenChange}>
            <SheetContent side="right" aria-describedby={undefined} style={{ maxWidth: '480px' }}>
                <SheetHeader>
                    <SheetTitle>Add component</SheetTitle>
                </SheetHeader>

                <div className="flex min-h-0 flex-1 flex-col gap-4 px-4">
                    <div className="flex items-start gap-2 rounded-lg border bg-muted/40 px-3 py-2.5">
                        <InfoIcon className="size-4 text-muted-foreground shrink-0 mt-0.5" aria-hidden />
                        <p className="text-xs text-muted-foreground">
                            <span className="font-semibold">LLM proxies</span> — any V4 LLM proxy can be added to this product. MCP proxies
                            and Agents are coming soon.
                        </p>
                    </div>

                    <div className="space-y-2">
                        <p className="text-sm font-medium">Search LLM proxies</p>
                        <div className="relative">
                            <SearchIcon
                                className="absolute left-3 top-1/2 -translate-y-1/2 size-4 text-muted-foreground pointer-events-none"
                                aria-hidden
                            />
                            <Input
                                placeholder="Filter by name or context path..."
                                value={search}
                                onChange={e => setSearch(e.target.value)}
                                className="pl-9"
                            />
                        </div>
                    </div>

                    <ScrollArea className="min-h-0 flex-1">
                        {!hasSearch ? (
                            <div className="flex flex-col items-center gap-2 py-8 text-center">
                                <SearchIcon className="size-6 text-muted-foreground opacity-50" aria-hidden />
                                <p className="text-sm text-muted-foreground">Type a name or path to find LLM proxies</p>
                            </div>
                        ) : isLoading ? (
                            <div className="space-y-1.5 pr-3">
                                {Array.from({ length: 4 }).map((_, i) => (
                                    <Skeleton key={i} className="h-12 w-full rounded-lg" />
                                ))}
                            </div>
                        ) : availableApis.length === 0 ? (
                            <p className="text-sm text-muted-foreground text-center py-6">
                                No LLM proxies found for &ldquo;{debouncedSearch}&rdquo;.
                            </p>
                        ) : (
                            <div className="space-y-1.5 pr-3">
                                {availableApis.map(api => (
                                    <ComponentRow
                                        key={api.id}
                                        api={api}
                                        selected={selectedApis.has(api.id)}
                                        onToggle={() => toggleApi(api)}
                                    />
                                ))}
                            </div>
                        )}
                    </ScrollArea>

                    {selectedApis.size > 0 ? (
                        <div className="overflow-y-auto" style={{ maxHeight: '5rem' }}>
                            <div className="flex flex-wrap gap-1.5 pt-1 pr-1">
                                {[...selectedApis.values()].map(api => (
                                    <SelectedChip key={api.id} name={api.name} onRemove={() => toggleApi(api)} />
                                ))}
                            </div>
                        </div>
                    ) : null}
                </div>

                <SheetFooter className="flex-row justify-end border-t">
                    <Button type="button" variant="outline" onClick={onClose}>
                        Cancel
                    </Button>
                    <Button type="button" onClick={handleAdd} disabled={selectedApis.size === 0 || isAdding}>
                        {isAdding ? 'Adding…' : `Add${selectedApis.size > 0 ? ` (${selectedApis.size})` : ''}`}
                    </Button>
                </SheetFooter>
            </SheetContent>
        </Sheet>
    );
}
