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
import { filterShardingTags, toShardingTagRows } from './shardingTags';
import type { OrgGroup, OrgTag, ShardingTagRow } from '../types/entrypoint';

describe('shardingTags utils', () => {
    const groups: OrgGroup[] = [
        { id: 'g1', name: 'Admins' },
        { id: 'g2', name: 'Developers' },
    ];

    describe('toShardingTagRows', () => {
        it('maps tags to rows and resolves restricted group names', () => {
            const tags: OrgTag[] = [
                { id: 't1', key: 'prod', name: 'Production', description: 'Prod tag', restricted_groups: ['g1', 'missing'] },
            ];

            expect(toShardingTagRows(tags, groups)).toEqual([
                {
                    id: 't1',
                    key: 'prod',
                    name: 'Production',
                    description: 'Prod tag',
                    restrictedGroupIds: ['g1', 'missing'],
                    restrictedGroupNames: ['Admins', 'missing'],
                },
            ]);
        });

        it('defaults description and restricted groups when omitted', () => {
            const tags: OrgTag[] = [{ id: 't2', key: 'edge', name: 'Edge' }];
            const rows = toShardingTagRows(tags, groups);
            expect(rows[0]?.description).toBe('');
            expect(rows[0]?.restrictedGroupIds).toEqual([]);
            expect(rows[0]?.restrictedGroupNames).toEqual([]);
        });
    });

    describe('filterShardingTags', () => {
        const rows: ShardingTagRow[] = [
            { id: 't1', key: 'prod', name: 'Production', description: 'Prod tag', restrictedGroupIds: [], restrictedGroupNames: [] },
            { id: 't2', key: 'edge', name: 'Edge zone', description: 'Edge locations', restrictedGroupIds: [], restrictedGroupNames: [] },
        ];

        it('returns all rows when query is empty', () => {
            expect(filterShardingTags(rows, '')).toEqual(rows);
            expect(filterShardingTags(rows, '   ')).toEqual(rows);
        });

        it('filters by key', () => {
            expect(filterShardingTags(rows, 'prod')).toEqual([rows[0]]);
        });

        it('filters by name case-insensitively', () => {
            expect(filterShardingTags(rows, 'EDGE ZONE')).toEqual([rows[1]]);
        });

        it('filters by description', () => {
            expect(filterShardingTags(rows, 'locations')).toEqual([rows[1]]);
        });

        it('returns empty array when nothing matches', () => {
            expect(filterShardingTags(rows, 'nomatch')).toEqual([]);
        });
    });
});
