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
    API_LIST_DEFAULT_PER_PAGE,
    apiListUrlStatesEqual,
    buildApiListSearchParams,
    emptyApiListUrlState,
    parseApiListSearchParams,
    urlSearchParamsEqual,
} from './apiListSearchParams';

describe('apiListSearchParams', () => {
    it('parses classic list query params', () => {
        const params = new URLSearchParams(
            'q=orders&page=2&size=25&order=status&apiTypes=V4_TCP_PROXY&statuses=STARTED&tags=eu-west&categories=partners',
        );

        expect(parseApiListSearchParams(params)).toEqual({
            query: 'orders',
            page: 2,
            perPage: 25,
            order: 'status',
            filters: {
                apiTypes: ['V4_TCP_PROXY'],
                statuses: ['STARTED'],
                tags: ['eu-west'],
                categories: ['partners'],
            },
        });
    });

    it('defaults missing params to an empty browse state', () => {
        expect(parseApiListSearchParams(new URLSearchParams())).toEqual(emptyApiListUrlState());
    });

    it('builds a URL with only non-default values', () => {
        const params = buildApiListSearchParams({
            query: 'orders',
            page: 3,
            perPage: API_LIST_DEFAULT_PER_PAGE,
            order: '-name',
            filters: {
                apiTypes: ['V4_TCP_PROXY'],
                statuses: [],
                tags: [],
                categories: [],
            },
        });

        expect(params.get('q')).toBe('orders');
        expect(params.get('page')).toBe('3');
        expect(params.has('size')).toBe(false);
        expect(params.get('order')).toBe('-name');
        expect(params.getAll('apiTypes')).toEqual(['V4_TCP_PROXY']);
        expect(params.has('statuses')).toBe(false);
    });

    it('treats URL params as equal when only ordering differs', () => {
        const a = new URLSearchParams('statuses=STARTED&apiTypes=V4_TCP_PROXY');
        const b = new URLSearchParams('apiTypes=V4_TCP_PROXY&statuses=STARTED');

        expect(urlSearchParamsEqual(a, b)).toBe(true);
    });

    it('detects different filter values in parsed state', () => {
        const browse = emptyApiListUrlState();
        const filtered = {
            ...browse,
            filters: { ...browse.filters, statuses: ['STOPPED'] },
        };

        expect(apiListUrlStatesEqual(browse, filtered)).toBe(false);
        expect(apiListUrlStatesEqual(filtered, { ...filtered })).toBe(true);
    });

    it('drops unknown statuses and api types from the URL', () => {
        const params = new URLSearchParams('statuses=STARTED,UNKNOWN&apiTypes=FEDERATED,V4_HTTP_PROXY');

        expect(parseApiListSearchParams(params, { includeFederated: false }).filters).toEqual({
            apiTypes: ['V4_HTTP_PROXY'],
            statuses: ['STARTED'],
            tags: [],
            categories: [],
        });
    });

    it('keeps api types from the URL when federation type sanitization is deferred', () => {
        const params = new URLSearchParams('apiTypes=FEDERATED');

        expect(parseApiListSearchParams(params, { sanitizeApiTypes: false }).filters.apiTypes).toEqual(['FEDERATED']);
    });

    it('caps page size to the table allowlist', () => {
        expect(parseApiListSearchParams(new URLSearchParams('size=100000')).perPage).toBe(API_LIST_DEFAULT_PER_PAGE);
        expect(parseApiListSearchParams(new URLSearchParams('size=50')).perPage).toBe(50);
    });
});
