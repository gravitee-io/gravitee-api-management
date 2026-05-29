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
import { serializeHttpClientOptions, serializeHttpProxyOptions, serializeSharedConfiguration } from './endpointSharedConfiguration';
import { DEFAULT_HTTP, DEFAULT_PROXY, DEFAULT_SHARED_CONFIG, DEFAULT_SSL } from '../pages/detail/endpoints/types';

describe('endpointSharedConfiguration', () => {
    it('serializes HTTP/1.1 without V2-only or renamed fields', () => {
        const http = serializeHttpClientOptions({ ...DEFAULT_HTTP, version: 'HTTP_1_1' });
        expect(http).toMatchObject({
            version: 'HTTP_1_1',
            propagateClientHost: false,
            maxConcurrentConnections: 20,
        });
        expect(http).not.toHaveProperty('maxHeaderSize');
        expect(http).not.toHaveProperty('maxChunkSize');
        expect(http).not.toHaveProperty('propagateClientHostHeader');
        expect(http).not.toHaveProperty('clearTextUpgrade');
    });

    it('serializes HTTP/2 with only schema-allowed extra fields', () => {
        const http = serializeHttpClientOptions({ ...DEFAULT_HTTP, version: 'HTTP_2' });
        expect(http.version).toBe('HTTP_2');
        expect(http).toHaveProperty('clearTextUpgrade');
        expect(http).toHaveProperty('http2MultiplexingLimit');
        expect(http).not.toHaveProperty('maxHeaderSize');
    });

    it('serializes disabled proxy without extraneous keys', () => {
        expect(serializeHttpProxyOptions({ ...DEFAULT_PROXY, enabled: false })).toEqual({
            enabled: false,
            useSystemProxy: false,
        });
    });

    it('serializes full shared configuration for default form state', () => {
        const sc = serializeSharedConfiguration(DEFAULT_SHARED_CONFIG);
        expect(sc.proxy).toEqual({ enabled: false, useSystemProxy: false });
        expect(sc.http?.version).toBe('HTTP_1_1');
        expect(sc.ssl).toEqual({ hostnameVerifier: DEFAULT_SSL.hostnameVerifier, trustAll: DEFAULT_SSL.trustAll });
        expect(sc.ssl).not.toHaveProperty('clientAuthentication');
    });
});
