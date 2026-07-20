/*
 * Copyright (C) 2026 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import {
    Button,
    Card,
    CardContent,
    Checkbox,
    Dialog,
    DialogClose,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
    Input,
    Spinner,
} from '@gravitee/graphene-core';
import { SearchIcon, Trash2Icon, XIcon } from '@gravitee/graphene-core/icons';
import { useEffect, useMemo, useState } from 'react';

import { useApisForMapping } from '../hooks/useApisForMapping';
import type { MappedApi } from '../types';

interface ApiMappingPanelProps {
    readonly mappedApis: readonly MappedApi[];
    readonly onChange: (mappedApis: readonly MappedApi[]) => void;
    readonly description?: string;
}

export function ApiMappingPanel({
    mappedApis,
    onChange,
    description = 'Choose which APIs belong to this item.',
}: ApiMappingPanelProps) {
    const [dialogOpen, setDialogOpen] = useState(false);

    const removeMapped = (apiId: string) => {
        onChange(mappedApis.filter(api => api.id !== apiId));
    };

    return (
        <section aria-labelledby="api-mapping-heading" className="space-y-3">
            <div className="flex flex-wrap items-start justify-between gap-3">
                <div className="space-y-0.5">
                    <h2 id="api-mapping-heading" className="text-base font-semibold tracking-tight">
                        Map APIs
                    </h2>
                    <p className="text-xs text-muted-foreground">{description}</p>
                </div>
                <Button type="button" onClick={() => setDialogOpen(true)}>
                    Map APIs
                </Button>
            </div>

            <Card>
                <CardContent className="space-y-3 p-5">
                    {mappedApis.length === 0 ? (
                        <p className="text-sm text-muted-foreground">
                            No APIs mapped yet. Click Map APIs to search and select APIs.
                        </p>
                    ) : (
                        <ul className="space-y-2">
                            {mappedApis.map(api => (
                                <li
                                    key={api.id}
                                    className="flex items-center justify-between gap-3 rounded-lg border border-border/60 px-3 py-2"
                                >
                                    <div className="min-w-0">
                                        <p className="truncate text-sm font-medium">{api.name}</p>
                                        <p className="truncate text-xs text-muted-foreground">{api.id}</p>
                                    </div>
                                    <Button
                                        type="button"
                                        variant="ghost"
                                        size="sm"
                                        aria-label={`Remove ${api.name}`}
                                        onClick={() => removeMapped(api.id)}
                                    >
                                        <Trash2Icon className="size-4" aria-hidden />
                                    </Button>
                                </li>
                            ))}
                        </ul>
                    )}
                </CardContent>
            </Card>

            <MapApisDialog
                open={dialogOpen}
                onOpenChange={setDialogOpen}
                mappedApis={mappedApis}
                onChange={onChange}
                description={description}
            />
        </section>
    );
}

export function MapApisDialog({
    open,
    onOpenChange,
    mappedApis,
    onChange,
    description = 'Search for APIs and select which ones to map.',
    title = 'Map APIs',
}: {
    readonly open: boolean;
    readonly onOpenChange: (open: boolean) => void;
    readonly mappedApis: readonly MappedApi[];
    readonly onChange: (mappedApis: readonly MappedApi[]) => void;
    readonly description?: string;
    readonly title?: string;
}) {
    const [search, setSearch] = useState('');
    const [debouncedSearch, setDebouncedSearch] = useState('');
    const [draftMapped, setDraftMapped] = useState<MappedApi[]>([...mappedApis]);
    const { apis, loading } = useApisForMapping({ enabled: open, query: debouncedSearch });

    useEffect(() => {
        if (!open) {
            return;
        }
        setDraftMapped([...mappedApis]);
        setSearch('');
        setDebouncedSearch('');
    }, [open, mappedApis]);

    useEffect(() => {
        const handle = window.setTimeout(() => setDebouncedSearch(search.trim()), 250);
        return () => window.clearTimeout(handle);
    }, [search]);

    const draftIds = useMemo(() => new Set(draftMapped.map(api => api.id)), [draftMapped]);

    const filteredApis = useMemo(() => {
        const q = search.trim().toLowerCase();
        if (!q) {
            return apis;
        }
        return apis.filter(api => api.name.toLowerCase().includes(q) || api.id.toLowerCase().includes(q));
    }, [apis, search]);

    const toggle = (api: MappedApi, checked: boolean) => {
        if (checked) {
            setDraftMapped(current => [...current.filter(a => a.id !== api.id), api]);
            return;
        }
        setDraftMapped(current => current.filter(a => a.id !== api.id));
    };

    const apply = () => {
        onChange(draftMapped);
        onOpenChange(false);
    };

    return (
        <Dialog open={open} onOpenChange={onOpenChange}>
            <DialogContent className="max-w-lg">
                <DialogHeader>
                    <DialogTitle>{title}</DialogTitle>
                    <DialogDescription>{description}</DialogDescription>
                </DialogHeader>

                <div className="space-y-4">
                    <div className="relative">
                        <SearchIcon
                            className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground"
                            aria-hidden
                        />
                        <Input
                            value={search}
                            onChange={event => setSearch(event.target.value)}
                            placeholder="Search APIs by name or id"
                            aria-label="Search APIs"
                            className="pl-9"
                            autoFocus
                        />
                        {search ? (
                            <Button
                                type="button"
                                variant="ghost"
                                size="sm"
                                className="absolute right-1 top-1/2 -translate-y-1/2"
                                aria-label="Clear search"
                                onClick={() => setSearch('')}
                            >
                                <XIcon className="size-4" aria-hidden />
                            </Button>
                        ) : null}
                    </div>

                    <div className="max-h-72 overflow-y-auto rounded-lg border border-border/60">
                        {loading ? (
                            <div className="flex items-center gap-2 px-4 py-6 text-sm text-muted-foreground">
                                <Spinner className="size-4" />
                                Searching APIs…
                            </div>
                        ) : filteredApis.length === 0 ? (
                            <p className="px-4 py-6 text-sm text-muted-foreground">
                                {search.trim() ? 'No APIs match your search.' : 'No APIs found in this environment.'}
                            </p>
                        ) : (
                            <ul className="divide-y divide-border/60">
                                {filteredApis.map(api => {
                                    const checked = draftIds.has(api.id);
                                    return (
                                        <li key={api.id} className="flex items-center gap-3 px-4 py-3">
                                            <Checkbox
                                                checked={checked}
                                                onCheckedChange={value => toggle(api, value === true)}
                                                aria-label={`Map ${api.name}`}
                                            />
                                            <div className="min-w-0">
                                                <p className="truncate text-sm font-medium">{api.name}</p>
                                                <p className="truncate text-xs text-muted-foreground">{api.id}</p>
                                            </div>
                                        </li>
                                    );
                                })}
                            </ul>
                        )}
                    </div>

                    <p className="text-xs text-muted-foreground">
                        {draftMapped.length} API{draftMapped.length === 1 ? '' : 's'} selected
                    </p>
                </div>

                <DialogFooter>
                    <DialogClose asChild>
                        <Button type="button" variant="outline">
                            Cancel
                        </Button>
                    </DialogClose>
                    <Button type="button" onClick={apply}>
                        Apply
                    </Button>
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
}
