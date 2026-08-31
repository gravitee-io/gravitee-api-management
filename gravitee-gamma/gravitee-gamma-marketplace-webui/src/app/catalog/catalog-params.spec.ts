/*
 * Copyright (C) 2026 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import { parseCatalogSearchParams, serializeCatalogSearchParams } from './catalog-params';

describe('catalog search params', () => {
    it('should parse catalog filters from the query string', () => {
        const params = parseCatalogSearchParams(
            new URLSearchParams('query=helpdesk&category=it&protocol=A2A_PROXY&label=ops&view=list&page=2&size=25'),
        );

        expect(params).toEqual({
            query: 'helpdesk',
            category: 'it',
            protocol: 'A2A_PROXY',
            label: 'ops',
            view: 'list',
            page: 2,
            pageSize: 25,
        });
    });

    it('should omit default view, page, and size from the query string', () => {
        const search = serializeCatalogSearchParams({
            query: 'helpdesk',
            category: '',
            protocol: '',
            label: '',
            view: 'grid',
            page: 1,
            pageSize: 12,
        });

        expect(search.toString()).toBe('query=helpdesk');
    });
});
