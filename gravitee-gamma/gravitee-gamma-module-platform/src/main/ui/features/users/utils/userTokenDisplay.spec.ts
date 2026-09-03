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
import { buildTokenUsageExample, formatTokenTimestamp, isDuplicateTokenError, validateTokenName } from './userTokenDisplay';

jest.mock('../../../shared/api/apimClient', () => ({
    loadApimBootstrap: jest.fn().mockResolvedValue({
        managementBaseURL: 'http://localhost:8083/management',
        organizationId: 'DEFAULT',
    }),
}));

describe('userTokenDisplay utilities', () => {
    it('validates token names like classic console', () => {
        expect(validateTokenName('')).toBe('Name is required.');
        expect(validateTokenName('a')).toBe('Name has to be at least 2 characters long.');
        expect(validateTokenName('ab')).toBeNull();
        expect(validateTokenName('a'.repeat(64))).toBeNull();
        expect(validateTokenName('a'.repeat(65))).toBe('Name has to be at most 64 characters long.');
    });

    it('detects duplicate token API errors', () => {
        expect(isDuplicateTokenError('A token with the name [External] already exists.')).toBe(true);
        expect(isDuplicateTokenError('Failed to generate token.')).toBe(false);
    });

    it('builds a curl usage example for the selected environment', async () => {
        await expect(buildTokenUsageExample('token-value', 'DEFAULT')).resolves.toBe(
            'curl -H "Authorization: Bearer token-value" "http://localhost:8083/management/organizations/DEFAULT/environments/DEFAULT"',
        );
    });

    it('formats token timestamps for table display', () => {
        const RealDateTimeFormat = Intl.DateTimeFormat;
        const dtfSpy = jest
            .spyOn(Intl, 'DateTimeFormat')
            .mockImplementation(
                (locale?: string | string[], options?: Intl.DateTimeFormatOptions) =>
                    new RealDateTimeFormat(locale, { ...options, timeZone: 'UTC' }),
            );
        try {
            expect(formatTokenTimestamp(undefined)).toBe('never');
            expect(formatTokenTimestamp(Date.parse('2021-08-31T01:35:35.403Z'))).toMatch(/Aug 31, 2021|31 Aug 2021/);
        } finally {
            dtfSpy.mockRestore();
        }
    });
});
