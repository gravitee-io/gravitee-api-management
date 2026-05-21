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
import { resolveSubscriptionApiKeyV2Parent } from './applicationSubscriptions';

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
