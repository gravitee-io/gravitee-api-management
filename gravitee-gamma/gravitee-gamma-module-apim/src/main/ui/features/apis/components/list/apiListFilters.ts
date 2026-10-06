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
import type { MultiSelectFilterOption } from '../../../../shared/components';

/** Matches Classic Console API list (`api-list.component.html` paginationPageSizeOptions). */
export const API_LIST_PAGE_SIZE_OPTIONS = [25, 50, 100, 200] as const;

/** Types this page can list. Labels match the classic Console API list. */
export const API_TYPE_FILTER_OPTIONS: MultiSelectFilterOption[] = [
    { value: 'V4_HTTP_PROXY', label: 'HTTP Proxy' },
    { value: 'V4_TCP_PROXY', label: 'TCP Proxy' },
];

export const FEDERATED_API_TYPE_OPTION: MultiSelectFilterOption = { value: 'FEDERATED', label: 'Federated API' };

export const API_STATUS_FILTER_OPTIONS: MultiSelectFilterOption[] = [
    { value: 'STARTED', label: 'Started' },
    { value: 'STOPPED', label: 'Stopped' },
];

const API_STATUS_VALUES = new Set(API_STATUS_FILTER_OPTIONS.map(option => option.value));

export interface ApiListFilterSelection {
    apiTypes: string[];
    statuses: string[];
    tags: string[];
    categories: string[];
}

export const EMPTY_API_LIST_FILTERS: ApiListFilterSelection = {
    apiTypes: [],
    statuses: [],
    tags: [],
    categories: [],
};

export function allowedApiTypeValues(includeFederated: boolean): ReadonlySet<string> {
    const allowed = new Set(API_TYPE_FILTER_OPTIONS.map(option => option.value));
    if (includeFederated) {
        allowed.add(FEDERATED_API_TYPE_OPTION.value);
    }
    return allowed;
}

export interface SanitizeApiListFiltersContext {
    includeFederated: boolean;
    /** When false, apiTypes are left as-is (used before the federation gate resolves). */
    sanitizeApiTypes?: boolean;
    /** Undefined while tag metadata is still loading; `[]` means the org has no tags. */
    tagKeys?: readonly string[];
    /** Undefined while category metadata is still loading; `[]` means the env has no categories. */
    categoryKeys?: readonly string[];
}

/** Drops URL/UI values outside the filter options (and optional tag/category keys). */
export function sanitizeApiListFilters(filters: ApiListFilterSelection, context: SanitizeApiListFiltersContext): ApiListFilterSelection {
    const allowedTypes = allowedApiTypeValues(context.includeFederated);
    const sanitizeApiTypes = context.sanitizeApiTypes ?? true;
    const tagKeys = context.tagKeys !== undefined ? new Set(context.tagKeys) : undefined;
    const categoryKeys = context.categoryKeys !== undefined ? new Set(context.categoryKeys) : undefined;

    return {
        apiTypes: sanitizeApiTypes ? filters.apiTypes.filter(type => allowedTypes.has(type)) : [...filters.apiTypes],
        statuses: filters.statuses.filter(status => API_STATUS_VALUES.has(status)),
        tags: tagKeys !== undefined ? filters.tags.filter(tag => tagKeys.has(tag)) : filters.tags,
        categories: categoryKeys !== undefined ? filters.categories.filter(category => categoryKeys.has(category)) : filters.categories,
    };
}

export function apiListFilterSelectionsEqual(a: ApiListFilterSelection, b: ApiListFilterSelection): boolean {
    return (
        stringArraysEqualAsSet(a.apiTypes, b.apiTypes) &&
        stringArraysEqualAsSet(a.statuses, b.statuses) &&
        stringArraysEqualAsSet(a.tags, b.tags) &&
        stringArraysEqualAsSet(a.categories, b.categories)
    );
}

function stringArraysEqualAsSet(a: readonly string[], b: readonly string[]): boolean {
    if (a.length !== b.length) {
        return false;
    }
    const sortedA = [...a].sort();
    const sortedB = [...b].sort();
    return sortedA.every((value, index) => value === sortedB[index]);
}

export function hasActiveApiListFilters(filters: ApiListFilterSelection): boolean {
    return filters.apiTypes.length > 0 || filters.statuses.length > 0 || filters.tags.length > 0 || filters.categories.length > 0;
}
