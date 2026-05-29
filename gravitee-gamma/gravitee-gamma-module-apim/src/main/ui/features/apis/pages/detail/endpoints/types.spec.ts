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
import { buildDefaultEndpointForGroup, validateEndpointName, validateEndpointTarget, validateGroupName } from './types';

describe('endpoint group types', () => {
    it('buildDefaultEndpointForGroup includes required target (classic create parity)', () => {
        expect(buildDefaultEndpointForGroup('my-group', 'https://api.example.com')).toEqual({
            name: 'my-group default endpoint',
            type: 'http-proxy',
            inheritConfiguration: true,
            weight: 1,
            configuration: { target: 'https://api.example.com' },
        });
    });

    it('validates names like classic console (required, no colons — no min length)', () => {
        expect(validateGroupName('a')).toBeNull();
        expect(validateGroupName('a:b')).toBe('Name must not contain colons.');
        expect(validateEndpointName('')).toBe('Name is required.');
    });

    it('requires endpoint target URL', () => {
        expect(validateEndpointTarget('')).toBe('Target URL is required.');
        expect(validateEndpointTarget('https://ok.example.com')).toBeNull();
    });
});
