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
import { Label } from '@gravitee/graphene-core';
import { useEffect, useId, useMemo, type ReactNode } from 'react';

import {
    API_STATUS_FILTER_OPTIONS,
    API_TYPE_FILTER_OPTIONS,
    apiListFilterSelectionsEqual,
    FEDERATED_API_TYPE_OPTION,
    sanitizeApiListFilters,
    type ApiListFilterSelection,
} from './apiListFilters';
import { MultiSelectFilter, type MultiSelectFilterOption } from '../../../../shared/components';
import { useEnvCategories } from '../../hooks/useEnvCategories';
import { useOrgTags } from '../../hooks/useOrgTags';

interface ApiListFilterBarProps {
    readonly filters: ApiListFilterSelection;
    readonly onChange: (filters: ApiListFilterSelection) => void;
    readonly includeFederated?: boolean;
    readonly disabled?: boolean;
}

export function ApiListFilterBar({ filters, onChange, includeFederated = false, disabled = false }: ApiListFilterBarProps) {
    const { data: orgTags, isFetched: tagsFetched } = useOrgTags();
    const { data: categories, isFetched: categoriesFetched } = useEnvCategories();

    const typeOptions = includeFederated ? [...API_TYPE_FILTER_OPTIONS, FEDERATED_API_TYPE_OPTION] : API_TYPE_FILTER_OPTIONS;
    const tagOptions = useMemo<MultiSelectFilterOption[]>(
        () => (orgTags ?? []).map(tag => ({ value: tag.key, label: tag.name || tag.key })),
        [orgTags],
    );
    const categoryOptions = useMemo<MultiSelectFilterOption[]>(
        () => (categories ?? []).map(category => ({ value: category.key, label: category.name })),
        [categories],
    );

    const tagKeys = tagsFetched ? (orgTags ?? []).map(tag => tag.key) : undefined;
    const categoryKeys = categoriesFetched ? (categories ?? []).map(category => category.key) : undefined;

    useEffect(() => {
        const sanitized = sanitizeApiListFilters(filters, { includeFederated, tagKeys, categoryKeys });
        if (!apiListFilterSelectionsEqual(filters, sanitized)) {
            onChange(sanitized);
        }
    }, [categoryKeys, filters, includeFederated, onChange, tagKeys]);

    const typeFilterId = useId();
    const statusFilterId = useId();
    const tagsFilterId = useId();
    const categoriesFilterId = useId();

    return (
        <>
            <FilterField label="API Type" controlId={typeFilterId}>
                <MultiSelectFilter
                    id={typeFilterId}
                    placeholder="All types"
                    ariaLabel="Filter by API type"
                    options={typeOptions}
                    selectedValues={filters.apiTypes}
                    onSelectedValuesChange={apiTypes => onChange({ ...filters, apiTypes })}
                    disabled={disabled}
                    className="w-[180px]"
                />
            </FilterField>
            <FilterField label="API Status" controlId={statusFilterId}>
                <MultiSelectFilter
                    id={statusFilterId}
                    placeholder="All statuses"
                    ariaLabel="Filter by API status"
                    options={API_STATUS_FILTER_OPTIONS}
                    selectedValues={filters.statuses}
                    onSelectedValuesChange={statuses => onChange({ ...filters, statuses })}
                    disabled={disabled}
                    className="w-[160px]"
                />
            </FilterField>
            <FilterField label="Sharding Tags" controlId={tagsFilterId}>
                <MultiSelectFilter
                    id={tagsFilterId}
                    placeholder="All tags"
                    ariaLabel="Filter by sharding tags"
                    options={tagOptions}
                    selectedValues={filters.tags}
                    onSelectedValuesChange={tags => onChange({ ...filters, tags })}
                    emptyMessage="No sharding tags"
                    disabled={disabled}
                    className="w-[180px]"
                />
            </FilterField>
            <FilterField label="Categories" controlId={categoriesFilterId}>
                <MultiSelectFilter
                    id={categoriesFilterId}
                    placeholder="All categories"
                    ariaLabel="Filter by categories"
                    options={categoryOptions}
                    selectedValues={filters.categories}
                    onSelectedValuesChange={nextCategories => onChange({ ...filters, categories: nextCategories })}
                    emptyMessage="No categories"
                    disabled={disabled}
                    className="w-[180px]"
                />
            </FilterField>
        </>
    );
}

function FilterField({ label, controlId, children }: { label: string; controlId: string; children: ReactNode }) {
    return (
        <div className="space-y-1.5">
            <Label htmlFor={controlId} className="text-xs">
                {label}
            </Label>
            {children}
        </div>
    );
}
