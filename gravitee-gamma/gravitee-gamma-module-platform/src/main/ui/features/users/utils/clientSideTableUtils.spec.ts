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
import { clampPage, filterClientSideTableItems, paginateClientSideTableItems } from '../../../shared/utils/clientSideTableUtils';

describe('clientSideTableUtils', () => {
    const items = [
        { id: 'api-1', name: 'Payments API', visibility: 'PUBLIC' },
        { id: 'api-2', name: 'Orders API', visibility: 'PRIVATE' },
    ];

    it('filters rows by searchable fields and ignores configured keys', () => {
        expect(filterClientSideTableItems(items, 'payment')).toEqual([items[0]]);
        expect(filterClientSideTableItems(items, 'orders', ['id', 'visibility'])).toEqual([items[1]]);
    });

    it('does not match ignored fields', () => {
        expect(filterClientSideTableItems(items, 'api-1', ['id', 'visibility'])).toEqual([]);
        expect(filterClientSideTableItems(items, 'private', ['id', 'visibility'])).toEqual([]);
    });

    it('paginates rows for the requested page', () => {
        expect(paginateClientSideTableItems(items, 1, 1)).toEqual([items[0]]);
        expect(paginateClientSideTableItems(items, 2, 1)).toEqual([items[1]]);
    });

    it('clamps page numbers to the available page count', () => {
        expect(clampPage(3, 12, 10)).toBe(2);
        expect(clampPage(0, 12, 10)).toBe(1);
    });

    it.each([0, -1, Number.NaN, Number.POSITIVE_INFINITY])('uses the default page size when %s is invalid', pageSize => {
        expect(clampPage(2, 12, pageSize)).toBe(2);
        expect(paginateClientSideTableItems(items, 1, pageSize)).toEqual(items);
    });
});
