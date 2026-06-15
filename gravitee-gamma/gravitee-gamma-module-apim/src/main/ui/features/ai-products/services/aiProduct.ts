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
import { apimFetchJsonV2 } from '../../../shared/api/apimClient';
import type { ApiProductListItem } from '../../api-products/types/apiProduct';
import type { ApiListResponse } from '../../apis/types';
import { DEVELOPER_RATE_LIMIT_METADATA_KEY, DEVELOPER_TOKEN_LIMIT_METADATA_KEY } from '../../apis/utils/planTransformers';
import type { LlmModel } from '../types/aiProduct';

const JSON_HEADERS = { 'Content-Type': 'application/json' };

/** Name of the single hidden plan every AI Product gets, so admins never manage plans by hand. */
const DEFAULT_PLAN_NAME = 'User Access';

/** Security scheme for the auto-managed access plan. API Key is the default; JWT/mTLS are supported types. */
export type AiProductPlanSecurity = 'API_KEY' | 'JWT' | 'MTLS';

/** The budget reset window for the access plan — same period flexibility as the token-ratelimit policy. */
export type BudgetWindow = 'MINUTE' | 'HOUR' | 'DAY' | 'WEEK' | 'MONTH';

/** token-ratelimit takes a periodTime in MINUTES, so each window is expressed in minutes. */
const WINDOW_MINUTES: Record<BudgetWindow, number> = { MINUTE: 1, HOUR: 60, DAY: 1440, WEEK: 10080, MONTH: 43200 };
export const WINDOW_LABEL: Record<BudgetWindow, string> = {
    MINUTE: 'per minute',
    HOUR: 'per hour',
    DAY: 'per day',
    WEEK: 'per week',
    MONTH: 'per month',
};

interface ProductPlan {
    id: string;
    name: string;
    status?: string;
    security?: { type?: string };
}

function windowPlanName(window: BudgetWindow): string {
    return `${DEFAULT_PLAN_NAME} (${WINDOW_LABEL[window]})`;
}

/**
 * Ensure the AI Product has a published access plan for the given reset window, creating it if needed.
 *
 * The admin only ever picks a window (per day / week / month) when onboarding a user; the matching plan
 * is created and managed automatically. The plan is API-key based with token-budget + rate-limit policies
 * whose limits are read PER USER from subscription metadata (`dynamicLimit`), so ONE plan per window serves
 * every user on it with their own budget. Users needing a different window simply get a different plan.
 */
export async function ensurePlanForWindow(
    environmentId: string,
    productId: string,
    window: BudgetWindow = 'MONTH',
    security: AiProductPlanSecurity = 'API_KEY',
): Promise<string> {
    const planName = windowPlanName(window);
    const existing = await apimFetchJsonV2<{ data: ProductPlan[] }>(
        environmentId,
        `/api-products/${productId}/plans?statuses=PUBLISHED&perPage=100`,
    );
    const reusable = (existing.data ?? []).find(p => p.name === planName && p.security?.type === security);
    if (reusable) return reusable.id;

    const created = await apimFetchJsonV2<ProductPlan>(environmentId, `/api-products/${productId}/plans`, {
        method: 'POST',
        headers: JSON_HEADERS,
        body: JSON.stringify({
            name: planName,
            description: `Auto-managed access plan (token budget resets ${WINDOW_LABEL[window]}). Each user has their own key, budget, and rate limit.`,
            definitionVersion: 'V4',
            mode: 'STANDARD',
            // MANUAL: every subscription lands PENDING so the admin approves it and sets that
            // user's personal token budget + rate limit before access is granted.
            validation: 'MANUAL',
            security: { type: security, configuration: {} },
            flows: [
                {
                    name: 'Per-user limits',
                    enabled: true,
                    request: [
                        {
                            name: 'Token budget',
                            enabled: true,
                            policy: 'token-ratelimit',
                            configuration: {
                                strategy: 'BLOCK_ON_INTERNAL_ERROR',
                                addHeaders: true,
                                rate: {
                                    limit: 0,
                                    // Per-user budget from subscription metadata; large fallback for users without one.
                                    dynamicLimit: `{#subscription.metadata['${DEVELOPER_TOKEN_LIMIT_METADATA_KEY}'] ?: 100000000}`,
                                    periodTime: WINDOW_MINUTES[window],
                                    periodTimeUnit: 'MINUTES',
                                },
                            },
                        },
                        {
                            name: 'Request rate limit',
                            enabled: true,
                            policy: 'rate-limit',
                            configuration: {
                                errorStrategy: 'BLOCK_ON_INTERNAL_ERROR',
                                addHeaders: true,
                                rate: {
                                    limit: 0,
                                    // Per-user requests/minute from subscription metadata.
                                    dynamicLimit: `{#subscription.metadata['${DEVELOPER_RATE_LIMIT_METADATA_KEY}'] ?: 1000000}`,
                                    periodTime: 1,
                                    periodTimeUnit: 'MINUTES',
                                },
                            },
                        },
                    ],
                },
            ],
        }),
    });

    await apimFetchJsonV2<void>(environmentId, `/api-products/${productId}/plans/${created.id}/_publish`, {
        method: 'POST',
        headers: JSON_HEADERS,
        body: JSON.stringify({}),
    });
    // Deploy so the published plan (with the budget/rate policies) reaches the gateway.
    await deployApiProduct(environmentId, productId).catch(() => undefined);
    return created.id;
}

/**
 * Search V4 LLM-proxy APIs eligible as AI Product components.
 *
 * Any V4 LLM proxy can be bundled — the backend exempts LLM proxies from the "allowed in API products"
 * opt-in. The `_search` index does NOT reliably filter V4 APIs by `apiTypes` (the api_type field isn't
 * indexed for V4 definitions), so we query by text and filter to V4 LLM proxies client-side using the
 * `type` each result already carries.
 */
export async function searchLlmComponents(environmentId: string, query: string, page: number, perPage: number): Promise<ApiListResponse> {
    const params = new URLSearchParams({ page: String(page), perPage: String(perPage) });
    const response = await apimFetchJsonV2<ApiListResponse>(environmentId, `/apis/_search?${params}`, {
        method: 'POST',
        headers: JSON_HEADERS,
        body: JSON.stringify({ query: query || undefined }),
    });
    const data = (response.data ?? []).filter(api => api.type === 'LLM_PROXY' && api.definitionVersion === 'V4');
    return { ...response, data };
}

export async function deployApiProduct(environmentId: string, productId: string): Promise<ApiProductListItem> {
    return apimFetchJsonV2<ApiProductListItem>(environmentId, `/api-products/${productId}/deployments`, {
        method: 'POST',
        headers: JSON_HEADERS,
        body: JSON.stringify({}),
    });
}

interface ApiWithEndpointGroups {
    name?: string;
    type?: string;
    endpointGroups?: {
        type?: string;
        sharedConfiguration?: { models?: LlmModel[]; provider?: string };
        endpoints?: { configuration?: { models?: LlmModel[]; provider?: string } }[];
    }[];
}

/** A model with the upstream provider it is served by (provider lives on the endpoint config). */
export interface ProviderModel extends LlmModel {
    provider: string;
}

/** Friendly label for the provider enum stored on the endpoint connector config. */
export function providerLabel(provider: string | undefined): string {
    switch ((provider ?? '').toUpperCase()) {
        case 'OPEN_AI':
        case 'OPEN_AI_COMPATIBLE':
            return 'OpenAI';
        case 'ANTHROPIC':
            return 'Anthropic';
        case 'GEMINI':
        case 'VERTEX_AI':
            return 'Google';
        case 'BEDROCK':
            return 'AWS Bedrock';
        case 'MISTRAL_AI':
            return 'Mistral';
        default:
            return provider || 'Unknown';
    }
}

function modelsFromApi(api: ApiWithEndpointGroups): ProviderModel[] {
    const out: ProviderModel[] = [];
    for (const group of api.endpointGroups ?? []) {
        for (const endpoint of group.endpoints ?? []) {
            const provider = endpoint.configuration?.provider ?? group.sharedConfiguration?.provider ?? 'Unknown';
            for (const model of endpoint.configuration?.models ?? group.sharedConfiguration?.models ?? []) {
                if (model?.name) out.push({ ...model, provider });
            }
        }
    }
    return out;
}

/**
 * Reads the models served by an LLM proxy from its endpoint connector configuration
 * (models may live on the group's sharedConfiguration or on each endpoint).
 */
export async function getComponentModels(environmentId: string, apiId: string): Promise<LlmModel[]> {
    const api = await apimFetchJsonV2<ApiWithEndpointGroups>(environmentId, `/apis/${apiId}`);
    const models = modelsFromApi(api);
    const seen = new Set<string>();
    return models.filter(model => !seen.has(model.name) && Boolean(seen.add(model.name)));
}

/**
 * Aggregates the models exposed by all of a product's LLM proxies, tagged by upstream provider,
 * so the admin sees the full multi-provider catalog (OpenAI + Anthropic + Google …) in one place.
 */
export async function getProductModels(environmentId: string, apiIds: string[]): Promise<ProviderModel[]> {
    const apis = await Promise.all(
        apiIds.map(id => apimFetchJsonV2<ApiWithEndpointGroups>(environmentId, `/apis/${id}`).catch(() => null)),
    );
    const all = apis.filter((a): a is ApiWithEndpointGroups => a !== null).flatMap(modelsFromApi);
    const seen = new Set<string>();
    return all.filter(model => {
        const key = `${model.provider}:${model.name}`;
        return !seen.has(key) && Boolean(seen.add(key));
    });
}
