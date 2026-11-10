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
import { getListPagination } from './listPagination';
import { createPortalsResponse } from '../../../testing/factories';

describe('getListPagination', () => {
    it('uses the API page count before deriving one from the total', () => {
        const response = createPortalsResponse({ pagination: { page: 2, perPage: 10, pageCount: 4, totalCount: 999 } });
        expect(getListPagination(response, 2, 10)).toEqual({ page: 2, pageCount: 4, hasPrevious: true, hasNext: true });
    });

    it.each([0, 1, 10, 11, 25])('derives the matching page count from a known total (%s)', totalCount => {
        const response = createPortalsResponse({ pagination: { totalCount } });
        expect(getListPagination(response, 1, 10)).toEqual({
            page: 1,
            pageCount: Math.ceil(totalCount / 10),
            hasPrevious: false,
            hasNext: totalCount > 10,
        });
    });

    it.each([undefined, -1])('uses explicit links without inventing totals when the count is unknown (%s)', totalCount => {
        const response = createPortalsResponse({ pagination: { totalCount }, links: { next: 'https://api.test/next' } });
        expect(getListPagination(response, 2, 10)).toEqual({ page: 2, pageCount: undefined, hasPrevious: true, hasNext: true });
        expect(getListPagination({ ...response, links: undefined }, 2, 10).hasNext).toBe(false);
    });

    it('uses pageCount even when totalCount is unknown', () => {
        const response = createPortalsResponse({ pagination: { page: 3, pageCount: 3, totalCount: -1 } });
        expect(getListPagination(response, 3, 10)).toEqual({ page: 3, pageCount: 3, hasPrevious: true, hasNext: false });
    });

    it('does not infer another page from a full response or invalid metadata', () => {
        const response = createPortalsResponse({ pagination: { pageCount: -1, totalCount: -1 } });
        expect(getListPagination(response, 1, 10)).toEqual({ page: 1, pageCount: undefined, hasPrevious: false, hasNext: false });
    });
});
