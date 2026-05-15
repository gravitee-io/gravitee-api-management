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
import {
    Badge,
    Button,
    Dialog,
    DialogContent,
    DialogFooter,
    DialogHeader,
    DialogTitle,
    Input,
    ScrollArea,
    Skeleton,
} from '@gravitee/graphene-core';
import { GlobeIcon, InfoIcon, SearchIcon, XIcon } from '@gravitee/graphene-core/icons';
import { useCallback, useEffect, useMemo, useState } from 'react';

import type { ApiListItem } from '../../../apis/types/api';
import { useApisAvailableForProduct } from '../../hooks/useApiProductApis';

interface AddApiToProductDialogProps {
    open: boolean;
    existingApiIds: string[];
    onClose: () => void;
    onAdd: (apiIds: string[]) => void;
    isAdding: boolean;
}

function ApiRow({ api, selected, onToggle }: { api: ApiListItem; selected: boolean; onToggle: () => void }) {
    const path = api.listeners?.find(l => l.type === 'HTTP')?.paths?.[0]?.path ?? '';
    return (
        <button
            type="button"
            onClick={onToggle}
            className={`w-full flex items-center gap-3 rounded-lg border px-3 py-2.5 text-left transition-colors ${
                selected ? 'border-primary/30 bg-primary/5' : 'hover:bg-muted'
            }`}
        >
            <div className="rounded-md bg-primary/10 p-1.5 shrink-0">
                <GlobeIcon className="size-3.5 text-primary" aria-hidden />
            </div>
            <div className="flex-1 min-w-0">
                <p className="text-sm font-medium truncate">{api.name}</p>
                {path ? <p className="text-xs text-muted-foreground truncate font-mono">{path}</p> : null}
            </div>
            <Badge variant="outline" className="text-xs shrink-0">
                {api.apiVersion}
            </Badge>
        </button>
    );
}

function SelectedChip({ name, onRemove }: { name: string; onRemove: () => void }) {
    return (
        <span className="inline-flex items-center gap-1 rounded-full border bg-secondary text-secondary-foreground px-2 py-0.5 text-xs font-medium">
            {name}
            <button type="button" onClick={onRemove} className="hover:text-destructive transition-colors" aria-label={`Remove ${name}`}>
                <XIcon className="size-3" aria-hidden />
            </button>
        </span>
    );
}

export function AddApiToProductDialog({ open, existingApiIds, onClose, onAdd, isAdding }: AddApiToProductDialogProps) {
    const [search, setSearch] = useState('');
    const [debouncedSearch, setDebouncedSearch] = useState('');
    const [selectedIds, setSelectedIds] = useState<Set<string>>(new Set());
    const [selectedApis, setSelectedApis] = useState<Map<string, ApiListItem>>(new Map());

    useEffect(() => {
        if (!open) {
            setSearch('');
            setDebouncedSearch('');
            setSelectedIds(new Set());
            setSelectedApis(new Map());
        }
    }, [open]);

    useEffect(() => {
        const timer = setTimeout(() => setDebouncedSearch(search), 300);
        return () => clearTimeout(timer);
    }, [search]);

    const hasSearch = debouncedSearch.trim().length > 0;
    const { data, isLoading } = useApisAvailableForProduct(debouncedSearch, 1, 50);

    const existingSet = useMemo(() => new Set(existingApiIds), [existingApiIds]);
    const availableApis = useMemo(() => (data?.data ?? []).filter(api => !existingSet.has(api.id)), [data, existingSet]);

    const toggleApi = useCallback((api: ApiListItem) => {
        setSelectedIds(prev => {
            const next = new Set(prev);
            if (next.has(api.id)) next.delete(api.id);
            else next.add(api.id);
            return next;
        });
        setSelectedApis(prev => {
            const next = new Map(prev);
            if (next.has(api.id)) next.delete(api.id);
            else next.set(api.id, api);
            return next;
        });
    }, []);

    const handleAdd = useCallback(() => onAdd([...selectedIds]), [onAdd, selectedIds]);
    const handleOpenChange = useCallback(
        (isOpen: boolean) => {
            if (!isOpen) onClose();
        },
        [onClose],
    );

    return (
        <Dialog open={open} onOpenChange={handleOpenChange}>
            <DialogContent className="max-w-lg">
                <DialogHeader>
                    <DialogTitle>Add API</DialogTitle>
                </DialogHeader>

                <div className="space-y-4">
                    {/* Visibility hint */}
                    <div className="flex items-start gap-2 rounded-lg border bg-muted/40 px-3 py-2.5">
                        <InfoIcon className="size-4 text-muted-foreground shrink-0 mt-0.5" aria-hidden />
                        <p className="text-xs text-muted-foreground">
                            <span className="font-semibold">API visibility</span> — Cannot see all your APIs? APIs must have API products
                            enabled before they appear in this list.
                        </p>
                    </div>

                    {/* Search */}
                    <div className="space-y-2">
                        <p className="text-sm font-medium">Search APIs</p>
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

                    {/* API list */}
                    <ScrollArea className="max-h-72">
                        {!hasSearch ? (
                            <div className="flex flex-col items-center gap-2 py-8 text-center">
                                <SearchIcon className="size-6 text-muted-foreground opacity-50" aria-hidden />
                                <p className="text-sm text-muted-foreground">Type a name or path to find APIs</p>
                            </div>
                        ) : isLoading ? (
                            <div className="space-y-1.5 pr-1">
                                {Array.from({ length: 4 }).map((_, i) => (
                                    <Skeleton key={i} className="h-12 w-full rounded-lg" />
                                ))}
                            </div>
                        ) : availableApis.length === 0 ? (
                            <p className="text-sm text-muted-foreground text-center py-6">
                                No APIs found for &ldquo;{debouncedSearch}&rdquo;.
                            </p>
                        ) : (
                            <div className="space-y-1.5 pr-1">
                                {availableApis.map(api => (
                                    <ApiRow key={api.id} api={api} selected={selectedIds.has(api.id)} onToggle={() => toggleApi(api)} />
                                ))}
                            </div>
                        )}
                    </ScrollArea>

                    {/* Selected chips — capped so they never push the dialog off-screen */}
                    {selectedIds.size > 0 ? (
                        <ScrollArea className="max-h-20">
                            <div className="flex flex-wrap gap-1.5 pt-1 pr-1">
                                {[...selectedApis.values()].map(api => (
                                    <SelectedChip key={api.id} name={api.name} onRemove={() => toggleApi(api)} />
                                ))}
                            </div>
                        </ScrollArea>
                    ) : null}
                </div>

                <DialogFooter>
                    <Button type="button" variant="outline" onClick={onClose}>
                        Cancel
                    </Button>
                    <Button type="button" onClick={handleAdd} disabled={selectedIds.size === 0 || isAdding}>
                        {isAdding ? 'Adding…' : `Add${selectedIds.size > 0 ? ` (${selectedIds.size})` : ''}`}
                    </Button>
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
}
