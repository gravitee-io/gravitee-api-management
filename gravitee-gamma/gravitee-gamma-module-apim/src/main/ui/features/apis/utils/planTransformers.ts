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
import type {
    ManagedPlan,
    PlanContext,
    PlanFormValue,
    PolicyFlow,
    PolicyStep,
    ResourceFilteringRule,
    RestrictionsFormData,
} from '../types/plan';
import { EMPTY_RESTRICTIONS as EMPTY_R } from '../types/plan';

// ─── Restriction flow helpers ─────────────────────────────────────────────────

function findStep(flows: PolicyFlow[], policyName: string): PolicyStep | undefined {
    for (const flow of flows) {
        const step = flow.request?.find(s => s.policy === policyName && s.enabled);
        if (step) return step;
    }
    return undefined;
}

function buildRateLimitFlow(data: RestrictionsFormData['rateLimit']): PolicyFlow {
    return {
        enabled: true,
        request: [
            {
                policy: 'rate-limit',
                enabled: true,
                configuration: {
                    rate: { limit: data.max, periodTime: data.period, periodTimeUnit: data.unit },
                    addHeaders: false,
                },
            },
        ],
    };
}

function buildQuotaFlow(data: RestrictionsFormData['quota']): PolicyFlow {
    return {
        enabled: true,
        request: [
            {
                policy: 'quota',
                enabled: true,
                configuration: {
                    quota: { limit: data.max, periodTime: data.period, periodTimeUnit: data.unit },
                    addHeaders: false,
                },
            },
        ],
    };
}

function buildResourceFilteringFlow(rules: ResourceFilteringRule[]): PolicyFlow {
    return {
        enabled: true,
        request: [
            {
                policy: 'resource-filtering',
                enabled: true,
                configuration: {
                    whitelist: rules.map(r => ({ pattern: r.pattern, methods: r.methods, enabled: r.whitelist })),
                },
            },
        ],
    };
}

// ─── Public transformers ───────────────────────────────────────────────────────

export function planFormToPayload(form: PlanFormValue, ctx: PlanContext): Omit<ManagedPlan, 'id' | 'order'> {
    const isKeyless = form.securityType === 'KEY_LESS';

    const characteristics = form.general.characteristics
        .split(',')
        .map(s => s.trim())
        .filter(Boolean);

    const flows: PolicyFlow[] = [];
    if (ctx.type === 'api') {
        if (form.restrictions.rateLimitEnabled) flows.push(buildRateLimitFlow(form.restrictions.rateLimit));
        if (form.restrictions.quotaEnabled) flows.push(buildQuotaFlow(form.restrictions.quota));
        if (form.restrictions.resourceFilteringEnabled && form.restrictions.resourceFiltering.length > 0) {
            flows.push(buildResourceFilteringFlow(form.restrictions.resourceFiltering));
        }
    }

    const payload: Omit<ManagedPlan, 'id' | 'order'> = {
        name: form.general.name.trim(),
        description: form.general.description.trim() || undefined,
        characteristics: characteristics.length > 0 ? characteristics : undefined,
        generalConditions: ctx.type === 'api' ? form.general.generalConditions || undefined : undefined,
        validation: isKeyless ? 'AUTO' : form.general.autoValidation ? 'AUTO' : 'MANUAL',
        commentRequired: isKeyless ? false : form.general.commentRequired,
        commentMessage: !isKeyless && form.general.commentRequired ? form.general.commentMessage.trim() || undefined : undefined,
        excludedGroups: ctx.type === 'api' && form.general.excludedGroups.length > 0 ? form.general.excludedGroups : undefined,
        status: 'STAGING',
        security: {
            type: form.securityType,
            configuration: isKeyless ? {} : form.security.configuration,
        },
        selectionRule: !isKeyless && form.security.selectionRule.trim() ? form.security.selectionRule.trim() : undefined,
        flows: flows.length > 0 ? flows : undefined,
        definitionVersion: 'V4',
        mode: 'STANDARD',
    };

    return payload;
}

export function planToFormValue(plan: ManagedPlan): PlanFormValue {
    const rateLimitStep = findStep(plan.flows ?? [], 'rate-limit');
    const quotaStep = findStep(plan.flows ?? [], 'quota');
    const rfStep = findStep(plan.flows ?? [], 'resource-filtering');

    const rlConfig = rateLimitStep?.configuration as
        | { rate?: { limit?: number; periodTime?: number; periodTimeUnit?: string } }
        | undefined;
    const qConfig = quotaStep?.configuration as { quota?: { limit?: number; periodTime?: number; periodTimeUnit?: string } } | undefined;
    const rfConfig = rfStep?.configuration as { whitelist?: { pattern?: string; methods?: string[]; enabled?: boolean }[] } | undefined;

    return {
        securityType: plan.security.type,
        general: {
            name: plan.name,
            description: plan.description ?? '',
            characteristics: (plan.characteristics ?? []).join(', '),
            generalConditions: plan.generalConditions ?? '',
            autoValidation: plan.validation === 'AUTO',
            commentRequired: plan.commentRequired ?? false,
            commentMessage: plan.commentMessage ?? '',
            excludedGroups: plan.excludedGroups ?? [],
        },
        security: {
            configuration: plan.security.configuration ?? {},
            selectionRule: plan.selectionRule ?? '',
        },
        restrictions: {
            rateLimitEnabled: Boolean(rateLimitStep),
            rateLimit: {
                max: rlConfig?.rate?.limit ?? EMPTY_R.rateLimit.max,
                period: rlConfig?.rate?.periodTime ?? EMPTY_R.rateLimit.period,
                unit: (rlConfig?.rate?.periodTimeUnit as RestrictionsFormData['rateLimit']['unit']) ?? EMPTY_R.rateLimit.unit,
            },
            quotaEnabled: Boolean(quotaStep),
            quota: {
                max: qConfig?.quota?.limit ?? EMPTY_R.quota.max,
                period: qConfig?.quota?.periodTime ?? EMPTY_R.quota.period,
                unit: (qConfig?.quota?.periodTimeUnit as RestrictionsFormData['quota']['unit']) ?? EMPTY_R.quota.unit,
            },
            resourceFilteringEnabled: Boolean(rfStep),
            resourceFiltering: (rfConfig?.whitelist ?? []).map(r => ({
                whitelist: r.enabled ?? true,
                pattern: r.pattern ?? '',
                methods: r.methods ?? [],
            })),
        },
    };
}
