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

/** Format a custom ruleset applies to (Management API v2 `ScoringAssetFormat`). */
export type ScoringAssetFormat =
    | 'OPENAPI'
    | 'ASYNCAPI'
    | 'GRAVITEE_PROXY'
    | 'GRAVITEE_MESSAGE'
    | 'GRAVITEE_NATIVE'
    | 'GRAVITEE_FEDERATION'
    | 'GRAVITEE_V2';

/** Severity buckets counted on both the environment overview and per-API scores. */
export type ScoringSeverityKey = 'errors' | 'warnings' | 'infos' | 'hints';

/** Aggregate shown in the overview cards. `score` is null when no API has been scored yet. */
export interface ScoreSummary {
    score: number | null;
    errors: number;
    warnings: number;
    infos: number;
    hints: number;
}

/** A single scored API row from `GET /scoring/apis`. `score` is a 0..1 fraction, absent when never evaluated. */
export interface EnvironmentApiScore {
    id: string;
    name: string;
    pictureUrl: string;
    score?: number;
    errors?: number;
    warnings?: number;
    infos?: number;
    hints?: number;
}

export interface ScoringPagination {
    page: number;
    perPage: number;
    pageCount: number;
    totalCount: number;
    pageItemsCount?: number;
}

export interface EnvironmentApisScoringResponse {
    data: EnvironmentApiScore[];
    pagination?: ScoringPagination;
}

/** A custom ruleset (`GET /scoring/rulesets`). Payload/format are immutable after import. */
export interface ScoringRuleset {
    id: string;
    name: string;
    description?: string;
    format?: ScoringAssetFormat;
    payload: string;
    createdAt?: string;
    updatedAt?: string;
    referenceId?: string;
    referenceType?: string;
}

export interface ScoringRulesetsResponse {
    data: ScoringRuleset[];
}

/** A custom function (`GET /scoring/functions`). Keyed by `name`. */
export interface ScoringFunction {
    name: string;
    payload: string;
    createdAt?: string;
    referenceId?: string;
    referenceType?: string;
}

export interface ScoringFunctionsResponse {
    data: ScoringFunction[];
}

export interface ImportRulesetRequest {
    name: string;
    description: string;
    payload: string;
    format: ScoringAssetFormat;
}

export interface UpdateRulesetRequest {
    name: string;
    description: string;
}

export interface ImportFunctionRequest {
    name: string;
    payload: string;
}
