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
import { Badge, Button, Input } from '@gravitee/graphene-core';
import { XIcon } from '@gravitee/graphene-core/icons';
import { useEffect, useState } from 'react';

import { useApiSubscriberSearch } from '../../../hooks/useSubscriptions';
import type { SubscriptionContext } from '../../../types/subscription';

interface ApplicationFilterProps {
    ctx: SubscriptionContext;
    selectedIds: string[];
    onChange: (applicationIds: string[]) => void;
}

/** Searches the API's subscribers as the user types, like the classic console, instead of loading them all. */
export function ApplicationFilter({ ctx, selectedIds, onChange }: Readonly<ApplicationFilterProps>) {
    const [query, setQuery] = useState('');
    const [debouncedQuery, setDebouncedQuery] = useState('');
    const [labels, setLabels] = useState<Record<string, string>>({});

    useEffect(() => {
        const timer = setTimeout(() => setDebouncedQuery(query), 300);
        return () => clearTimeout(timer);
    }, [query]);

    const { data: results = [], isLoading } = useApiSubscriberSearch(ctx, debouncedQuery);
    const options = results.filter(option => !selectedIds.includes(option.value));
    const isSearching = query.trim().length > 0;

    const handleSelect = (option: { value: string; label: string }) => {
        setLabels(previous => ({ ...previous, [option.value]: option.label }));
        onChange([...selectedIds, option.value]);
        setQuery('');
        setDebouncedQuery('');
    };

    return (
        <div className="space-y-2">
            <div className="relative">
                <Input
                    className="w-full"
                    placeholder="Search applications…"
                    aria-label="Filter by application"
                    value={query}
                    onChange={e => setQuery(e.target.value)}
                />
                {isSearching && (
                    <div className="absolute z-10 mt-1 w-full max-h-56 overflow-y-auto rounded-md border bg-popover shadow-md">
                        {isLoading && <p className="px-3 py-2 text-sm text-muted-foreground">Searching…</p>}
                        {!isLoading && options.length === 0 && (
                            <p className="px-3 py-2 text-sm text-muted-foreground">No applications found</p>
                        )}
                        {options.map(option => (
                            <button
                                key={option.value}
                                type="button"
                                className="block w-full px-3 py-2 text-left text-sm hover:bg-muted"
                                onClick={() => handleSelect(option)}
                            >
                                {option.label}
                            </button>
                        ))}
                    </div>
                )}
            </div>

            {selectedIds.length > 0 && (
                <div className="flex flex-wrap gap-1.5">
                    {selectedIds.map(id => (
                        <Badge key={id} variant="secondary" className="gap-1 pr-1">
                            {labels[id] ?? id}
                            <Button
                                type="button"
                                variant="ghost"
                                size="icon"
                                className="size-4"
                                aria-label={`Remove ${labels[id] ?? id}`}
                                onClick={() => onChange(selectedIds.filter(selectedId => selectedId !== id))}
                            >
                                <XIcon className="size-3" aria-hidden />
                            </Button>
                        </Badge>
                    ))}
                </div>
            )}
        </div>
    );
}
