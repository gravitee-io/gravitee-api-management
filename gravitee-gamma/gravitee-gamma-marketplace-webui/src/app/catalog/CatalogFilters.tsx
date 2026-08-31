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
    Input,
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
    ToggleGroup,
    ToggleGroupItem,
} from '@gravitee/graphene-core';
import { LayoutGridIcon, ListIcon } from '@gravitee/graphene-core/icons';
import { useEffect, useState, type KeyboardEvent } from 'react';

import { hasCatalogFilters, type CatalogParams, type CatalogView } from './catalog-params';
import { PROTOCOL_OPTIONS } from './protocol';
import type { Category } from '../../api/types';

const ALL_VALUE = 'all';

interface CatalogFiltersProps {
    readonly params: CatalogParams;
    readonly categories: readonly Category[];
    readonly labels: readonly string[];
    readonly onChange: (patch: Partial<CatalogParams>) => void;
}

export function CatalogFilters({ params, categories, labels, onChange }: CatalogFiltersProps) {
    const [query, setQuery] = useState(params.query);
    const filtersActive = hasCatalogFilters(params);

    useEffect(() => {
        setQuery(params.query);
    }, [params.query]);

    useEffect(() => {
        const timeout = window.setTimeout(() => {
            if (query.trim() !== params.query) {
                onChange({ query: query.trim() });
            }
        }, 300);
        return () => window.clearTimeout(timeout);
    }, [onChange, params.query, query]);

    const submitQuery = () => {
        const trimmed = query.trim();
        if (trimmed !== params.query) {
            onChange({ query: trimmed });
        }
    };

    const handleSearchKeyDown = (event: KeyboardEvent<HTMLInputElement>) => {
        if (event.key === 'Enter') {
            event.preventDefault();
            submitQuery();
        }
    };

    const labelOptions = labels.includes(params.label) || !params.label ? labels : [params.label, ...labels];

    return (
        <div className="flex flex-col gap-3">
            <div className="flex flex-wrap items-center justify-between gap-3">
                <div className="w-full max-w-sm">
                    <label htmlFor="catalog-search" className="sr-only">
                        Search catalog
                    </label>
                    <Input
                        id="catalog-search"
                        type="search"
                        placeholder="Search agents..."
                        value={query}
                        onChange={event => setQuery(event.target.value)}
                        onKeyDown={handleSearchKeyDown}
                        className="h-8"
                    />
                </div>
                <ToggleGroup
                    type="single"
                    value={params.view}
                    onValueChange={value => value && onChange({ view: value as CatalogView })}
                    aria-label="Catalog view"
                >
                    <ToggleGroupItem value="grid" aria-label="Grid">
                        <LayoutGridIcon className="size-4" aria-hidden />
                    </ToggleGroupItem>
                    <ToggleGroupItem value="list" aria-label="List">
                        <ListIcon className="size-4" aria-hidden />
                    </ToggleGroupItem>
                </ToggleGroup>
            </div>
            <div className="flex flex-wrap items-center gap-2">
                <Select value={params.protocol || ALL_VALUE} onValueChange={value => onChange({ protocol: value === ALL_VALUE ? '' : value })}>
                    <SelectTrigger className="h-8 w-40" aria-label="Protocol">
                        <SelectValue placeholder="Protocol" />
                    </SelectTrigger>
                    <SelectContent>
                        <SelectItem value={ALL_VALUE}>All</SelectItem>
                        {PROTOCOL_OPTIONS.map(option => (
                            <SelectItem key={option.value} value={option.value}>
                                {option.label}
                            </SelectItem>
                        ))}
                    </SelectContent>
                </Select>
                <Select value={params.label || ALL_VALUE} onValueChange={value => onChange({ label: value === ALL_VALUE ? '' : value })}>
                    <SelectTrigger className="h-8 w-40" aria-label="Labels">
                        <SelectValue placeholder="Labels" />
                    </SelectTrigger>
                    <SelectContent>
                        <SelectItem value={ALL_VALUE}>All</SelectItem>
                        {labelOptions.map(label => (
                            <SelectItem key={label} value={label}>
                                {label}
                            </SelectItem>
                        ))}
                    </SelectContent>
                </Select>
                <Select
                    value={params.category || ALL_VALUE}
                    onValueChange={value => onChange({ category: value === ALL_VALUE ? '' : value })}
                >
                    <SelectTrigger className="h-8 w-40" aria-label="Category">
                        <SelectValue placeholder="Category" />
                    </SelectTrigger>
                    <SelectContent>
                        <SelectItem value={ALL_VALUE}>All</SelectItem>
                        {categories.map(category => (
                            <SelectItem key={category.id} value={category.id}>
                                {category.name ?? category.id}
                            </SelectItem>
                        ))}
                    </SelectContent>
                </Select>
                {filtersActive ? (
                    <Button
                        type="button"
                        variant="ghost"
                        size="sm"
                        onClick={() => onChange({ query: '', category: '', protocol: '', label: '' })}
                    >
                        Reset
                    </Button>
                ) : null}
            </div>
        </div>
    );
}
