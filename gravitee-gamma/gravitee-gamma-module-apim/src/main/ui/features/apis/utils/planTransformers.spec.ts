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
import { planFormToPayload, planToFormValue } from './planTransformers';
import { EMPTY_GENERAL, EMPTY_RESTRICTIONS, EMPTY_SECURITY } from '../types/plan';
import type { ManagedPlan, PlanContext, PlanFormValue } from '../types/plan';

const API_CTX: PlanContext = { type: 'api', entityId: 'api-1' };
const API_PRODUCT_CTX: PlanContext = { type: 'api-product', entityId: 'product-1' };

const SAVED_PLAN: ManagedPlan = {
    id: 'plan-1',
    name: 'Restricted Gold',
    status: 'PUBLISHED',
    order: 1,
    validation: 'MANUAL',
    security: { type: 'API_KEY', configuration: {} },
};

const FEDERATED_PLAN_FORM: PlanFormValue = {
    securityType: 'API_KEY',
    general: {
        ...EMPTY_GENERAL,
        name: 'Provider Gold',
        description: 'Plan owned by the upstream provider',
        characteristics: ['gold'],
        autoValidation: true,
    },
    security: { ...EMPTY_SECURITY },
    restrictions: { ...EMPTY_RESTRICTIONS },
};

describe('planFormToPayload', () => {
    it('sends the whole plan body under the definition version it was given', () => {
        expect(planFormToPayload(FEDERATED_PLAN_FORM, API_CTX, 'FEDERATED')).toEqual({
            name: 'Provider Gold',
            description: 'Plan owned by the upstream provider',
            characteristics: ['gold'],
            generalConditions: undefined,
            validation: 'AUTO',
            commentRequired: false,
            commentMessage: undefined,
            excludedGroups: undefined,
            tags: undefined,
            status: 'STAGING',
            security: { type: 'API_KEY', configuration: {} },
            selectionRule: undefined,
            flows: undefined,
            definitionVersion: 'FEDERATED',
            mode: 'STANDARD',
        });
    });

    it('drops every subscriber-facing setting a keyless plan cannot honour', () => {
        const keylessForm: PlanFormValue = {
            securityType: 'KEY_LESS',
            general: {
                ...EMPTY_GENERAL,
                name: 'Open access',
                autoValidation: false,
                commentRequired: true,
                commentMessage: 'Tell us who you are',
            },
            security: { configuration: { propagateAuthHeader: false }, selectionRule: "#context.attributes['tier'] == 'gold'" },
            restrictions: { ...EMPTY_RESTRICTIONS },
        };

        const payload = planFormToPayload(keylessForm, API_CTX, 'V4');

        expect(payload.validation).toBe('AUTO');
        expect(payload.commentRequired).toBe(false);
        expect(payload.commentMessage).toBeUndefined();
        expect(payload.security).toEqual({ type: 'KEY_LESS', configuration: {} });
        expect(payload.selectionRule).toBeUndefined();
    });

    it('withholds the API-only fields from a plan written in an API Product context', () => {
        const productForm: PlanFormValue = {
            securityType: 'API_KEY',
            general: {
                ...EMPTY_GENERAL,
                name: 'Product Gold',
                generalConditions: 'terms-page-id',
                excludedGroups: ['group-1'],
            },
            security: { ...EMPTY_SECURITY },
            restrictions: { ...EMPTY_RESTRICTIONS, rateLimitEnabled: true },
        };

        const payload = planFormToPayload(productForm, API_PRODUCT_CTX, 'V4');

        expect(payload.flows).toBeUndefined();
        expect(payload.generalConditions).toBeUndefined();
        expect(payload.excludedGroups).toBeUndefined();
    });

    it('emits one policy flow per enabled restriction, with the keys and path guards resolved', () => {
        const restrictedForm: PlanFormValue = {
            securityType: 'API_KEY',
            general: { ...EMPTY_GENERAL, name: 'Restricted' },
            security: { ...EMPTY_SECURITY },
            restrictions: {
                ...EMPTY_RESTRICTIONS,
                rateLimitEnabled: true,
                rateLimit: { ...EMPTY_RESTRICTIONS.rateLimit, key: '', useKeyOnly: true },
                quotaEnabled: true,
                quota: { ...EMPTY_RESTRICTIONS.quota, key: '', useKeyOnly: true },
                resourceFilteringEnabled: true,
                resourceFiltering: [
                    { whitelist: true, pattern: '/public/**', methods: ['GET'] },
                    { whitelist: false, pattern: '/admin/**', methods: ['POST', 'DELETE'] },
                ],
                normalizeRequestPath: false,
                decodeEncodedSlash: true,
            },
        };

        const payload = planFormToPayload(restrictedForm, API_CTX, 'V4');

        expect(payload.flows).toEqual([
            {
                enabled: true,
                request: [
                    {
                        policy: 'rate-limit',
                        enabled: true,
                        configuration: {
                            errorStrategy: 'FALLBACK_PASS_TROUGH',
                            async: false,
                            addHeaders: false,
                            rate: {
                                key: undefined,
                                useKeyOnly: false,
                                limit: 10,
                                dynamicLimit: undefined,
                                periodTime: 1,
                                periodTimeUnit: 'SECONDS',
                                dynamicPeriodTime: undefined,
                            },
                        },
                    },
                ],
            },
            {
                enabled: true,
                request: [
                    {
                        policy: 'quota',
                        enabled: true,
                        configuration: {
                            errorStrategy: 'FALLBACK_PASS_TROUGH',
                            async: false,
                            addHeaders: true,
                            quota: {
                                key: undefined,
                                useKeyOnly: false,
                                limit: 100,
                                dynamicLimit: undefined,
                                periodTime: 1,
                                periodTimeUnit: 'HOURS',
                                dynamicPeriodTime: undefined,
                            },
                        },
                    },
                ],
            },
            {
                enabled: true,
                request: [
                    {
                        policy: 'resource-filtering',
                        enabled: true,
                        configuration: {
                            whitelist: [{ pattern: '/public/**', methods: ['GET'] }],
                            blacklist: [{ pattern: '/admin/**', methods: ['POST', 'DELETE'] }],
                            normalizeRequestPath: false,
                            decodeEncodedSlash: false,
                        },
                    },
                ],
            },
        ]);
    });
});

describe('planToFormValue', () => {
    it('reads every restriction setting back out of the saved policy flows', () => {
        const plan: ManagedPlan = {
            ...SAVED_PLAN,
            flows: [
                {
                    enabled: true,
                    request: [
                        {
                            policy: 'rate-limit',
                            enabled: true,
                            configuration: {
                                errorStrategy: 'BLOCK_ON_INTERNAL_ERROR',
                                async: true,
                                addHeaders: false,
                                rate: {
                                    key: "#request.headers['X-Tenant']",
                                    useKeyOnly: true,
                                    limit: 37,
                                    dynamicLimit: '{#context.attributes.rateMax}',
                                    periodTime: 45,
                                    periodTimeUnit: 'SECONDS',
                                    dynamicPeriodTime: '{#context.attributes.ratePeriod}',
                                },
                            },
                        },
                    ],
                },
                {
                    enabled: true,
                    request: [
                        {
                            policy: 'quota',
                            enabled: true,
                            configuration: {
                                errorStrategy: 'FALLBACK_PASS_TROUGH',
                                async: false,
                                addHeaders: true,
                                quota: {
                                    key: "#request.headers['X-Account']",
                                    useKeyOnly: false,
                                    limit: 9000,
                                    dynamicLimit: '{#context.attributes.quotaMax}',
                                    periodTime: 3,
                                    periodTimeUnit: 'MONTHS',
                                    dynamicPeriodTime: '{#context.attributes.quotaPeriod}',
                                },
                            },
                        },
                    ],
                },
                {
                    enabled: true,
                    request: [
                        {
                            policy: 'resource-filtering',
                            enabled: true,
                            configuration: {
                                whitelist: [{ pattern: '/public/**', methods: ['GET'] }],
                                blacklist: [{ pattern: '/admin/**', methods: ['POST', 'DELETE'] }],
                                normalizeRequestPath: true,
                                decodeEncodedSlash: true,
                            },
                        },
                    ],
                },
            ],
        };

        expect(planToFormValue(plan).restrictions).toEqual({
            rateLimitEnabled: true,
            rateLimit: {
                errorStrategy: 'BLOCK_ON_INTERNAL_ERROR',
                async: true,
                addHeaders: false,
                key: "#request.headers['X-Tenant']",
                useKeyOnly: true,
                max: 37,
                dynamicLimit: '{#context.attributes.rateMax}',
                period: 45,
                unit: 'SECONDS',
                dynamicPeriodTime: '{#context.attributes.ratePeriod}',
            },
            quotaEnabled: true,
            quota: {
                errorStrategy: 'FALLBACK_PASS_TROUGH',
                async: false,
                addHeaders: true,
                key: "#request.headers['X-Account']",
                useKeyOnly: false,
                max: 9000,
                dynamicLimit: '{#context.attributes.quotaMax}',
                period: 3,
                unit: 'MONTHS',
                dynamicPeriodTime: '{#context.attributes.quotaPeriod}',
            },
            resourceFilteringEnabled: true,
            resourceFiltering: [
                { whitelist: true, pattern: '/public/**', methods: ['GET'] },
                { whitelist: false, pattern: '/admin/**', methods: ['POST', 'DELETE'] },
            ],
            normalizeRequestPath: true,
            decodeEncodedSlash: true,
        });
    });

    it('offers the wizard defaults when the plan carries no policy flow', () => {
        expect(planToFormValue({ ...SAVED_PLAN, flows: [] }).restrictions).toEqual({
            rateLimitEnabled: false,
            rateLimit: {
                errorStrategy: 'FALLBACK_PASS_TROUGH',
                async: false,
                addHeaders: false,
                key: '',
                useKeyOnly: false,
                max: 10,
                dynamicLimit: '',
                period: 1,
                unit: 'SECONDS',
                dynamicPeriodTime: '',
            },
            quotaEnabled: false,
            quota: {
                errorStrategy: 'FALLBACK_PASS_TROUGH',
                async: false,
                addHeaders: true,
                key: '',
                useKeyOnly: false,
                max: 100,
                dynamicLimit: '',
                period: 1,
                unit: 'HOURS',
                dynamicPeriodTime: '',
            },
            resourceFilteringEnabled: false,
            resourceFiltering: [],
            normalizeRequestPath: false,
            decodeEncodedSlash: false,
        });
    });

    it('offers the wizard defaults when the plan has no flows key at all', () => {
        const restrictions = planToFormValue(SAVED_PLAN).restrictions;

        expect(restrictions.rateLimitEnabled).toBe(false);
        expect(restrictions.quotaEnabled).toBe(false);
        expect(restrictions.resourceFilteringEnabled).toBe(false);
        expect(restrictions.resourceFiltering).toEqual([]);
    });
});
