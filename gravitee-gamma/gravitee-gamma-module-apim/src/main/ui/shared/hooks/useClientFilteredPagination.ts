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

import { useEffect, useMemo, useState } from 'react';

export const CLIENT_TABLE_PAGE_SIZE_OPTIONS = [10, 25, 50, 100] as const;

const DEFAULT_PAGE_SIZE = CLIENT_TABLE_PAGE_SIZE_OPTIONS[0];

/**
 * Client search + pagination over an in-memory collection.
 * Pass the current page slice to DataTable with `serverSide` + controlled `pagination`
 *
 * `matchesSearch` receives a trimmed, lowercased query (empty query skips filtering).
 */
export function useClientFilteredPagination<T>(items: T[], matchesSearch: (item: T, query: string) => boolean) {
    const [search, setSearch] = useState('');
    const [page, setPage] = useState(1);
    const [pageSize, setPageSize] = useState<number>(DEFAULT_PAGE_SIZE);

    const filtered = useMemo(() => {
        const query = search.trim().toLowerCase();
        if (!query) return items;
        return items.filter(item => matchesSearch(item, query));
    }, [items, matchesSearch, search]);

    const totalCount = filtered.length;
    const pageCount = Math.max(1, Math.ceil(totalCount / pageSize) || 1);
    const safePage = Math.min(page, pageCount);

    useEffect(() => {
        if (page !== safePage) setPage(safePage);
    }, [page, safePage]);

    const pageData = useMemo(() => {
        const start = (safePage - 1) * pageSize;
        return filtered.slice(start, start + pageSize);
    }, [filtered, safePage, pageSize]);

    function handleSearchChange(value: string) {
        setSearch(value);
        setPage(1);
    }

    function handlePageSizeChange(size: number) {
        setPageSize(size);
        setPage(1);
    }

    return {
        search,
        page: safePage,
        pageSize,
        totalCount,
        pageData,
        handleSearchChange,
        setPage,
        handlePageSizeChange,
    };
}
