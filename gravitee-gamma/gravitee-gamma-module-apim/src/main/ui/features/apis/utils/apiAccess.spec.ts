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
import { getApiAccessPath } from './apiAccess';
import type { ApiListItem } from '../types/api';

function makeApi(overrides: Partial<ApiListItem> = {}): ApiListItem {
    return { id: '1', name: 'Test API', apiVersion: '1.0', type: 'PROXY', definitionVersion: 'V4', ...overrides };
}

describe('getApiAccessPath', () => {
    it('returns null when the API has no listeners', () => {
        expect(getApiAccessPath(makeApi())).toBeNull();
    });

    it('returns null when the listeners array is empty', () => {
        expect(getApiAccessPath(makeApi({ listeners: [] }))).toBeNull();
    });

    it('returns null when there is no HTTP listener', () => {
        expect(getApiAccessPath(makeApi({ listeners: [{ type: 'GRPC' }] }))).toBeNull();
    });

    it('returns null when the HTTP listener has no paths', () => {
        expect(getApiAccessPath(makeApi({ listeners: [{ type: 'HTTP' }] }))).toBeNull();
        expect(getApiAccessPath(makeApi({ listeners: [{ type: 'HTTP', paths: [] }] }))).toBeNull();
    });

    it('returns the path alone when the first HTTP path has no host', () => {
        const api = makeApi({ listeners: [{ type: 'HTTP', paths: [{ path: '/my-api' }] }] });
        expect(getApiAccessPath(api)).toBe('/my-api');
    });

    it('strips a trailing slash from the path', () => {
        const api = makeApi({ listeners: [{ type: 'HTTP', paths: [{ path: '/my-api/' }] }] });
        expect(getApiAccessPath(api)).toBe('/my-api');
    });

    it('preserves a bare "/" path', () => {
        const api = makeApi({ listeners: [{ type: 'HTTP', paths: [{ path: '/' }] }] });
        expect(getApiAccessPath(api)).toBe('/');
    });

    it('returns host + path when both are present', () => {
        const api = makeApi({ listeners: [{ type: 'HTTP', paths: [{ host: 'api.example.com', path: '/v1' }] }] });
        expect(getApiAccessPath(api)).toBe('api.example.com/v1');
    });

    it('strips a trailing slash from host + path', () => {
        const api = makeApi({ listeners: [{ type: 'HTTP', paths: [{ host: 'api.example.com', path: '/v1/' }] }] });
        expect(getApiAccessPath(api)).toBe('api.example.com/v1');
    });

    it('uses only the first path when multiple paths are present', () => {
        const api = makeApi({
            listeners: [{ type: 'HTTP', paths: [{ path: '/first' }, { path: '/second' }] }],
        });
        expect(getApiAccessPath(api)).toBe('/first');
    });
});
