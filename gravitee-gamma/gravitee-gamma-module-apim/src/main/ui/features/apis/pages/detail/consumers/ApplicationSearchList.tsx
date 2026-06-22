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
import { Badge, Button, InputGroup, InputGroupAddon, InputGroupInput, Skeleton } from '@gravitee/graphene-core';
import { MonitorIcon, SearchIcon, XIcon } from '@gravitee/graphene-core/icons';
import { useCallback, useEffect, useState } from 'react';

import { useApplicationSearch } from '../../../hooks/useSubscriptions';
import type { Application } from '../../../types/subscription';

interface ApplicationSearchListProps {
    selected: Application | null;
    onSelect: (app: Application | null) => void;
}

export function ApplicationSearchList({ selected, onSelect }: Readonly<ApplicationSearchListProps>) {
    const [query, setQuery] = useState('');
    const [debouncedQuery, setDebouncedQuery] = useState('');

    useEffect(() => {
        const timer = setTimeout(() => setDebouncedQuery(query), 300);
        return () => clearTimeout(timer);
    }, [query]);

    const { data: apps = [], isLoading } = useApplicationSearch(debouncedQuery);
    const hasQuery = debouncedQuery.trim().length > 0;

    const handleQuery = useCallback((e: React.ChangeEvent<HTMLInputElement>) => setQuery(e.target.value), []);

    if (selected) {
        return (
            <div className="flex items-center gap-3 rounded-md border px-3 py-2">
                <MonitorIcon className="size-4 shrink-0 text-muted-foreground" aria-hidden />
                <div className="min-w-0 flex-1">
                    <p className="text-sm font-medium truncate">{selected.name}</p>
                    {selected.primaryOwner?.displayName && (
                        <p className="text-xs text-muted-foreground truncate">{selected.primaryOwner.displayName}</p>
                    )}
                </div>
                {selected.type && (
                    <Badge variant="secondary" className="text-xs shrink-0">
                        {selected.type}
                    </Badge>
                )}
                <Button type="button" variant="ghost" size="icon" className="size-6 shrink-0" onClick={() => onSelect(null)}>
                    <XIcon className="size-3.5" aria-hidden />
                    <span className="sr-only">Remove selection</span>
                </Button>
            </div>
        );
    }

    return (
        <div className="space-y-2">
            <InputGroup>
                <InputGroupAddon align="inline-start">
                    <SearchIcon className="size-3.5 text-muted-foreground" aria-hidden />
                </InputGroupAddon>
                <InputGroupInput placeholder="Type to search applications…" value={query} onChange={handleQuery} />
            </InputGroup>

            <div className="rounded-md border overflow-y-auto" style={{ maxHeight: '14rem' }}>
                {!hasQuery && (
                    <div className="flex flex-col items-center gap-2 py-8 text-center">
                        <SearchIcon className="size-5 text-muted-foreground" style={{ opacity: 0.4 }} aria-hidden />
                        <p className="text-sm text-muted-foreground">Type a name to find applications</p>
                    </div>
                )}

                {hasQuery && isLoading && (
                    <div className="p-2 space-y-1.5">
                        {[1, 2, 3].map(i => (
                            <Skeleton key={i} className="h-12 w-full rounded" />
                        ))}
                    </div>
                )}

                {hasQuery && !isLoading && apps.length === 0 && (
                    <p className="py-6 text-center text-sm text-muted-foreground">
                        No applications found for &ldquo;{debouncedQuery}&rdquo;.
                    </p>
                )}

                {hasQuery && !isLoading && apps.length > 0 && (
                    <div>
                        {apps.map((app, idx) => (
                            <button
                                key={app.id}
                                type="button"
                                onClick={() => onSelect(app)}
                                className="flex w-full items-center gap-3 px-3 py-2.5 text-left text-sm transition-colors hover:bg-muted"
                                style={idx > 0 ? { borderTop: '1px solid var(--color-border)' } : undefined}
                            >
                                <MonitorIcon className="size-4 shrink-0 text-muted-foreground" aria-hidden />
                                <div className="min-w-0 flex-1">
                                    <p className="font-medium truncate">{app.name}</p>
                                    {app.primaryOwner?.displayName && (
                                        <p className="text-xs text-muted-foreground truncate">{app.primaryOwner.displayName}</p>
                                    )}
                                </div>
                                {app.type && (
                                    <Badge variant="secondary" className="text-xs shrink-0">
                                        {app.type}
                                    </Badge>
                                )}
                            </button>
                        ))}
                    </div>
                )}
            </div>
        </div>
    );
}
