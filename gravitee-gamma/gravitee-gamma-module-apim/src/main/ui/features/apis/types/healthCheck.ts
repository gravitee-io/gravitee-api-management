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

/** Dimension the availability / response-time aggregations are grouped by. */
export type HealthField = 'endpoint' | 'gateway';

// ─── Backend DTOs (Management API v2 — /apis/{id}/health/*) ──────────────────

/** GET /health/average-response-time-overtime */
export interface ApiHealthResponseTimeOvertime {
    timeRange: {
        from: number;
        to: number;
        interval: number;
    };
    /** One average response-time (ms) per time bucket. */
    data: number[];
}

/**
 * GET /health/availability and /health/average-response-time.
 *
 * `global` is a fraction in [0..1] for availability, and a raw value (ms) for
 * response time. `group` is keyed by endpoint/gateway name. The backend has
 * historically typed `group` as an array of single-key objects, so we accept
 * both shapes and normalize in the adapter layer.
 */
export interface ApiHealthFieldMetric {
    global: number;
    group: Record<string, number> | Array<Record<string, number>>;
}

export type ApiAvailability = ApiHealthFieldMetric;
export type ApiAverageResponseTime = ApiHealthFieldMetric;

export interface HealthCheckLogsRequestParams {
    from: number;
    to: number;
    page: number;
    perPage: number;
    success: boolean;
}

export interface HealthCheckPagination {
    page?: number;
    perPage?: number;
    pageCount?: number;
    totalCount?: number;
    pageItemsCount?: number;
}

export interface HealthCheckLogsResponse {
    data?: HealthCheckLog[];
    pagination?: HealthCheckPagination;
}

export interface HealthCheckLog {
    id: string;
    timestamp: string;
    endpointName: string;
    gatewayId: string;
    responseTime: number;
    success: boolean;
    steps: HealthCheckStep[];
}

export interface HealthCheckStep {
    name: string;
    success: boolean;
    message?: string;
    request?: HealthCheckStepRequest;
    response?: HealthCheckStepResponse;
}

export interface HealthCheckStepRequest {
    uri: string;
    method: string;
    headers: Record<string, string>;
}

export interface HealthCheckStepResponse {
    status: number;
    body?: string;
    headers: Record<string, string>;
}

// ─── View models (consumed by the dashboard components) ──────────────────────

/** Global headline metrics rendered as graphene `Metric` cards. */
export interface HealthGlobalStats {
    /** Availability as a percentage in [0..100]. */
    availabilityPct: number;
    /** Average response time in ms. */
    avgResponseTimeMs: number;
}

/** One row of the per-endpoint / per-gateway availability table. */
export interface AvailabilityRow {
    key: string;
    name: string;
    /** Availability as a percentage in [0..100]. */
    availabilityPct: number;
    /** Average response time in ms (undefined when not reported for the key). */
    avgResponseTimeMs?: number;
}

/** One point of the response-time trend (graphene `LineChart` data point). */
export interface ResponseTimeTrendPoint {
    /** Bucket timestamp (epoch ms) used as the chart category. */
    timestamp: number;
    /** Average response time (ms) for the bucket. */
    responseTime: number;
}
