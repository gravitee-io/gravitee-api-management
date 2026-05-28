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
import { orderToSort, sortToOrder, toSortableTimestamp } from './tableSort';

describe('tableSort', () => {
    it('converts sorting state to API order', () => {
        expect(sortToOrder([{ id: 'name', desc: false }])).toBe('name');
        expect(sortToOrder([{ id: 'updated_at', desc: true }])).toBe('-updated_at');
    });

    it('converts API order to sorting state', () => {
        expect(orderToSort('name', [{ id: 'updated_at', desc: true }])).toEqual([{ id: 'name', desc: false }]);
        expect(orderToSort('-updated_at', [{ id: 'name', desc: false }])).toEqual([{ id: 'updated_at', desc: true }]);
    });

    it('parses timestamps for column sorting', () => {
        expect(toSortableTimestamp('2024-01-02T00:00:00.000Z')).toBeGreaterThan(toSortableTimestamp('2024-01-01T00:00:00.000Z'));
        expect(toSortableTimestamp(undefined)).toBe(0);
    });
});
