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
    buildSubscriptionCreatePayload,
    getSubscriptionCreateValidationError,
    isPlanDisabled,
    planDisabledReason,
    shouldShowApiKeyModeChoice,
    subscriptionMatchesPlan,
} from './applicationSubscriptionCreateUtils';
import type { SubscribablePlan, SubscriptionPageItem } from '../types/applicationSubscription';

const enabledPlan: SubscribablePlan = { id: 'p1', name: 'Standard', security: { type: 'API_KEY' } };

const apiReference = { type: 'API' as const, id: 'api-1', name: 'Payments' };
const productReference = { type: 'API_PRODUCT' as const, id: 'prod-1', name: 'Billing' };

describe('applicationSubscriptionCreateUtils', () => {
    describe('subscriptionMatchesPlan', () => {
        it('matches API subscriptions by api id', () => {
            expect(subscriptionMatchesPlan({ api: 'api-1' }, apiReference)).toBe(true);
            expect(subscriptionMatchesPlan({ api: 'other' }, apiReference)).toBe(false);
        });

        it('matches API product subscriptions by reference', () => {
            expect(subscriptionMatchesPlan({ referenceType: 'API_PRODUCT', referenceId: 'prod-1' }, productReference)).toBe(true);
            expect(subscriptionMatchesPlan({ referenceType: 'API_PRODUCT', referenceId: 'other' }, productReference)).toBe(false);
        });
    });

    describe('shouldShowApiKeyModeChoice', () => {
        const existingApiKeySub: SubscriptionPageItem = { id: 's1', api: 'other-api', status: 'ACCEPTED' };

        it('shows choice for UNSPECIFIED app with existing API key sub on another API', () => {
            expect(
                shouldShowApiKeyModeChoice({
                    applicationApiKeyMode: 'UNSPECIFIED',
                    planSecurityType: 'API_KEY',
                    canUseSharedApiKeys: true,
                    isFederatedApi: false,
                    selectedReference: apiReference,
                    apiKeySubscriptions: [existingApiKeySub],
                }),
            ).toBe(true);
        });

        it('hides choice for first API key subscription', () => {
            expect(
                shouldShowApiKeyModeChoice({
                    applicationApiKeyMode: 'UNSPECIFIED',
                    planSecurityType: 'API_KEY',
                    canUseSharedApiKeys: true,
                    isFederatedApi: false,
                    selectedReference: apiReference,
                    apiKeySubscriptions: [],
                }),
            ).toBe(false);
        });

        it('hides choice when shared API keys are disabled', () => {
            expect(
                shouldShowApiKeyModeChoice({
                    applicationApiKeyMode: 'UNSPECIFIED',
                    planSecurityType: 'API_KEY',
                    canUseSharedApiKeys: false,
                    isFederatedApi: false,
                    selectedReference: apiReference,
                    apiKeySubscriptions: [existingApiKeySub],
                }),
            ).toBe(false);
        });
    });

    describe('buildSubscriptionCreatePayload', () => {
        it('includes apiKeyMode only when the choice is shown', () => {
            expect(buildSubscriptionCreatePayload('hello', true, 'SHARED')).toEqual({
                request: 'hello',
                apiKeyMode: 'SHARED',
            });
            expect(buildSubscriptionCreatePayload('hello', false, 'SHARED')).toEqual({ request: 'hello' });
        });
    });

    it('disables plans with general conditions or PUSH mode', () => {
        expect(isPlanDisabled({ ...enabledPlan, generalConditions: 'terms' })).toBe(true);
        expect(isPlanDisabled({ ...enabledPlan, mode: 'PUSH' })).toBe(true);
        expect(isPlanDisabled(enabledPlan)).toBe(false);
    });

    it('returns human-readable disabled reasons', () => {
        expect(planDisabledReason({ ...enabledPlan, generalConditions: 'x' })).toContain('portal');
        expect(planDisabledReason({ ...enabledPlan, mode: 'PUSH' })).toContain('Push plan');
        expect(planDisabledReason(enabledPlan)).toBeUndefined();
    });

    it('requires a request message when commentRequired is set', () => {
        const plan = { ...enabledPlan, commentRequired: true };
        expect(getSubscriptionCreateValidationError(plan, '', false, null)).toContain('message to the API owner');
        expect(getSubscriptionCreateValidationError(plan, 'Need access', false, null)).toBeNull();
    });

    it('requires apiKeyMode when the choice is shown', () => {
        expect(getSubscriptionCreateValidationError(enabledPlan, '', true, null)).toContain('Choose whether');
        expect(getSubscriptionCreateValidationError(enabledPlan, '', true, 'EXCLUSIVE')).toBeNull();
    });
});
