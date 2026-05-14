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
import { isSameApplicationType, normalizeApplicationTypes } from './applicationTypeLabels';
import { APPLICATION_TYPES_FIXTURE } from '../fixtures/applicationTypes.fixture';

describe('normalizeApplicationTypes', () => {
    it('returns only API-enabled types with API ids, labels, and grant metadata', () => {
        const normalized = normalizeApplicationTypes([
            {
                id: 'simple',
                name: 'Simple',
                description: 'A standalone client where you manage your own client_id. No DCR involved.',
                requires_redirect_uris: false,
                allowed_grant_types: [],
                default_grant_types: [],
                mandatory_grant_types: [],
            },
        ]);

        expect(normalized).toHaveLength(1);
        expect(normalized[0]?.id).toBe('simple');
        expect(normalized[0]?.name).toBe('Simple');
    });

    it('preserves response_types on grant types (applications/types.json shape)', () => {
        const normalized = normalizeApplicationTypes([APPLICATION_TYPES_FIXTURE[1]!]);

        expect(normalized[0]?.id).toBe('browser');
        expect(normalized[0]?.allowed_grant_types[0]?.response_types).toEqual(['code']);
        expect(normalized[0]?.allowed_grant_types[1]?.response_types).toEqual(['token', 'id_token']);
    });

    it('sorts types in catalog order and supports partial API responses', () => {
        const normalized = normalizeApplicationTypes([
            APPLICATION_TYPES_FIXTURE[4]!,
            APPLICATION_TYPES_FIXTURE[1]!,
            APPLICATION_TYPES_FIXTURE[0]!,
        ]);

        expect(normalized.map(type => type.id)).toEqual(['simple', 'browser', 'backend_to_backend']);
    });
});

describe('isSameApplicationType', () => {
    it('compares ids case-insensitively', () => {
        expect(isSameApplicationType('simple', 'SIMPLE')).toBe(true);
        expect(isSameApplicationType('browser', 'web')).toBe(false);
    });
});
