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

import { act, renderHook } from '@testing-library/react';

import { useClientFilteredPagination } from './useClientFilteredPagination';

const ITEMS = Array.from({ length: 25 }, (_, i) => ({ id: `item-${i}`, name: i % 2 === 0 ? `alpha-${i}` : `beta-${i}` }));

describe('useClientFilteredPagination', () => {
    const matchesSearch = (item: (typeof ITEMS)[number], query: string) => item.name.toLowerCase().includes(query);

    it('paginates the full collection by default', () => {
        const { result } = renderHook(() => useClientFilteredPagination(ITEMS, matchesSearch));
        expect(result.current.totalCount).toBe(25);
        expect(result.current.pageData).toHaveLength(10);
        expect(result.current.pageData[0]?.id).toBe('item-0');
    });

    it('filters by search and resets to page 1', () => {
        const { result } = renderHook(() => useClientFilteredPagination(ITEMS, matchesSearch));

        act(() => {
            result.current.setPage(2);
        });
        expect(result.current.page).toBe(2);

        act(() => {
            result.current.handleSearchChange('beta');
        });

        expect(result.current.page).toBe(1);
        expect(result.current.totalCount).toBe(12);
        expect(result.current.pageData.every(item => item.name.startsWith('beta-'))).toBe(true);
    });

    it('changes page size and resets to page 1', () => {
        const { result } = renderHook(() => useClientFilteredPagination(ITEMS, matchesSearch));

        act(() => {
            result.current.setPage(2);
            result.current.handlePageSizeChange(25);
        });

        expect(result.current.page).toBe(1);
        expect(result.current.pageSize).toBe(25);
        expect(result.current.pageData).toHaveLength(25);
    });
});
