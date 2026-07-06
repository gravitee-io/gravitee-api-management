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
import { mergeV4ProxiesWithScores } from './apiScore';
import type { ApiListItem } from '../../apis/types';
import type { EnvironmentApiScore } from '../types/apiScore';

describe('mergeV4ProxiesWithScores', () => {
    const proxy = (over: Partial<ApiListItem> & Pick<ApiListItem, 'id' | 'name'>): ApiListItem => ({
        apiVersion: '1.0',
        type: 'PROXY',
        definitionVersion: 'V4',
        ...over,
    });

    const scored = (over: Partial<EnvironmentApiScore> & Pick<EnvironmentApiScore, 'id' | 'name'>): EnvironmentApiScore => ({
        pictureUrl: '',
        ...over,
    });

    it('returns every V4 proxy with score fields merged from the scoring list', () => {
        const result = mergeV4ProxiesWithScores(
            [proxy({ id: 'a', name: 'Alpha', _links: { pictureUrl: 'https://cdn/a.png' } }), proxy({ id: 'b', name: 'Beta' })],
            [scored({ id: 'a', name: 'Alpha-old', pictureUrl: 'legacy', score: 0.8, errors: 2 })],
        );

        expect(result).toEqual([
            { id: 'a', name: 'Alpha', pictureUrl: 'https://cdn/a.png', score: 0.8, errors: 2 },
            { id: 'b', name: 'Beta', pictureUrl: '', score: undefined, errors: undefined },
        ]);
    });

    it('ignores scored rows for APIs outside the V4 proxy catalog', () => {
        const result = mergeV4ProxiesWithScores(
            [proxy({ id: 'a', name: 'Alpha' })],
            [scored({ id: 'legacy-v2', name: 'Legacy', score: 0.5 })],
        );

        expect(result).toHaveLength(1);
        expect(result[0].score).toBeUndefined();
    });
});
