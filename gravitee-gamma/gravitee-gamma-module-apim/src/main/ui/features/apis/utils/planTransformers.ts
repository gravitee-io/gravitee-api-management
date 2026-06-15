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
    TokenBudgetFormData,
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

/** token-ratelimit only supports SECONDS/MINUTES — convert friendly UI units to minutes. */
function tokenBudgetPeriodInMinutes(data: TokenBudgetFormData): number {
    if (data.unit === 'DAYS') return data.period * 1440;
    if (data.unit === 'HOURS') return data.period * 60;
    return data.period;
}

function buildTokenBudgetFlow(data: TokenBudgetFormData): PolicyFlow {
    return {
        enabled: true,
        request: [
            {
                policy: 'token-ratelimit',
                enabled: true,
                configuration: {
                    // Deterministic 429 on budget exhaustion; addHeaders exposes
                    // X-Token-Rate-Limit-Remaining to consumers.
                    strategy: 'BLOCK_ON_INTERNAL_ERROR',
                    addHeaders: true,
                    rate: {
                        // PER-DEVELOPER limit: the amount comes from each subscription's metadata
                        // (set when adding a developer), so one plan serves N developers with N
                        // personal limits. `data.limit` is the default for devs without an override.
                        // token-ratelimit uses dynamicLimit only when the static limit is 0.
                        limit: 0,
                        dynamicLimit: `{#subscription.metadata['tokenLimit'] ?: ${data.limit}}`,
                        periodTime: tokenBudgetPeriodInMinutes(data),
                        periodTimeUnit: 'MINUTES',
                    },
                },
            },
        ],
    };
}

/** Metadata key carrying a user's personal token budget on their subscription. */
export const DEVELOPER_TOKEN_LIMIT_METADATA_KEY = 'tokenLimit';

/** Metadata key carrying a user's personal request rate limit (requests/minute) on their subscription. */
export const DEVELOPER_RATE_LIMIT_METADATA_KEY = 'rateLimit';

/** Extract the plan-level default token limit from a per-developer dynamicLimit EL expression. */
function tokenBudgetDefaultFromDynamicLimit(dynamicLimit: string | undefined, staticLimit: number): number {
    if (!dynamicLimit) return staticLimit;
    const match = dynamicLimit.match(/\?:\s*(\d+)\s*}/);
    return match ? Number(match[1]) : staticLimit;
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
    } else if (ctx.type === 'api-product') {
        // Product plan flows execute at the gateway for the resolved product subscription.
        if (form.restrictions.rateLimitEnabled) flows.push(buildRateLimitFlow(form.restrictions.rateLimit));
        if (form.restrictions.tokenBudgetEnabled) flows.push(buildTokenBudgetFlow(form.restrictions.tokenBudget));
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

type TokenBudgetConfig = {
    rate?: { limit?: number; dynamicLimit?: string; periodTime?: number; periodTimeUnit?: string };
};

/** Convert a period stored in minutes back to the friendliest UI unit. */
function tokenBudgetFromConfig(config: TokenBudgetConfig | undefined): TokenBudgetFormData {
    const limit = tokenBudgetDefaultFromDynamicLimit(config?.rate?.dynamicLimit, config?.rate?.limit ?? EMPTY_R.tokenBudget.limit);
    let minutes = config?.rate?.periodTime ?? 1440;
    if (config?.rate?.periodTimeUnit === 'SECONDS') minutes = Math.max(1, Math.round(minutes / 60));
    if (minutes % 1440 === 0) return { limit, period: minutes / 1440, unit: 'DAYS' };
    if (minutes % 60 === 0) return { limit, period: minutes / 60, unit: 'HOURS' };
    return { limit, period: minutes, unit: 'MINUTES' };
}

export function planToFormValue(plan: ManagedPlan): PlanFormValue {
    const rateLimitStep = findStep(plan.flows ?? [], 'rate-limit');
    const quotaStep = findStep(plan.flows ?? [], 'quota');
    const tokenBudgetStep = findStep(plan.flows ?? [], 'token-ratelimit');
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
            tokenBudgetEnabled: Boolean(tokenBudgetStep),
            tokenBudget: tokenBudgetFromConfig(tokenBudgetStep?.configuration as TokenBudgetConfig | undefined),
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

// ─── Display helpers (plan table columns) ─────────────────────────────────────

function formatCount(value: number): string {
    if (value >= 1_000_000 && value % 100_000 === 0) return `${value / 1_000_000}M`;
    if (value >= 1_000 && value % 100 === 0) return `${value / 1_000}K`;
    return value.toLocaleString();
}

function formatPeriod(period: number, unit: string): string {
    const label = unit.toLowerCase().replace(/s$/, '');
    return period === 1 ? label : `${period} ${label}s`;
}

/** "60 / minute" from the plan's rate-limit flow, or null when no rate limit is set. */
export function formatPlanRateLimit(plan: ManagedPlan): string | null {
    const step = findStep(plan.flows ?? [], 'rate-limit');
    const rate = (step?.configuration as { rate?: { limit?: number; periodTime?: number; periodTimeUnit?: string } } | undefined)?.rate;
    if (!rate?.limit) return null;
    return `${formatCount(rate.limit)} / ${formatPeriod(rate.periodTime ?? 1, rate.periodTimeUnit ?? 'SECONDS')}`;
}

/** "500K / day" from the plan's token-ratelimit flow, or null when no token budget is set. */
export function formatPlanTokenBudget(plan: ManagedPlan): string | null {
    const step = findStep(plan.flows ?? [], 'token-ratelimit');
    if (!step) return null;
    const budget = tokenBudgetFromConfig(step.configuration as TokenBudgetConfig);
    if (!budget.limit) return null;
    return `${formatCount(budget.limit)} / ${formatPeriod(budget.period, budget.unit)}`;
}
