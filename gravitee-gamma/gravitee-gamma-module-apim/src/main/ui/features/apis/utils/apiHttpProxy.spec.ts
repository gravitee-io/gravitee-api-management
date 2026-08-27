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
import { formatEndpointTarget, hasTcpListeners, isHttpProxyApi } from './apiHttpProxy';
import type { ApiDetailDto } from '../types';

describe('hasTcpListeners', () => {
    it('returns false when api is null, undefined, or has no listeners', () => {
        expect(hasTcpListeners(null)).toBe(false);
        expect(hasTcpListeners(undefined)).toBe(false);
        expect(hasTcpListeners({})).toBe(false);
    });

    it('returns false when no listener is TCP', () => {
        expect(hasTcpListeners({ listeners: [{ type: 'HTTP' }] })).toBe(false);
    });

    it('returns true when any listener is TCP', () => {
        expect(hasTcpListeners({ listeners: [{ type: 'HTTP' }, { type: 'TCP' }] })).toBe(true);
    });
});

describe('isHttpProxyApi', () => {
    it('returns false when api is null or undefined', () => {
        expect(isHttpProxyApi(null)).toBe(false);
        expect(isHttpProxyApi(undefined)).toBe(false);
    });

    it('returns false when the api is not a PROXY type', () => {
        expect(isHttpProxyApi({ id: '1', name: 'a', type: 'MESSAGE' } as ApiDetailDto)).toBe(false);
    });

    it('returns false when the api has a TCP listener', () => {
        const api = { id: '1', name: 'a', type: 'PROXY', listeners: [{ type: 'TCP' }] } as unknown as ApiDetailDto;
        expect(isHttpProxyApi(api)).toBe(false);
    });

    it('returns true for a PROXY api without TCP listeners', () => {
        const api = { id: '1', name: 'a', type: 'PROXY', listeners: [{ type: 'HTTP' }] } as ApiDetailDto;
        expect(isHttpProxyApi(api)).toBe(true);
    });
});

describe('formatEndpointTarget', () => {
    it('returns undefined when the target is undefined', () => {
        expect(formatEndpointTarget(undefined)).toBeUndefined();
    });

    it('returns the string target unchanged for HTTP endpoints', () => {
        expect(formatEndpointTarget('https://backend.example.com')).toBe('https://backend.example.com');
    });

    it('formats a TCP target as host:port', () => {
        expect(formatEndpointTarget({ host: 'backend.example.com', port: 9090, secured: false })).toBe('backend.example.com:9090');
    });
});
