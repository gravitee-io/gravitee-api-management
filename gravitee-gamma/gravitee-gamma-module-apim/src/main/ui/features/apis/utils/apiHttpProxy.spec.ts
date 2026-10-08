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
import {
    areAllListenersTcp,
    formatEndpointTarget,
    getApiProxyTypeLabel,
    hasTcpListener,
    hasTcpListeners,
    isHttpProxyApi,
    isTcpEndpointGroup,
    normalizeTcpHost,
    supportsResponseTemplates,
} from './apiHttpProxy';
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

    it('returns true when a listener exposes a tcp-proxy entrypoint even without an explicit TCP type', () => {
        expect(
            hasTcpListeners({
                listeners: [{ type: 'HTTP', entrypoints: [{ type: 'tcp-proxy' }] }],
            }),
        ).toBe(true);
    });

    it('returns true when endpoint groups use tcp-proxy', () => {
        expect(
            hasTcpListeners({
                listeners: [],
                endpointGroups: [{ type: 'tcp-proxy' }],
            }),
        ).toBe(true);
    });
});

describe('hasTcpListener', () => {
    it('is true only when a listener type is TCP', () => {
        expect(hasTcpListener(null)).toBe(false);
        expect(hasTcpListener({ listeners: [{ type: 'HTTP' }] })).toBe(false);
        expect(hasTcpListener({ listeners: [{ type: 'HTTP' }], endpointGroups: [{ type: 'tcp-proxy' }] })).toBe(false);
        expect(hasTcpListener({ listeners: [{ entrypoints: [{ type: 'tcp-proxy' }] }] })).toBe(false);
        expect(hasTcpListener({ listeners: [{ type: 'HTTP', entrypoints: [{ type: 'tcp-proxy' }] }] })).toBe(false);
        expect(hasTcpListener({ listeners: [{ type: 'TCP' }] })).toBe(true);
        expect(hasTcpListener({ listeners: [{ type: 'HTTP' }, { type: 'TCP' }] })).toBe(true);
    });
});

describe('areAllListenersTcp', () => {
    it('returns false when there is no listener list, or it is empty', () => {
        expect(areAllListenersTcp(null)).toBe(false);
        expect(areAllListenersTcp(undefined)).toBe(false);
        expect(areAllListenersTcp({})).toBe(false);
        expect(areAllListenersTcp({ listeners: [] })).toBe(false);
    });

    it('returns false when any listener is not TCP', () => {
        expect(areAllListenersTcp({ listeners: [{ type: 'HTTP' }] })).toBe(false);
        expect(areAllListenersTcp({ listeners: [{ type: 'HTTP' }, { type: 'TCP' }] })).toBe(false);
        expect(areAllListenersTcp({ listeners: [{ type: 'HTTP' }, { entrypoints: [{ type: 'tcp-proxy' }] }] })).toBe(false);
        expect(areAllListenersTcp({ listeners: [{ entrypoints: [{ type: 'tcp-proxy' }] }] })).toBe(false);
        expect(areAllListenersTcp({ listeners: [{ type: 'HTTP', entrypoints: [{ type: 'tcp-proxy' }] }] })).toBe(false);
    });

    it('returns false when TCP is only an endpoint group', () => {
        expect(areAllListenersTcp({ listeners: [{ type: 'HTTP' }], endpointGroups: [{ type: 'tcp-proxy' }] })).toBe(false);
    });

    it('returns true when every listener type is TCP', () => {
        expect(areAllListenersTcp({ listeners: [{ type: 'TCP' }] })).toBe(true);
        expect(areAllListenersTcp({ listeners: [{ type: 'TCP' }, { type: 'tcp' }] })).toBe(true);
    });
});

describe('getApiProxyTypeLabel', () => {
    it('returns TCP Proxy when the API has TCP listeners', () => {
        expect(getApiProxyTypeLabel({ listeners: [{ type: 'TCP' }] })).toBe('TCP Proxy');
    });

    it('returns HTTP Proxy when the API has no TCP listeners', () => {
        expect(getApiProxyTypeLabel({ listeners: [{ type: 'HTTP' }] })).toBe('HTTP Proxy');
    });
});

describe('supportsResponseTemplates', () => {
    it('returns true when api is null, undefined, or a supported type', () => {
        expect(supportsResponseTemplates(null)).toBe(true);
        expect(supportsResponseTemplates(undefined)).toBe(true);
        expect(supportsResponseTemplates({ type: 'PROXY' })).toBe(true);
        expect(supportsResponseTemplates({ type: 'MESSAGE' })).toBe(true);
    });

    it('returns false for MCP_PROXY and LLM_PROXY', () => {
        expect(supportsResponseTemplates({ type: 'MCP_PROXY' })).toBe(false);
        expect(supportsResponseTemplates({ type: 'LLM_PROXY' })).toBe(false);
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

describe('normalizeTcpHost', () => {
    it('returns an empty string for undefined', () => {
        expect(normalizeTcpHost(undefined)).toBe('');
    });

    it('returns string hosts unchanged', () => {
        expect(normalizeTcpHost('tcp.example.com')).toBe('tcp.example.com');
    });

    it('extracts host from object-shaped entries', () => {
        expect(normalizeTcpHost({ host: 'tcp.example.com' })).toBe('tcp.example.com');
    });
});

describe('isTcpEndpointGroup', () => {
    it('returns true for tcp-proxy groups', () => {
        expect(isTcpEndpointGroup({ type: 'tcp-proxy' })).toBe(true);
    });

    it('returns false for http-proxy groups and missing type', () => {
        expect(isTcpEndpointGroup({ type: 'http-proxy' })).toBe(false);
        expect(isTcpEndpointGroup(undefined)).toBe(false);
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
