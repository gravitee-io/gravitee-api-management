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
import { clampPage, filterInheritedResources, paginateInheritedResources } from './userInheritedResources';

describe('userInheritedResources', () => {
    const items = [
        { id: 'api-1', name: 'Payment API', version: '1', visibility: 'PRIVATE' },
        { id: 'api-2', name: 'Orders API', version: '2', visibility: 'PUBLIC' },
    ];

    it('filters inherited resources by searchable fields', () => {
        expect(filterInheritedResources(items, 'payment')).toEqual([items[0]]);
        expect(filterInheritedResources(items, '2', ['id', 'visibility'])).toEqual([items[1]]);
    });

    it('ignores configured keys when filtering', () => {
        expect(filterInheritedResources(items, 'api-1', ['id', 'visibility'])).toEqual([]);
        expect(filterInheritedResources(items, 'private', ['id', 'visibility'])).toEqual([]);
    });

    it('paginates filtered results', () => {
        expect(paginateInheritedResources(items, 1, 1)).toEqual([items[0]]);
        expect(paginateInheritedResources(items, 2, 1)).toEqual([items[1]]);
    });

    it('clamps page when results shrink', () => {
        expect(clampPage(3, 12, 10)).toBe(2);
        expect(clampPage(5, 0, 10)).toBe(1);
        expect(clampPage(1, 25, 10)).toBe(1);
    });
});
