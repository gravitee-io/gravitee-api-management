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

export const PRODUCT_RESOURCE_PAGE_SIZE_OPTIONS = [25, 50, 100] as const;

const DEFAULT_PAGE_SIZE = PRODUCT_RESOURCE_PAGE_SIZE_OPTIONS[0];

const ALLOWED_PAGE_SIZES = new Set<number>(PRODUCT_RESOURCE_PAGE_SIZE_OPTIONS);

function normalizePageSize(pageSize: number): number {
    return ALLOWED_PAGE_SIZES.has(pageSize) ? pageSize : DEFAULT_PAGE_SIZE;
}

export function useProductResourceTableState<T>(items: T[], matchesSearch?: (item: T, query: string) => boolean) {
    const [search, setSearch] = useState('');
    const [page, setPage] = useState(1);
    const [pageSize, setPageSize] = useState<number>(DEFAULT_PAGE_SIZE);

    const filtered = useMemo(() => {
        const query = search.trim().toLowerCase();
        if (!query || !matchesSearch) return items;
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
        setPageSize(normalizePageSize(size));
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
