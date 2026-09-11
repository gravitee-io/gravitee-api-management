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

import { findHttpStatusCode, HTTP_STATUS_CODES, isValidHttpStatusCode } from './httpStatusCodes';

describe('HTTP_STATUS_CODES', () => {
    it('lists each status code at most once', () => {
        const codes = HTTP_STATUS_CODES.map(status => status.code);
        expect(new Set(codes).size).toBe(codes.length);
    });

    it('labels 302 as FOUND', () => {
        expect(findHttpStatusCode(302)).toEqual({ code: 302, label: 'FOUND' });
    });
});

describe('isValidHttpStatusCode', () => {
    it('accepts only allowlisted status codes', () => {
        expect(isValidHttpStatusCode(400)).toBe(true);
        expect(isValidHttpStatusCode('404')).toBe(true);
        expect(isValidHttpStatusCode(302)).toBe(true);
    });

    it('rejects codes outside the allowlist, even if they are in the 100–599 range', () => {
        expect(isValidHttpStatusCode(418)).toBe(false);
        expect(isValidHttpStatusCode('451')).toBe(false);
        expect(isValidHttpStatusCode(299)).toBe(false);
    });

    it('rejects empty, non-numeric, and out-of-range values', () => {
        expect(isValidHttpStatusCode(undefined)).toBe(false);
        expect(isValidHttpStatusCode('')).toBe(false);
        expect(isValidHttpStatusCode('abc')).toBe(false);
        expect(isValidHttpStatusCode(99)).toBe(false);
        expect(isValidHttpStatusCode(600)).toBe(false);
        expect(isValidHttpStatusCode('4000')).toBe(false);
    });
});
