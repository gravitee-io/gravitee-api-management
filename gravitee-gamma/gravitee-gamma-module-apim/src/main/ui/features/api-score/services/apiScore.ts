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
import { searchApis } from '../../apis/services/apiList';
import type { ApiListItem } from '../../apis/types';
import type {
    EnvironmentApiScore,
    EnvironmentApisScoringResponse,
    ImportFunctionRequest,
    ImportRulesetRequest,
    ScoringFunctionsResponse,
    ScoringRuleset,
    ScoringRulesetsResponse,
    UpdateRulesetRequest,
} from '../types/apiScore';

/** Page size used when eagerly collecting every page of a paginated resource. */
const FETCH_ALL_PAGE_SIZE = 100;

// ─── Overview ───────────────────────────────────────────────────────────────

export function getScoredApis(environmentId: string, page: number, perPage: number): Promise<EnvironmentApisScoringResponse> {
    const params = new URLSearchParams({ page: String(page), perPage: String(perPage) });
    return apimFetchJsonV2<EnvironmentApisScoringResponse>(environmentId, `/scoring/apis?${params}`);
}

async function fetchAllPages<T>(fetchPage: (page: number) => Promise<{ data?: T[]; pagination?: { totalCount?: number } }>): Promise<T[]> {
    const all: T[] = [];
    for (let page = 1; ; page++) {
        const res = await fetchPage(page);
        const batch = res.data ?? [];
        all.push(...batch);
        const total = res.pagination?.totalCount ?? all.length;
        if (batch.length === 0 || all.length >= total) break;
    }
    return all;
}

/** Every environment API row returned by `GET /scoring/apis` (includes APIs without a report). */
export async function getAllScoredApis(environmentId: string): Promise<EnvironmentApiScore[]> {
    return fetchAllPages(page => getScoredApis(environmentId, page, FETCH_ALL_PAGE_SIZE));
}

async function getAllV4ProxyApis(environmentId: string): Promise<ApiListItem[]> {
    return fetchAllPages(page => searchApis(environmentId, {}, page, FETCH_ALL_PAGE_SIZE));
}

/** Merges the V4 proxy catalog with the latest per-API score rows from `GET /scoring/apis`. */
export function mergeV4ProxiesWithScores(proxies: ApiListItem[], scored: EnvironmentApiScore[]): EnvironmentApiScore[] {
    const scoreById = new Map(scored.map(row => [row.id, row]));
    return proxies.map(proxy => {
        const report = scoreById.get(proxy.id);
        return {
            id: proxy.id,
            name: proxy.name,
            pictureUrl: proxy._links?.pictureUrl ?? report?.pictureUrl ?? '',
            score: report?.score,
            errors: report?.errors,
            warnings: report?.warnings,
            infos: report?.infos,
            hints: report?.hints,
        };
    });
}

/**
 * V4 HTTP-proxy APIs merged with their latest score row. Uses the proxy list as the source of truth
 * so every managed API appears even when the scoring endpoint pagination or scope differs.
 *
 * Loads every page eagerly so overview cards and client-side pagination stay consistent — the scoring
 * API has no type filter, so server-side paging alone cannot scope to V4 proxies.
 */
export async function getV4ScoredApis(environmentId: string): Promise<EnvironmentApiScore[]> {
    const [proxies, scored] = await Promise.all([getAllV4ProxyApis(environmentId), getAllScoredApis(environmentId)]);
    return mergeV4ProxiesWithScores(proxies, scored);
}

// ─── Rulesets ─────────────────────────────────────────────────────────────────

export function getScoringRulesets(environmentId: string): Promise<ScoringRulesetsResponse> {
    return apimFetchJsonV2<ScoringRulesetsResponse>(environmentId, '/scoring/rulesets');
}

export function importScoringRuleset(environmentId: string, request: ImportRulesetRequest): Promise<void> {
    return apimFetchJsonV2<void>(environmentId, '/scoring/rulesets', {
        method: 'POST',
        body: JSON.stringify(request),
    });
}

export function updateScoringRuleset(environmentId: string, rulesetId: string, request: UpdateRulesetRequest): Promise<ScoringRuleset> {
    return apimFetchJsonV2<ScoringRuleset>(environmentId, `/scoring/rulesets/${rulesetId}`, {
        method: 'PUT',
        body: JSON.stringify(request),
    });
}

export function deleteScoringRuleset(environmentId: string, rulesetId: string): Promise<void> {
    return apimFetchJsonV2<void>(environmentId, `/scoring/rulesets/${rulesetId}`, { method: 'DELETE' });
}

// ─── Functions ──────────────────────────────────────────────────────────────

export function getScoringFunctions(environmentId: string): Promise<ScoringFunctionsResponse> {
    return apimFetchJsonV2<ScoringFunctionsResponse>(environmentId, '/scoring/functions');
}

export function importScoringFunction(environmentId: string, request: ImportFunctionRequest): Promise<void> {
    return apimFetchJsonV2<void>(environmentId, '/scoring/functions', {
        method: 'POST',
        body: JSON.stringify(request),
    });
}

/** Functions are keyed by name (no id) — the delete path segment is the function name. */
export function deleteScoringFunction(environmentId: string, functionName: string): Promise<void> {
    return apimFetchJsonV2<void>(environmentId, `/scoring/functions/${encodeURIComponent(functionName)}`, { method: 'DELETE' });
}
