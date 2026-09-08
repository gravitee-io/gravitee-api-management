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
    readonly _links?: { readonly pictureUrl?: string };
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

export interface ApiAvailabilityMetric {
    readonly global?: number | null;
    readonly group?: unknown;
}

export interface EnvironmentHealthApi {
    readonly id: string;
    readonly name: string;
    readonly apiVersion: string;
    readonly state?: ApiState;
    readonly lifecycleState?: ApiLifecycleState;
    readonly workflowState?: ApiWorkflowState;
    readonly origin?: string;
    readonly pictureUrl?: string;
    readonly healthcheckEnabled: boolean;
}

export type { HealthCheckReport };
