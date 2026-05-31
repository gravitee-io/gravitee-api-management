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
export type PolicyType = 'MCP' | 'AGENT' | 'MODEL' | 'API' | 'EVENT' | 'CUSTOM';
export type PolicyStatus = 'DRAFT' | 'DEPLOYED' | 'DISABLED';

export interface PolicyTarget {
    readonly id: string;
    readonly label: string;
}

export interface PolicyResponse {
    readonly id: string;
    readonly environmentId: string;
    readonly name: string;
    readonly description: string | null;
    readonly policyText: string;
    readonly type: PolicyType;
    readonly target: PolicyTarget | null;
    readonly status: PolicyStatus;
    readonly createdAt: string;
    readonly updatedAt: string;
}

export interface PolicyRequest {
    readonly name: string;
    readonly description?: string | null;
    readonly policyText: string;
    readonly type: PolicyType;
    readonly target?: PolicyTarget | null;
    readonly status?: PolicyStatus | null;
}

export interface EntityResponse {
    readonly id: string;
    readonly environmentId: string;
    readonly uid: string;
    readonly attributes: Record<string, unknown>;
    readonly parents: string[];
    readonly createdAt: string;
    readonly updatedAt: string;
}

export interface EntityRequest {
    readonly uid: string;
    readonly attributes: Record<string, unknown>;
    readonly parents: string[];
}

export interface SchemaResponse {
    readonly environmentId: string;
    readonly schemaText: string;
    readonly updatedAt: string | null;
}

export interface PagedResponse<T> {
    readonly data: readonly T[];
    readonly total: number;
    readonly page: number;
    readonly perPage: number;
}

export interface ValidationErrorResponse {
    readonly message: string;
    readonly status: number;
    readonly errors: readonly string[];
}

// Mirrors io.gravitee.apim.core.async_job.model.AsyncJob.Status. PENDING is in-flight.
export type AmSyncStatus = 'PENDING' | 'SUCCESS' | 'ERROR' | 'TIMEOUT';

export interface AmSyncStartResponse {
    readonly jobId: string;
    readonly status: AmSyncStatus;
    readonly totalUsers: number;
}

export interface AmSyncStatusResponse {
    readonly jobId: string;
    readonly status: AmSyncStatus;
    readonly entitiesUpserted: number | null;
    readonly error: string | null;
    readonly completedAt: string | null;
}
