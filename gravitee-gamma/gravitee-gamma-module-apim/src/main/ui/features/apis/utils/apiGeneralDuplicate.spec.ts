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
import { extractContextPathPlaceholder, extractHostPlaceholder, getDuplicateEntryMode } from './apiGeneralDuplicate';
import type { ApiDetailDto } from '../types';

describe('apiGeneralDuplicate', () => {
    it('derives duplicate entry mode and placeholders from listeners', () => {
        const httpApi = {
            id: 'api-http',
            listeners: [{ type: 'HTTP', paths: [{ path: '/foo' }] }],
        } as ApiDetailDto;

        expect(getDuplicateEntryMode(httpApi)).toBe('contextPath');
        expect(extractContextPathPlaceholder(httpApi)).toBe('/foo');

        const virtualHostApi = {
            id: 'api-vhost',
            listeners: [{ type: 'HTTP', hosts: [{ host: 'api.example.com', path: '/foo' }] }],
        } as ApiDetailDto;

        expect(extractContextPathPlaceholder(virtualHostApi)).toBe('api.example.com/foo');

        const tcpApi = {
            id: 'api-tcp',
            listeners: [{ type: 'TCP', hosts: [{ host: 'tcp.example.com', path: '/' }] }],
        } as ApiDetailDto;

        expect(getDuplicateEntryMode(tcpApi)).toBe('host');
        expect(extractHostPlaceholder(tcpApi)).toBe('tcp.example.com');
    });
});
