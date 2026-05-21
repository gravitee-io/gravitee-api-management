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
import { canSubmitApiKeyExpirationChange, isAfterMinCandidate, parseDatetimeLocalValue } from './applicationSubscriptionApiKeyExpireUtils';

describe('applicationSubscriptionApiKeyExpireUtils', () => {
    const minMs = new Date('2025-06-01T12:00:00').getTime();

    it('accepts only future datetimes', () => {
        expect(isAfterMinCandidate('2024-01-01T00:00', minMs)).toBe(false);
        expect(isAfterMinCandidate('invalid', minMs)).toBe(false);
        expect(isAfterMinCandidate('2025-02-31T12:00', minMs)).toBe(false);
        expect(isAfterMinCandidate('2025-99-99T12:00', minMs)).toBe(false);
        expect(isAfterMinCandidate('2099-06-15T12:00', minMs)).toBe(true);
    });

    it('parses only valid datetime-local values', () => {
        expect(parseDatetimeLocalValue('2025-06-15T12:30')?.toISOString()).toBe(new Date(2025, 5, 15, 12, 30).toISOString());
        expect(parseDatetimeLocalValue('2025-02-31T12:00')).toBeNull();
        expect(parseDatetimeLocalValue('2025-06-15')).toBeNull();
    });

    it('requires dirty state and non-empty value to submit', () => {
        expect(canSubmitApiKeyExpirationChange(false, '2099-06-15T12:00', minMs)).toBe(false);
        expect(canSubmitApiKeyExpirationChange(true, '', minMs)).toBe(false);
        expect(canSubmitApiKeyExpirationChange(true, '2099-06-15T12:00', minMs)).toBe(true);
        expect(canSubmitApiKeyExpirationChange(true, '2020-01-01T00:00', minMs)).toBe(false);
    });
});
