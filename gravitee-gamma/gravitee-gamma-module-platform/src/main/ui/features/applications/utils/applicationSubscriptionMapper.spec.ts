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
    canCloseSubscription,
    formatSubscriptionSecurityType,
    mapSubscriptionToTableRow,
    mapSubscriptionsPageToRows,
} from './applicationSubscriptionMapper';
import type { ApplicationSubscriptionsPagedResponse, SubscriptionPageItem } from '../types/applicationSubscription';

const baseSubscription: SubscriptionPageItem = {
    id: 'sub-1',
    plan: 'plan-1',
    api: 'api-1',
    status: 'ACCEPTED',
    created_at: 1000,
    origin: 'MANAGEMENT',
};

const metadata: ApplicationSubscriptionsPagedResponse['metadata'] = {
    'plan-1': { name: 'Gold', securityType: 'API_KEY' },
    'api-1': { name: 'Payments API', apiVersion: 'v1' },
    'product-1': { name: 'Billing Product', apiVersion: '2.0' },
};

describe('applicationSubscriptionMapper', () => {
    describe('formatSubscriptionSecurityType', () => {
        it('maps known security types', () => {
            expect(formatSubscriptionSecurityType('API_KEY')).toBe('API Key');
            expect(formatSubscriptionSecurityType('JWT')).toBe('JWT');
        });

        it('returns the raw value for unknown types', () => {
            expect(formatSubscriptionSecurityType('CUSTOM')).toBe('CUSTOM');
        });
    });

    describe('canCloseSubscription', () => {
        it('allows close for accepted, pending, and paused', () => {
            expect(canCloseSubscription('ACCEPTED')).toBe(true);
            expect(canCloseSubscription('PENDING')).toBe(true);
            expect(canCloseSubscription('PAUSED')).toBe(true);
        });

        it('disallows close for closed and rejected', () => {
            expect(canCloseSubscription('CLOSED')).toBe(false);
            expect(canCloseSubscription('REJECTED')).toBe(false);
            expect(canCloseSubscription(undefined)).toBe(false);
        });
    });

    describe('mapSubscriptionToTableRow', () => {
        it('returns null when id or status is missing', () => {
            expect(mapSubscriptionToTableRow({ ...baseSubscription, id: undefined }, metadata, 'EXCLUSIVE')).toBeNull();
            expect(mapSubscriptionToTableRow({ ...baseSubscription, status: undefined }, metadata, 'EXCLUSIVE')).toBeNull();
        });

        it('maps API subscription with metadata and exclusive API key mode', () => {
            const row = mapSubscriptionToTableRow(baseSubscription, metadata, 'EXCLUSIVE');
            expect(row).toEqual({
                id: 'sub-1',
                apiName: 'Payments API',
                apiVersion: 'v1',
                referenceTypeLabel: 'API',
                createdAt: 1000,
                endAt: undefined,
                planName: 'Gold',
                securityType: 'API Key',
                isSharedApiKey: false,
                processedAt: undefined,
                startingAt: undefined,
                status: 'ACCEPTED',
                origin: 'MANAGEMENT',
            });
        });

        it('sets isSharedApiKey only for API_KEY plans in SHARED application mode', () => {
            const shared = mapSubscriptionToTableRow(baseSubscription, metadata, 'SHARED');
            const exclusive = mapSubscriptionToTableRow(baseSubscription, metadata, 'EXCLUSIVE');
            const jwtPlan = mapSubscriptionToTableRow(baseSubscription, { 'plan-1': { securityType: 'JWT' }, 'api-1': {} }, 'SHARED');

            expect(shared?.isSharedApiKey).toBe(true);
            expect(exclusive?.isSharedApiKey).toBe(false);
            expect(jwtPlan?.isSharedApiKey).toBe(false);
        });

        it('uses PUSH security label when plan mode is PUSH', () => {
            const row = mapSubscriptionToTableRow(baseSubscription, { 'plan-1': { planMode: 'PUSH', securityType: 'API_KEY' } }, undefined);
            expect(row?.securityType).toBe('PUSH');
        });

        it('maps API product subscriptions using referenceId metadata', () => {
            const productSub: SubscriptionPageItem = {
                ...baseSubscription,
                referenceType: 'API_PRODUCT',
                referenceId: 'product-1',
                api: undefined,
            };
            const row = mapSubscriptionToTableRow(productSub, metadata, undefined);
            expect(row?.referenceTypeLabel).toBe('API Product');
            expect(row?.apiName).toBe('Billing Product');
            expect(row?.apiVersion).toBe('2.0');
        });

        it('falls back to subscription ids when metadata is missing', () => {
            const row = mapSubscriptionToTableRow(baseSubscription, undefined, undefined);
            expect(row?.planName).toBe('plan-1');
            expect(row?.apiName).toBe('api-1');
        });
    });

    describe('mapSubscriptionsPageToRows', () => {
        it('filters out invalid rows', () => {
            const response: ApplicationSubscriptionsPagedResponse = {
                data: [baseSubscription, { plan: 'orphan' }],
                metadata,
                page: { current: 1, size: 10, per_page: 10, total_pages: 1, total_elements: 2 },
            };
            expect(mapSubscriptionsPageToRows(response, 'EXCLUSIVE')).toHaveLength(1);
        });
    });
});
