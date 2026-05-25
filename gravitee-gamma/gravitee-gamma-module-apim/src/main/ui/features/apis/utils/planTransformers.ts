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
    QuotaFormData,
    RateLimitErrorStrategy,
    RateLimitFormData,
    ResourceFilteringRule,
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

function buildRateLimitFlow(data: RateLimitFormData): PolicyFlow {
    return {
        enabled: true,
        request: [
            {
                policy: 'rate-limit',
                enabled: true,
                configuration: {
                    errorStrategy: data.errorStrategy,
                    async: data.async,
                    addHeaders: data.addHeaders,
                    rate: {
                        key: data.key || undefined,
                        useKeyOnly: data.key ? data.useKeyOnly : false,
                        limit: data.max,
                        dynamicLimit: data.dynamicLimit || undefined,
                        periodTime: data.period,
                        periodTimeUnit: data.unit,
                        dynamicPeriodTime: data.dynamicPeriodTime || undefined,
                    },
                },
            },
        ],
    };
}

function buildQuotaFlow(data: QuotaFormData): PolicyFlow {
    return {
        enabled: true,
        request: [
            {
                policy: 'quota',
                enabled: true,
                configuration: {
                    errorStrategy: data.errorStrategy,
                    async: data.async,
                    addHeaders: data.addHeaders,
                    quota: {
                        key: data.key || undefined,
                        useKeyOnly: data.key ? data.useKeyOnly : false,
                        limit: data.max,
                        dynamicLimit: data.dynamicLimit || undefined,
                        periodTime: data.period,
                        periodTimeUnit: data.unit,
                        dynamicPeriodTime: data.dynamicPeriodTime || undefined,
                    },
                },
            },
        ],
    };
}

function buildResourceFilteringFlow(
    rules: ResourceFilteringRule[],
    normalizeRequestPath: boolean,
    decodeEncodedSlash: boolean,
): PolicyFlow {
    return {
        enabled: true,
        request: [
            {
                policy: 'resource-filtering',
                enabled: true,
                configuration: {
                    whitelist: rules.filter(r => r.whitelist).map(r => ({ pattern: r.pattern, methods: r.methods })),
                    blacklist: rules.filter(r => !r.whitelist).map(r => ({ pattern: r.pattern, methods: r.methods })),
                    normalizeRequestPath,
                    decodeEncodedSlash: normalizeRequestPath ? decodeEncodedSlash : false,
                },
            },
        ],
    };
}

// ─── Public transformers ───────────────────────────────────────────────────────

export function planFormToPayload(form: PlanFormValue, ctx: PlanContext): Omit<ManagedPlan, 'id' | 'order'> {
    const isKeyless = form.securityType === 'KEY_LESS';

    const characteristics = form.general.characteristics;

    const flows: PolicyFlow[] = [];
    if (ctx.type === 'api') {
        if (form.restrictions.rateLimitEnabled) flows.push(buildRateLimitFlow(form.restrictions.rateLimit));
        if (form.restrictions.quotaEnabled) flows.push(buildQuotaFlow(form.restrictions.quota));
        if (form.restrictions.resourceFilteringEnabled) {
            flows.push(
                buildResourceFilteringFlow(
                    form.restrictions.resourceFiltering,
                    form.restrictions.normalizeRequestPath,
                    form.restrictions.decodeEncodedSlash,
                ),
            );
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
        tags: form.general.tags.length > 0 ? form.general.tags : undefined,
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
        | {
              errorStrategy?: string;
              async?: boolean;
              addHeaders?: boolean;
              rate?: {
                  key?: string;
                  useKeyOnly?: boolean;
                  limit?: number;
                  dynamicLimit?: string;
                  periodTime?: number;
                  periodTimeUnit?: string;
                  dynamicPeriodTime?: string;
              };
          }
        | undefined;
    const qConfig = quotaStep?.configuration as
        | {
              errorStrategy?: string;
              async?: boolean;
              addHeaders?: boolean;
              quota?: {
                  key?: string;
                  useKeyOnly?: boolean;
                  limit?: number;
                  dynamicLimit?: string;
                  periodTime?: number;
                  periodTimeUnit?: string;
                  dynamicPeriodTime?: string;
              };
          }
        | undefined;
    const rfConfig = rfStep?.configuration as
        | {
              whitelist?: { pattern?: string; methods?: string[] }[];
              blacklist?: { pattern?: string; methods?: string[] }[];
              normalizeRequestPath?: boolean;
              decodeEncodedSlash?: boolean;
          }
        | undefined;

    return {
        securityType: plan.security.type,
        general: {
            name: plan.name,
            description: plan.description ?? '',
            characteristics: plan.characteristics ?? [],
            generalConditions: plan.generalConditions ?? '',
            autoValidation: plan.validation === 'AUTO',
            commentRequired: plan.commentRequired ?? false,
            commentMessage: plan.commentMessage ?? '',
            excludedGroups: plan.excludedGroups ?? [],
            tags: plan.tags ?? [],
        },
        security: {
            configuration: plan.security.configuration ?? {},
            selectionRule: plan.selectionRule ?? '',
        },
        restrictions: {
            rateLimitEnabled: Boolean(rateLimitStep),
            rateLimit: {
                errorStrategy: (rlConfig?.errorStrategy as RateLimitErrorStrategy) ?? EMPTY_R.rateLimit.errorStrategy,
                async: rlConfig?.async ?? EMPTY_R.rateLimit.async,
                addHeaders: rlConfig?.addHeaders ?? EMPTY_R.rateLimit.addHeaders,
                key: rlConfig?.rate?.key ?? '',
                useKeyOnly: rlConfig?.rate?.useKeyOnly ?? false,
                max: rlConfig?.rate?.limit ?? EMPTY_R.rateLimit.max,
                dynamicLimit: rlConfig?.rate?.dynamicLimit ?? '',
                period: rlConfig?.rate?.periodTime ?? EMPTY_R.rateLimit.period,
                unit: (rlConfig?.rate?.periodTimeUnit as RateLimitFormData['unit']) ?? EMPTY_R.rateLimit.unit,
                dynamicPeriodTime: rlConfig?.rate?.dynamicPeriodTime ?? '',
            },
            quotaEnabled: Boolean(quotaStep),
            quota: {
                errorStrategy: (qConfig?.errorStrategy as RateLimitErrorStrategy) ?? EMPTY_R.quota.errorStrategy,
                async: qConfig?.async ?? EMPTY_R.quota.async,
                addHeaders: qConfig?.addHeaders ?? EMPTY_R.quota.addHeaders,
                key: qConfig?.quota?.key ?? '',
                useKeyOnly: qConfig?.quota?.useKeyOnly ?? false,
                max: qConfig?.quota?.limit ?? EMPTY_R.quota.max,
                dynamicLimit: qConfig?.quota?.dynamicLimit ?? '',
                period: qConfig?.quota?.periodTime ?? EMPTY_R.quota.period,
                unit: (qConfig?.quota?.periodTimeUnit as QuotaFormData['unit']) ?? EMPTY_R.quota.unit,
                dynamicPeriodTime: qConfig?.quota?.dynamicPeriodTime ?? '',
            },
            resourceFilteringEnabled: Boolean(rfStep),
            resourceFiltering: [
                ...(rfConfig?.whitelist ?? []).map(r => ({ whitelist: true, pattern: r.pattern ?? '', methods: r.methods ?? [] })),
                ...(rfConfig?.blacklist ?? []).map(r => ({ whitelist: false, pattern: r.pattern ?? '', methods: r.methods ?? [] })),
            ],
            normalizeRequestPath: rfConfig?.normalizeRequestPath ?? false,
            decodeEncodedSlash: rfConfig?.decodeEncodedSlash ?? false,
        },
    };
}
