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

import { toSharedPolicyGroupHistoriesSortByParam, toSharedPolicyGroupHistoryRowId } from './sharedPolicyGroupHistoriesSort';

describe('sharedPolicyGroupHistoriesSort', () => {
    describe('toSharedPolicyGroupHistoriesSortByParam', () => {
        it('maps ascending version like classic Console', () => {
            expect(toSharedPolicyGroupHistoriesSortByParam([{ id: 'version', desc: false }])).toBe('version');
        });

        it('maps descending deployedAt with a leading dash', () => {
            expect(toSharedPolicyGroupHistoriesSortByParam([{ id: 'deployedAt', desc: true }])).toBe('-deployedAt');
        });

        it('returns undefined for empty sorting', () => {
            expect(toSharedPolicyGroupHistoriesSortByParam([])).toBeUndefined();
        });

        it('returns undefined for unsupported column ids', () => {
            expect(toSharedPolicyGroupHistoriesSortByParam([{ id: 'name', desc: false }])).toBeUndefined();
        });
    });

    describe('toSharedPolicyGroupHistoryRowId', () => {
        it('combines version and updatedAt like classic Console page table ids', () => {
            expect(toSharedPolicyGroupHistoryRowId({ version: 3, updatedAt: '2024-01-01T00:00:00.000Z' })).toBe(
                '3-2024-01-01T00:00:00.000Z',
            );
        });
    });
});
