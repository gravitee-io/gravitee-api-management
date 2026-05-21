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
import { mapDetailToCloseTarget, mapSubscriptionEntityToDetail } from './applicationSubscriptionDetailMapper';
import type { ApplicationSubscriptionEntity } from '../types/applicationSubscription';

const baseEntity: ApplicationSubscriptionEntity = {
    id: 'sub-1',
    status: 'ACCEPTED',
    api: { id: 'api-1', name: 'Payments', version: 'v2' },
    plan: { id: 'plan-1', name: 'Gold', security: 'API_KEY' },
    subscribed_by: { displayName: 'Alice' },
    origin: 'MANAGEMENT',
};

describe('applicationSubscriptionDetailMapper', () => {
    it('returns null when id or status is missing', () => {
        expect(mapSubscriptionEntityToDetail({ ...baseEntity, id: undefined }, 'EXCLUSIVE')).toBeNull();
        expect(mapSubscriptionEntityToDetail({ ...baseEntity, status: undefined }, 'EXCLUSIVE')).toBeNull();
    });

    it('maps API subscription detail with version in display name', () => {
        const detail = mapSubscriptionEntityToDetail(baseEntity, 'EXCLUSIVE');
        expect(detail).toMatchObject({
            id: 'sub-1',
            apiDisplay: 'Payments — v2',
            referenceTypeLabel: 'API',
            planName: 'Gold',
            securityType: 'API Key',
            subscribedBy: 'Alice',
            isSharedApiKey: false,
        });
    });

    it('detects API product via referenceType or apiProduct', () => {
        const byRef = mapSubscriptionEntityToDetail({ ...baseEntity, referenceType: 'API_PRODUCT', api: undefined }, undefined);
        const byProduct = mapSubscriptionEntityToDetail(
            {
                ...baseEntity,
                api: undefined,
                apiProduct: { id: 'prod-1', name: 'Billing' },
            },
            undefined,
        );
        expect(byRef?.referenceTypeLabel).toBe('API Product');
        expect(byProduct?.referenceTypeLabel).toBe('API Product');
        expect(byProduct?.apiDisplay).toBe('Billing');
    });

    it('sets isSharedApiKey for SHARED mode and API_KEY plan security', () => {
        expect(mapSubscriptionEntityToDetail(baseEntity, 'SHARED')?.isSharedApiKey).toBe(true);
        expect(mapSubscriptionEntityToDetail(baseEntity, 'EXCLUSIVE')?.isSharedApiKey).toBe(false);
        expect(
            mapSubscriptionEntityToDetail({ ...baseEntity, plan: { id: 'p', name: 'J', security: 'JWT' } }, 'SHARED')?.isSharedApiKey,
        ).toBe(false);
    });

    it('passes subscription metadata through', () => {
        const detail = mapSubscriptionEntityToDetail({ ...baseEntity, metadata: { env: 'prod' } }, 'EXCLUSIVE');
        expect(detail?.metadata).toEqual({ env: 'prod' });
    });

    it('maps detail to close target', () => {
        const detail = mapSubscriptionEntityToDetail(baseEntity, 'EXCLUSIVE')!;
        expect(mapDetailToCloseTarget(detail)).toEqual({
            id: 'sub-1',
            referenceTypeLabel: 'API',
            securityType: 'API Key',
            isSharedApiKey: false,
        });
    });
});
