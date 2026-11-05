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

import { useProductResourceTableState } from './useProductResourceTableState';

describe('useProductResourceTableState', () => {
    it('clamps the page when the filtered list shrinks below the current page', () => {
        const items = Array.from({ length: 30 }, (_, index) => ({ name: `resource-${index}` }));
        const { result, rerender } = renderHook(
            ({ data }) => useProductResourceTableState(data, (item, query) => item.name.includes(query)),
            { initialProps: { data: items } },
        );

        act(() => {
            result.current.setPage(2);
        });
        expect(result.current.page).toBe(2);

        rerender({ data: items.slice(0, 5) });

        expect(result.current.page).toBe(1);
        expect(result.current.pageData).toHaveLength(5);
    });

    it('ignores page sizes outside the allowed options', () => {
        const items = Array.from({ length: 30 }, (_, index) => ({ name: `resource-${index}` }));
        const { result } = renderHook(() => useProductResourceTableState(items));

        act(() => {
            result.current.handlePageSizeChange(17);
        });

        expect(result.current.pageSize).toBe(25);
    });
});
