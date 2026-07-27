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
import { entrypointTargetLabel, toEntrypointMappingRows } from './entrypointMappings';
import type { Entrypoint, OrgEnvironment, OrgTag } from '../types/entrypoint';

describe('entrypointMappings utils', () => {
    const environments: OrgEnvironment[] = [
        { id: 'env-1', name: 'Production' },
        { id: 'env-2', name: 'Development' },
    ];
    const tags: OrgTag[] = [
        { id: 't1', key: 'prod', name: 'Production tag' },
        { id: 't2', key: 'edge', name: 'Edge' },
    ];

    it('maps target labels including Kafka display name', () => {
        expect(entrypointTargetLabel('HTTP')).toBe('HTTP');
        expect(entrypointTargetLabel('TCP')).toBe('TCP');
        expect(entrypointTargetLabel('KAFKA')).toBe('Kafka');
        expect(entrypointTargetLabel(undefined)).toBe('—');
    });

    it('joins tag and environment names for display rows', () => {
        const entrypoints: Entrypoint[] = [
            {
                id: 'ep-1',
                value: 'https://api.example.com',
                target: 'HTTP',
                tags: ['prod', 'missing'],
                environmentIds: ['env-1'],
            },
        ];

        const rows = toEntrypointMappingRows(entrypoints, environments, tags);
        expect(rows).toEqual([
            {
                id: 'ep-1',
                value: 'https://api.example.com',
                target: 'HTTP',
                targetLabel: 'HTTP',
                tags: ['prod', 'missing'],
                tagsName: ['Production tag', 'missing'],
                environmentIds: ['env-1'],
                environmentNames: ['Production'],
            },
        ]);
    });

    it('uses empty arrays when tags and environments are omitted', () => {
        const rows = toEntrypointMappingRows([{ id: 'ep-2', value: '4082', target: 'TCP' }], environments, tags);
        expect(rows[0]?.tagsName).toEqual([]);
        expect(rows[0]?.environmentNames).toEqual([]);
    });
});
