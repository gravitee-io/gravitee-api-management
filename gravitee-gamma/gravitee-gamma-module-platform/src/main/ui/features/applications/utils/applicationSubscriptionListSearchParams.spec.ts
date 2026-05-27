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
    buildApplicationSubscriptionListSearchParams,
    parseApplicationSubscriptionListSearchParams,
} from './applicationSubscriptionListSearchParams';

describe('applicationSubscriptionListSearchParams', () => {
    it('parses console-aligned query params', () => {
        const params = new URLSearchParams('page=2&size=25&status=ACCEPTED,CLOSED&apis=api-1,api-2&apiKey=key-abc');
        expect(parseApplicationSubscriptionListSearchParams(params)).toEqual({
            page: 2,
            pageSize: 25,
            apiFilters: ['api-1', 'api-2'],
            statusFilters: ['ACCEPTED', 'CLOSED'],
            apiKeyInput: 'key-abc',
        });
    });

    it('falls back to defaults when params are missing or invalid', () => {
        expect(parseApplicationSubscriptionListSearchParams(new URLSearchParams())).toEqual({
            page: 1,
            pageSize: 10,
            apiFilters: [],
            statusFilters: ['ACCEPTED', 'PAUSED', 'PENDING', 'RESUMED'],
            apiKeyInput: '',
        });
        expect(parseApplicationSubscriptionListSearchParams(new URLSearchParams('page=0&size=-1'))).toMatchObject({
            page: 1,
            pageSize: 10,
        });
    });

    it('omits default values when building search params', () => {
        const built = buildApplicationSubscriptionListSearchParams({
            page: 1,
            pageSize: 10,
            apiFilters: [],
            statusFilters: ['ACCEPTED', 'PAUSED', 'PENDING', 'RESUMED'],
            apiKeyInput: '',
        });
        expect(built.get('status')).toBeNull();
        expect(built.get('page')).toBeNull();
        expect(built.get('size')).toBeNull();
    });

    it('omits default status filters regardless of order', () => {
        const built = buildApplicationSubscriptionListSearchParams({
            page: 1,
            pageSize: 10,
            apiFilters: [],
            statusFilters: ['RESUMED', 'PENDING', 'PAUSED', 'ACCEPTED'],
            apiKeyInput: '',
        });
        expect(built.get('status')).toBeNull();
    });

    it('serializes non-default filters', () => {
        const built = buildApplicationSubscriptionListSearchParams({
            page: 3,
            pageSize: 50,
            apiFilters: ['a1'],
            statusFilters: ['CLOSED'],
            apiKeyInput: 'k1',
        });
        expect(built.get('page')).toBe('3');
        expect(built.get('size')).toBe('50');
        expect(built.get('apis')).toBe('a1');
        expect(built.get('status')).toBe('CLOSED');
        expect(built.get('apiKey')).toBe('k1');
    });

    it('sorts status filters when serializing', () => {
        const built = buildApplicationSubscriptionListSearchParams({
            page: 1,
            pageSize: 10,
            apiFilters: [],
            statusFilters: ['PENDING', 'ACCEPTED'],
            apiKeyInput: '',
        });
        expect(built.get('status')).toBe('ACCEPTED,PENDING');
    });
});
