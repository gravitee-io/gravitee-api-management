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

import { useClientSideTableState } from './useClientSideTableState';
import { CLIENT_SIDE_TABLE_DEFAULT_PAGE_SIZE } from '../utils/clientSideTableUtils';

const SEARCH_IGNORE_KEYS = ['id'] as const;

describe('useClientSideTableState', () => {
    const items = Array.from({ length: 12 }, (_, index) => ({
        id: `item-${index + 1}`,
        name: `Item ${index + 1}`,
    }));

    it('paginates and filters client-side data', () => {
        const { result } = renderHook(() => useClientSideTableState(items, SEARCH_IGNORE_KEYS));

        expect(result.current.paginatedItems).toHaveLength(10);
        expect(result.current.totalCount).toBe(12);

        act(() => {
            result.current.setPage(2);
        });

        expect(result.current.page).toBe(2);
        expect(result.current.paginatedItems).toHaveLength(2);

        act(() => {
            result.current.handleSearchChange('Item 11');
        });

        expect(result.current.page).toBe(1);
        expect(result.current.totalCount).toBe(1);
        expect(result.current.paginatedItems[0]?.name).toBe('Item 11');
    });

    it('resets search, page, and page size when resetWhen changes', () => {
        const { result, rerender } = renderHook(({ resetWhen }) => useClientSideTableState(items, SEARCH_IGNORE_KEYS, { resetWhen }), {
            initialProps: { resetWhen: 'env-a' },
        });

        act(() => {
            result.current.handlePageSizeChange(25);
            result.current.setPage(2);
            result.current.handleSearchChange('Item 11');
        });

        expect(result.current.pageSize).toBe(25);
        expect(result.current.page).toBe(1);
        expect(result.current.search).toBe('Item 11');

        rerender({ resetWhen: 'env-b' });

        expect(result.current.search).toBe('');
        expect(result.current.page).toBe(1);
        expect(result.current.pageSize).toBe(CLIENT_SIDE_TABLE_DEFAULT_PAGE_SIZE);
    });

    it('clamps the page when the filtered result set shrinks', () => {
        const { result } = renderHook(() => useClientSideTableState(items, SEARCH_IGNORE_KEYS));

        act(() => {
            result.current.setPage(2);
        });

        expect(result.current.page).toBe(2);

        act(() => {
            result.current.handleSearchChange('Item 11');
        });

        expect(result.current.page).toBe(1);
        expect(result.current.totalCount).toBe(1);
    });
});
