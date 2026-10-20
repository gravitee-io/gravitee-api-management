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

import { useExpandedDocumentationFolders } from './useExpandedDocumentationFolders';

const ids = (expandedIds: ReadonlySet<string>) => [...expandedIds].sort();

describe('useExpandedDocumentationFolders', () => {
    beforeEach(() => {
        sessionStorage.clear();
        jest.restoreAllMocks();
    });

    it('starts with every folder collapsed', () => {
        const { result } = renderHook(() => useExpandedDocumentationFolders('api-1'));

        expect(result.current.expandedIds.size).toBe(0);
    });

    it('expands a collapsed folder and collapses an expanded one', () => {
        const { result } = renderHook(() => useExpandedDocumentationFolders('api-1'));

        act(() => result.current.toggle('guides'));
        expect(ids(result.current.expandedIds)).toEqual(['guides']);

        act(() => result.current.toggle('guides'));
        expect(result.current.expandedIds.size).toBe(0);
    });

    it('expands folders without collapsing the ones already open', () => {
        const { result } = renderHook(() => useExpandedDocumentationFolders('api-1'));

        act(() => result.current.expand('guides'));
        act(() => result.current.expand('guides'));
        act(() => result.current.expand('reference'));

        expect(ids(result.current.expandedIds)).toEqual(['guides', 'reference']);
    });

    it('remembers the open folders of an API when its documentation is shown again', () => {
        const first = renderHook(() => useExpandedDocumentationFolders('api-1'));
        act(() => first.result.current.toggle('guides'));
        first.unmount();

        const again = renderHook(() => useExpandedDocumentationFolders('api-1'));

        expect(ids(again.result.current.expandedIds)).toEqual(['guides']);
    });

    it('remembers the open folders of each API apart', () => {
        const first = renderHook(() => useExpandedDocumentationFolders('api-1'));
        act(() => first.result.current.toggle('guides'));

        const other = renderHook(() => useExpandedDocumentationFolders('api-2'));

        expect(other.result.current.expandedIds.size).toBe(0);
    });

    it('ignores a remembered value it cannot read', () => {
        const first = renderHook(() => useExpandedDocumentationFolders('api-1'));
        act(() => first.result.current.toggle('guides'));
        first.unmount();
        const key = sessionStorage.key(0);
        if (!key) throw new Error('Nothing was remembered');
        sessionStorage.setItem(key, '{"not": "a list"}');

        const again = renderHook(() => useExpandedDocumentationFolders('api-1'));

        expect(again.result.current.expandedIds.size).toBe(0);
    });

    it('still expands folders when the browser refuses storage', () => {
        jest.spyOn(Storage.prototype, 'getItem').mockImplementation(() => {
            throw new Error('SecurityError');
        });
        jest.spyOn(Storage.prototype, 'setItem').mockImplementation(() => {
            throw new Error('QuotaExceededError');
        });
        const { result } = renderHook(() => useExpandedDocumentationFolders('api-1'));

        act(() => result.current.toggle('guides'));

        expect(ids(result.current.expandedIds)).toEqual(['guides']);
    });
});
