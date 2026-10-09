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
import { maskSensitiveHeader } from './maskSensitiveHeader';

describe('maskSensitiveHeader', () => {
    it('keeps the scheme and the last four characters of a long bearer token', () => {
        expect(maskSensitiveHeader('Bearer eyJhbGciOiJIUzI1NiJ9.payload.signature', 'Authorization')).toBe('Bearer ••••••••ture');
    });

    it('masks cookie values entirely', () => {
        expect(maskSensitiveHeader('sessionId=some-very-long-session-value; HttpOnly', 'Set-Cookie')).toBe('••••••••');
    });

    it('leaves ordinary headers readable', () => {
        expect(maskSensitiveHeader('application/json', 'Accept')).toBe('application/json');
    });
});
