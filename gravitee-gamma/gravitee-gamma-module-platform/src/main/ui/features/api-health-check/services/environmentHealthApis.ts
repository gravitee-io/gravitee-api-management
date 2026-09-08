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
import type { ApiAvailabilityMetric, ApiSearchHit, ApiSearchResponse, EnvironmentHealthApi } from '../types';
import { reportAvailabilityPctFromMetric } from '../utils/availability';
import { healthCheckEnabled } from '../utils/healthCheckEnabled';
import { HEALTH_CHECK_FILTER_QUERY, V4_HTTP_PROXY_API_TYPES } from '../utils/healthCheckQuery';
import { summarizeReportBuckets, type HealthCheckReport } from '../utils/reportBuckets';

const JSON_HEADERS = { 'Content-Type': 'application/json' };
const REPORT_PAGE_SIZE = 100;
const AVAILABILITY_CONCURRENCY = 10;

function isAbortError(error: unknown): boolean {
    return error instanceof DOMException ? error.name === 'AbortError' : error instanceof Error && error.name === 'AbortError';
}

export interface SearchEnvironmentHealthApisParams {
    readonly query?: string;
    readonly page: number;
    readonly perPage: number;
    readonly sortBy?: string;
}

export function toEnvironmentHealthApi(hit: ApiSearchHit): EnvironmentHealthApi {
    return {
        id: hit.id,
        name: hit.name,
        apiVersion: hit.apiVersion ?? '',
        state: hit.state,
        lifecycleState: hit.lifecycleState,
        workflowState: hit.workflowState,
        origin: hit.originContext?.origin,
        pictureUrl: hit._links?.pictureUrl,
        healthcheckEnabled: healthCheckEnabled(hit),
    };
}

export async function searchEnvironmentHealthApis(
    environmentId: string,
    params: SearchEnvironmentHealthApisParams,
    signal?: AbortSignal,
): Promise<ApiSearchResponse> {
    const searchParams = new URLSearchParams({ page: String(params.page), perPage: String(params.perPage) });
    if (params.sortBy) {
        searchParams.set('sortBy', params.sortBy);
    }
    return apimFetchJsonV2<ApiSearchResponse>(environmentId, `/apis/_search?${searchParams}`, {
        method: 'POST',
        headers: JSON_HEADERS,
        body: JSON.stringify({
            query: params.query || undefined,
            apiTypes: [...V4_HTTP_PROXY_API_TYPES],
        }),
        signal,
    });
}

export async function getApiAvailability(
    environmentId: string,
    apiId: string,
    from: number,
    to: number,
    signal?: AbortSignal,
): Promise<ApiAvailabilityMetric> {
    const path = `/apis/${encodeURIComponent(apiId)}/health/availability?from=${from}&to=${to}&field=endpoint`;
    return apimFetchJsonV2<ApiAvailabilityMetric>(environmentId, path, { signal });
}

export async function fetchEnvironmentHealthReport(
    environmentId: string,
    from: number,
    to: number,
    signal?: AbortSignal,
): Promise<HealthCheckReport> {
    const samples: Array<number | null> = [];
    let attempted = 0;
    let failed = 0;
    let page = 1;
    let pageCount = 1;

    while (page <= pageCount) {
        if (signal?.aborted) {
            throw new DOMException('Aborted', 'AbortError');
        }
        const result = await searchEnvironmentHealthApis(
            environmentId,
            { query: HEALTH_CHECK_FILTER_QUERY, page, perPage: REPORT_PAGE_SIZE },
            signal,
        );
        pageCount = result.pagination.pageCount || page;

        for (let index = 0; index < result.data.length; index += AVAILABILITY_CONCURRENCY) {
            if (signal?.aborted) {
                throw new DOMException('Aborted', 'AbortError');
            }
            const chunk = result.data.slice(index, index + AVAILABILITY_CONCURRENCY);
            const chunkResults = await Promise.all(
                chunk.map(async hit => {
                    try {
                        const metric = await getApiAvailability(environmentId, hit.id, from, to, signal);
                        return { sample: reportAvailabilityPctFromMetric(metric), failed: false };
                    } catch (error) {
                        if (signal?.aborted || isAbortError(error)) {
                            throw error;
                        }
                        return { sample: null, failed: true };
                    }
                }),
            );
            for (const row of chunkResults) {
                attempted += 1;
                if (row.failed) {
                    failed += 1;
                }
                samples.push(row.sample);
            }
        }

        if (result.data.length === 0) {
            break;
        }
        page += 1;
    }

    if (attempted > 0 && failed === attempted) {
        throw new Error('Failed to load availability for all health-check APIs');
    }

    return summarizeReportBuckets(samples);
}
