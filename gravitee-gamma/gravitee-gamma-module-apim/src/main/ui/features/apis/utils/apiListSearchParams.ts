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
    API_LIST_PAGE_SIZE_OPTIONS,
    apiListFilterSelectionsEqual,
    EMPTY_API_LIST_FILTERS,
    sanitizeApiListFilters,
    type ApiListFilterSelection,
} from '../components/list/apiListFilters';

export const API_LIST_DEFAULT_PAGE = 1;
/** Matches Classic Console API list default (`api-list.component.ts` pagination.size). */
export const API_LIST_DEFAULT_PER_PAGE = 25;

export interface ApiListUrlState {
    query: string;
    page: number;
    perPage: number;
    order?: string;
    filters: ApiListFilterSelection;
}

export interface ParseApiListSearchParamsOptions {
    includeFederated?: boolean;
    /** Default true. Set false until the federation gate has resolved so `FEDERATED` is not stripped early. */
    sanitizeApiTypes?: boolean;
}

function parsePositiveInt(value: string | null, fallback: number): number {
    if (!value) return fallback;
    const parsed = Number(value);
    return Number.isFinite(parsed) && parsed > 0 ? parsed : fallback;
}

function parseAllowedPageSize(value: string | null): number {
    const parsed = parsePositiveInt(value, API_LIST_DEFAULT_PER_PAGE);
    return (API_LIST_PAGE_SIZE_OPTIONS as readonly number[]).includes(parsed) ? parsed : API_LIST_DEFAULT_PER_PAGE;
}

function parseMultiValueParam(params: URLSearchParams, key: string): string[] {
    const values = params.getAll(key);
    if (values.length === 0) return [];
    return values
        .flatMap(v => v.split(','))
        .map(s => s.trim())
        .filter(Boolean);
}

function appendMulti(params: URLSearchParams, key: string, values: readonly string[]) {
    const sorted = [...values].sort();
    sorted.forEach(value => params.append(key, value));
}

/** Reads list state from the URL (classic Console: q, page, size, order, apiTypes, statuses, tags, categories). */
export function parseApiListSearchParams(params: URLSearchParams, options?: ParseApiListSearchParamsOptions): ApiListUrlState {
    const order = params.get('order');
    const rawFilters: ApiListFilterSelection = {
        apiTypes: parseMultiValueParam(params, 'apiTypes'),
        statuses: parseMultiValueParam(params, 'statuses'),
        tags: parseMultiValueParam(params, 'tags'),
        categories: parseMultiValueParam(params, 'categories'),
    };

    return {
        query: params.get('q') ?? '',
        page: parsePositiveInt(params.get('page'), API_LIST_DEFAULT_PAGE),
        perPage: parseAllowedPageSize(params.get('size')),
        order: order || undefined,
        filters: sanitizeApiListFilters(rawFilters, {
            includeFederated: options?.includeFederated ?? false,
            sanitizeApiTypes: options?.sanitizeApiTypes ?? true,
        }),
    };
}

/** Writes list state to the URL; omits default values to keep URLs short. */
export function buildApiListSearchParams(state: ApiListUrlState): URLSearchParams {
    const next = new URLSearchParams();

    if (state.query) next.set('q', state.query);
    if (state.page > API_LIST_DEFAULT_PAGE) next.set('page', String(state.page));
    if (state.perPage !== API_LIST_DEFAULT_PER_PAGE) next.set('size', String(state.perPage));
    if (state.order) next.set('order', state.order);

    appendMulti(next, 'apiTypes', state.filters.apiTypes);
    appendMulti(next, 'statuses', state.filters.statuses);
    appendMulti(next, 'tags', state.filters.tags);
    appendMulti(next, 'categories', state.filters.categories);

    return next;
}

export function emptyApiListUrlState(): ApiListUrlState {
    return {
        query: '',
        page: API_LIST_DEFAULT_PAGE,
        perPage: API_LIST_DEFAULT_PER_PAGE,
        filters: { ...EMPTY_API_LIST_FILTERS },
    };
}

/** Compares parsed list state (ignores URL param ordering). */
export function apiListUrlStatesEqual(a: ApiListUrlState, b: ApiListUrlState): boolean {
    return (
        a.query === b.query &&
        a.page === b.page &&
        a.perPage === b.perPage &&
        (a.order ?? undefined) === (b.order ?? undefined) &&
        apiListFilterSelectionsEqual(a.filters, b.filters)
    );
}

export function urlSearchParamsEqual(a: URLSearchParams, b: URLSearchParams, options?: ParseApiListSearchParamsOptions): boolean {
    return apiListUrlStatesEqual(parseApiListSearchParams(a, options), parseApiListSearchParams(b, options));
}
