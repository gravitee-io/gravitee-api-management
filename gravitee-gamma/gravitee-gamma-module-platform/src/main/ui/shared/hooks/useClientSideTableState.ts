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
import { useDeferredValue, useEffect, useMemo, useState } from 'react';

import {
    clampPage,
    CLIENT_SIDE_TABLE_DEFAULT_PAGE_SIZE,
    filterClientSideTableItems,
    normalizeClientSideTablePageSize,
    paginateClientSideTableItems,
} from '../utils/clientSideTableUtils';

interface UseClientSideTableStateOptions<T> {
    /** When this value changes, search and pagination reset to their defaults. */
    readonly resetWhen?: unknown;
    /** Avoids generic field scanning when a table has a known search target. */
    readonly matchesSearch?: (item: T, normalizedSearch: string) => boolean;
}

export function useClientSideTableState<T extends object>(
    items: readonly T[],
    searchIgnoreKeys: readonly string[],
    options: UseClientSideTableStateOptions<T> = {},
) {
    const [search, setSearch] = useState('');
    const [page, setPage] = useState(1);
    const [pageSize, setPageSize] = useState(CLIENT_SIDE_TABLE_DEFAULT_PAGE_SIZE);
    const deferredSearch = useDeferredValue(search);

    useEffect(() => {
        setSearch('');
        setPage(1);
        setPageSize(CLIENT_SIDE_TABLE_DEFAULT_PAGE_SIZE);
    }, [options.resetWhen]);

    const filteredItems = useMemo(() => {
        const normalizedSearch = deferredSearch.trim().toLowerCase();
        const matchesSearch = options.matchesSearch;
        if (!normalizedSearch || !matchesSearch) {
            return filterClientSideTableItems(items, normalizedSearch, searchIgnoreKeys);
        }
        return items.filter(item => matchesSearch(item, normalizedSearch));
    }, [deferredSearch, items, options.matchesSearch, searchIgnoreKeys]);
    const totalCount = filteredItems.length;
    const currentPage = clampPage(page, totalCount, pageSize);
    const paginatedItems = useMemo(
        () => paginateClientSideTableItems(filteredItems, currentPage, pageSize),
        [filteredItems, currentPage, pageSize],
    );
    const hasActiveSearch = deferredSearch.trim().length > 0;

    useEffect(() => {
        setPage(previous => clampPage(previous, totalCount, pageSize));
    }, [totalCount, pageSize]);

    function handleSearchChange(value: string) {
        setSearch(value);
        setPage(1);
    }

    function handlePageSizeChange(size: number) {
        setPageSize(normalizeClientSideTablePageSize(size));
        setPage(1);
    }

    return {
        search,
        page: currentPage,
        pageSize,
        totalCount,
        paginatedItems,
        hasActiveSearch,
        handleSearchChange,
        handlePageSizeChange,
        setPage,
    };
}
