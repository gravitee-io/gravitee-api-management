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
import type { HealthCheckEndpointGroup } from './utils/healthCheckEnabled';
import type { Timeframe } from './utils/healthTimeframe';
import type { HealthCheckReport } from './utils/reportBuckets';

export type ApiState = 'CLOSED' | 'INITIALIZED' | 'STARTED' | 'STOPPED' | 'STOPPING';
export type ApiLifecycleState = 'ARCHIVED' | 'CREATED' | 'DEPRECATED' | 'PUBLISHED' | 'UNPUBLISHED';
export type ApiWorkflowState = 'DRAFT' | 'IN_REVIEW' | 'REQUEST_FOR_CHANGES' | 'REVIEW_OK';

export interface ApiSearchHit {
    readonly id: string;
    readonly name: string;
    readonly apiVersion?: string;
    readonly state?: ApiState;
    readonly lifecycleState?: ApiLifecycleState;
    readonly workflowState?: ApiWorkflowState;
    readonly originContext?: { readonly origin?: string };
    readonly endpointGroups?: readonly HealthCheckEndpointGroup[];
}

export interface ApiSearchPagination {
    readonly page: number;
    readonly perPage: number;
    readonly pageCount: number;
    readonly totalCount: number;
}

export interface ApiSearchResponse {
    readonly data: readonly ApiSearchHit[];
    readonly pagination: ApiSearchPagination;
}

/**
 * v1 `GET /apis/{id}/health?type=availability`, the call Classic makes.
 *
 * `global` carries every timeframe at once as a percentage, so switching timeframe costs no request.
 * It is null when the API has never reported, which is how a silent API is told apart from one that is
 * reporting 0% -- a distinction the v2 availability endpoint collapses.
 */
export interface ApiAvailabilityMetric {
    readonly global?: Partial<Record<Timeframe, number>> | null;
    readonly buckets?: Record<string, Partial<Record<Timeframe, number>>> | null;
}

/**
 * v1 `GET /apis/{id}/health/average?type=AVAILABILITY&from&to&interval`, Classic's second per-row call.
 * Classic shows the gauge only when `values[0].buckets[0].data` exists, so an API with a lifetime
 * percentage but nothing inside the selected window still reads as "No data to display".
 */
export interface ApiHealthAverage {
    readonly timestamp?: { readonly from?: number; readonly to?: number; readonly interval?: number };
    readonly values?: ReadonlyArray<{
        readonly buckets?: ReadonlyArray<{ readonly name?: string; readonly data?: readonly number[] }>;
        readonly field?: string;
    }>;
}

export interface EnvironmentHealthApi {
    readonly id: string;
    readonly name: string;
    readonly apiVersion: string;
    readonly state?: ApiState;
    readonly lifecycleState?: ApiLifecycleState;
    readonly workflowState?: ApiWorkflowState;
    readonly origin?: string;
    readonly healthcheckEnabled: boolean;
}

export type { HealthCheckReport };
