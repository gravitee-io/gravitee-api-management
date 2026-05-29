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
import { hasBackendSecurityConfiguration, hasDefaultEndpointGroupBackendSecurityConfigured } from './endpointGroupBackendSecurity';
import type { ApiDetailDto } from '../types';

describe('endpointGroupBackendSecurity', () => {
    it('returns false when shared configuration is missing or empty', () => {
        expect(hasBackendSecurityConfiguration(undefined)).toBe(false);
        expect(hasBackendSecurityConfiguration({})).toBe(false);
    });

    it('returns false when only HTTP timeouts differ from defaults', () => {
        expect(hasBackendSecurityConfiguration({ http: { readTimeout: 20000 } })).toBe(false);
    });

    it('returns false when only default SSL/proxy/headers are persisted', () => {
        expect(
            hasBackendSecurityConfiguration({
                http: { readTimeout: 10000 },
                ssl: { hostnameVerifier: true, trustAll: false, clientAuthentication: 'NONE' },
                proxy: { enabled: false },
                headers: [],
            }),
        ).toBe(false);
    });

    it('detects proxy, SSL, and header security settings', () => {
        expect(hasBackendSecurityConfiguration({ proxy: { enabled: true, host: 'proxy.local' } })).toBe(true);
        expect(hasBackendSecurityConfiguration({ ssl: { clientAuthentication: 'REQUIRED' } })).toBe(true);
        expect(hasBackendSecurityConfiguration({ ssl: { trustAll: true } })).toBe(true);
        expect(hasBackendSecurityConfiguration({ headers: [{ name: 'Authorization', value: 'Bearer x' }] })).toBe(true);
    });

    it('uses the first endpoint group only', () => {
        const api = {
            id: 'api-1',
            endpointGroups: [
                { name: 'default', type: 'http-proxy', sharedConfiguration: {} },
                { name: 'other', type: 'http-proxy', sharedConfiguration: { ssl: { clientAuthentication: 'REQUIRED' } } },
            ],
        } as ApiDetailDto;

        expect(hasDefaultEndpointGroupBackendSecurityConfigured(api)).toBe(false);
    });
});
