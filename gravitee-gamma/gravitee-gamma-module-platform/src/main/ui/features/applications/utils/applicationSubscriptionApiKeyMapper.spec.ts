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
import { mapApiKeyEntityToRow, mapApiKeysToRows, maskApiKey } from './applicationSubscriptionApiKeyMapper';

describe('applicationSubscriptionApiKeyMapper', () => {
    describe('maskApiKey', () => {
        it('masks keys longer than four characters', () => {
            expect(maskApiKey('abcdefghij')).toBe('••••••••ghij');
        });

        it('returns short keys unchanged', () => {
            expect(maskApiKey('ab')).toBe('ab');
        });
    });

    describe('mapApiKeyEntityToRow', () => {
        it('returns null without id or key', () => {
            expect(mapApiKeyEntityToRow({ id: 'k1' })).toBeNull();
            expect(mapApiKeyEntityToRow({ key: 'secret' })).toBeNull();
        });

        it('marks revoked and expired keys as invalid', () => {
            expect(mapApiKeyEntityToRow({ id: '1', key: 'active-key-1234', revoked: true })?.isValid).toBe(false);
            expect(mapApiKeyEntityToRow({ id: '2', key: 'expired-key-1234', expired: true })?.isValid).toBe(false);
            expect(mapApiKeyEntityToRow({ id: '3', key: 'valid-key-1234' })?.isValid).toBe(true);
        });

        it('uses revoked_at as endDate when revoked', () => {
            const row = mapApiKeyEntityToRow({ id: '1', key: 'key-1234', revoked: true, revoked_at: 99 });
            expect(row?.endDate).toBe(99);
        });
    });

    describe('mapApiKeysToRows', () => {
        it('filters invalid entities', () => {
            expect(mapApiKeysToRows([{ id: '1', key: 'abc' }, { id: '2' }])).toHaveLength(1);
        });
    });
});
