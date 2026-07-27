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
import { isForbiddenApiError } from './apiErrors';
import { ApimApiError } from '../api/apimClient';

describe('isForbiddenApiError', () => {
    it('is true for a 403 ApimApiError while isError is true', () => {
        expect(isForbiddenApiError(true, new ApimApiError(403, 'Forbidden'))).toBe(true);
    });

    it('is false for a non-403 ApimApiError', () => {
        expect(isForbiddenApiError(true, new ApimApiError(500, 'Internal Server Error'))).toBe(false);
        expect(isForbiddenApiError(true, new ApimApiError(404, 'Not Found'))).toBe(false);
    });

    it('is false when isError is false, even if the error looks like a 403', () => {
        expect(isForbiddenApiError(false, new ApimApiError(403, 'Forbidden'))).toBe(false);
    });

    it('is false for a plain Error with a status-shaped property, since it is not an ApimApiError', () => {
        const fakeError = Object.assign(new Error('nope'), { status: 403 });
        expect(isForbiddenApiError(true, fakeError)).toBe(false);
    });

    it('is false when there is no error', () => {
        expect(isForbiddenApiError(true, undefined)).toBe(false);
        expect(isForbiddenApiError(true, null)).toBe(false);
    });
});
