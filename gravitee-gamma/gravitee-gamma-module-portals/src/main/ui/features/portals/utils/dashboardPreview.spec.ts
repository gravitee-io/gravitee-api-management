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
import { hasMorePortals } from './dashboardPreview';
import { createPortal, createPortalsResponse } from '../../../testing/factories';

describe('dashboard preview', () => {
    it.each([
        [1, 3, false],
        [3, 3, false],
        [4, 3, true],
        [5, 5, false],
        [6, 5, true],
    ])('uses the matching total %s for the visible limit %s', (totalCount, limit, expected) => {
        expect(hasMorePortals(createPortalsResponse({ pagination: { totalCount } }), limit)).toBe(expected);
    });

    it.each([undefined, -1])('uses received items when the total is %s', totalCount => {
        const data = Array.from({ length: 6 }, (_, index) => createPortal({ id: String(index) }));
        expect(hasMorePortals(createPortalsResponse({ data, pagination: { totalCount } }), 5)).toBe(true);
    });

    it('uses next-page evidence when the response does not provide a total', () => {
        expect(hasMorePortals(createPortalsResponse({ pagination: {}, links: { next: '/portals?page=2' } }), 5)).toBe(true);
        expect(hasMorePortals(createPortalsResponse({ pagination: { page: 1, pageCount: 2 } }), 5)).toBe(true);
    });

    it('does not invent extra results when the total is unknown', () => {
        expect(hasMorePortals(createPortalsResponse({ pagination: { totalCount: -1 } }), 5)).toBe(false);
        expect(hasMorePortals(createPortalsResponse({ data: [], pagination: {} }), 3)).toBe(false);
    });
});
