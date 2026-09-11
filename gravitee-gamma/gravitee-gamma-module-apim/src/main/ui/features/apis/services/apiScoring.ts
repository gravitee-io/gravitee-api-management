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
import type { ApiScoring, ApiScoringTriggerResponse, ScoringAsyncJobsResponse } from '../types/scoring';

const apiPath = (apiId: string) => `/apis/${encodeURIComponent(apiId)}`;

/** Duck-type 404 so Module Federation duplicate `ApimApiError` classes still match. */
export function isNotFoundError(error: unknown): boolean {
    if (typeof error !== 'object' || error === null || !('status' in error)) return false;
    return Number((error as { status: unknown }).status) === 404;
}

export async function getApiScoring(environmentId: string, apiId: string): Promise<ApiScoring | null> {
    try {
        return (await apimFetchJsonV2<ApiScoring>(environmentId, `${apiPath(apiId)}/scoring`)) ?? null;
    } catch (error) {
        if (isNotFoundError(error)) {
            return null;
        }
        throw error;
    }
}

export async function evaluateApiScoring(environmentId: string, apiId: string): Promise<ApiScoringTriggerResponse> {
    return apimFetchJsonV2<ApiScoringTriggerResponse>(environmentId, `${apiPath(apiId)}/scoring/_evaluate`, {
        method: 'POST',
    });
}

export async function listScoringJobs(environmentId: string, apiId: string): Promise<ScoringAsyncJobsResponse> {
    const searchParams = new URLSearchParams({
        page: '1',
        perPage: '10',
        type: 'SCORING_REQUEST',
        sourceId: apiId,
    });
    const response = await apimFetchJsonV2<ScoringAsyncJobsResponse>(environmentId, `/async-jobs?${searchParams.toString()}`);
    return { data: response.data ?? [] };
}
