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
import { buildApplicationSubscriptionsQuery, resolveSubscriptionApiKeyV2Parent } from './applicationSubscriptions';

describe('buildApplicationSubscriptionsQuery', () => {
    it('joins status values with commas like console HttpParams', () => {
        expect(buildApplicationSubscriptionsQuery(1, 10, { status: ['ACCEPTED', 'PAUSED', 'PENDING'] })).toBe(
            'page=1&size=10&status=ACCEPTED,PAUSED,PENDING',
        );
    });

    it('includes all subscription statuses for overview-style totals without encoding commas', () => {
        expect(
            buildApplicationSubscriptionsQuery(1, 1, {
                status: ['ACCEPTED', 'CLOSED', 'PAUSED', 'PENDING', 'REJECTED', 'RESUMED'],
            }),
        ).toBe('page=1&size=1&status=ACCEPTED,CLOSED,PAUSED,PENDING,REJECTED,RESUMED');
    });

    it('omits status when no filters are provided', () => {
        expect(buildApplicationSubscriptionsQuery(1, 20)).toBe('page=1&size=20');
    });

    it('serializes api, api_key, and security_types filters', () => {
        expect(
            buildApplicationSubscriptionsQuery(2, 50, {
                apis: ['api-1', 'api-2'],
                apiKey: 'key-abc',
                securityTypes: ['API_KEY'],
            }),
        ).toBe('page=2&size=50&api=api-1,api-2&api_key=key-abc&security_types=API_KEY');
    });
});

describe('resolveSubscriptionApiKeyV2Parent', () => {
    it('resolves API parent from api.id', () => {
        expect(resolveSubscriptionApiKeyV2Parent({ api: { id: 'api-1', name: 'A' } })).toEqual({
            kind: 'API',
            apiId: 'api-1',
        });
    });

    it('resolves API parent from referenceType API and referenceId', () => {
        expect(
            resolveSubscriptionApiKeyV2Parent({
                referenceType: 'API',
                referenceId: 'api-ref',
            }),
        ).toEqual({ kind: 'API', apiId: 'api-ref' });
    });

    it('resolves API product parent from referenceType', () => {
        expect(
            resolveSubscriptionApiKeyV2Parent({
                referenceType: 'API_PRODUCT',
                referenceId: 'prod-1',
            }),
        ).toEqual({ kind: 'API_PRODUCT', apiProductId: 'prod-1' });
    });

    it('prefers apiProduct.id over referenceId', () => {
        expect(
            resolveSubscriptionApiKeyV2Parent({
                apiProduct: { id: 'prod-embedded', name: 'P' },
                referenceId: 'prod-ref',
            }),
        ).toEqual({ kind: 'API_PRODUCT', apiProductId: 'prod-embedded' });
    });

    it('returns null when references are missing', () => {
        expect(resolveSubscriptionApiKeyV2Parent({})).toBeNull();
        expect(resolveSubscriptionApiKeyV2Parent({ referenceType: 'API_PRODUCT' })).toBeNull();
        expect(resolveSubscriptionApiKeyV2Parent({ referenceType: 'API' })).toBeNull();
    });
});
