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
import { retryTransientRequest } from './queryRetry';

describe('retryTransientRequest', () => {
    it('retries transient failures twice', () => {
        expect(retryTransientRequest(0, new Error('network'))).toBe(true);
        expect(retryTransientRequest(1, new Error('network'))).toBe(true);
        expect(retryTransientRequest(2, new Error('network'))).toBe(false);
    });

    it('does not retry client errors', () => {
        expect(retryTransientRequest(0, { status: 400 })).toBe(false);
        expect(retryTransientRequest(0, { status: 404 })).toBe(false);
    });

    it('retries server errors', () => {
        expect(retryTransientRequest(0, { status: 500 })).toBe(true);
    });
});
