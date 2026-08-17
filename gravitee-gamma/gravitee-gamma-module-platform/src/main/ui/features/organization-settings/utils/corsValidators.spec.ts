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

import { CORS_HTTP_METHODS, DEFAULT_CORS_MAX_AGE, getInvalidAllowOrigins } from './corsValidators';

describe('corsValidators', () => {
    it('exposes Classic HTTP methods in order', () => {
        expect(CORS_HTTP_METHODS).toEqual(['*', 'GET', 'DELETE', 'PATCH', 'POST', 'PUT', 'OPTIONS', 'TRACE', 'HEAD']);
    });

    it('defaults max age to Classic 1728000 seconds', () => {
        expect(DEFAULT_CORS_MAX_AGE).toBe(1728000);
    });

    it('accepts *, plain origins, and valid regular expressions', () => {
        expect(getInvalidAllowOrigins(['*', 'https://console.example.com', '(http|https).*.mydomain.com'])).toEqual([]);
    });

    it('rejects invalid regular expressions that look like patterns', () => {
        expect(getInvalidAllowOrigins(['(http'])).toEqual(['(http']);
    });
});
