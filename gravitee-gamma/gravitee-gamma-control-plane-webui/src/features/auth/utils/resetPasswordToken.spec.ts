/*
 * Copyright (C) 2026 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import { isResetPasswordTokenExpired, parseResetPasswordToken } from './resetPasswordToken';

const VALID_TOKEN =
    'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.' +
    'eyJzdWIiOiJ1c2VyLTEiLCJlbWFpbCI6Im5vcm0xQGdtYWlsLmNvbSIsImZpcnN0bmFtZSI6Im5vcm0xIiwibGFzdG5hbWUiOiJub3JtMSIsImV4cCI6OTk5OTk5OTk5OTk5fQ.' +
    'signature';

describe('resetPasswordToken', () => {
    it('parses user claims from a reset token', () => {
        const claims = parseResetPasswordToken(VALID_TOKEN);

        expect(claims).toEqual(
            expect.objectContaining({
                sub: 'user-1',
                email: 'norm1@gmail.com',
                firstname: 'norm1',
                lastname: 'norm1',
            }),
        );
    });

    it('returns null for invalid tokens', () => {
        expect(parseResetPasswordToken('not-a-jwt')).toBeNull();
    });

    it('detects expired tokens', () => {
        const expiredToken = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.' + 'eyJzdWIiOiJ1c2VyLTEiLCJleHAiOjF9.' + 'signature';

        const claims = parseResetPasswordToken(expiredToken);
        expect(claims).not.toBeNull();
        expect(isResetPasswordTokenExpired(claims!)).toBe(true);
    });
});
