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

export type ScoringStatus = 'SUCCESS' | 'PENDING' | 'ERROR';
export type ScoringAssetType = 'ASYNCAPI' | 'GRAVITEE_DEFINITION' | 'SWAGGER';
export type ScoringSeverity = 'ERROR' | 'HINT' | 'INFO' | 'WARN';
export type ScoringFilter = ScoringSeverity | 'ALL';
export type ScoringAsyncJobStatus = 'ERROR' | 'PENDING' | 'SUCCESS' | 'TIMEOUT';

export interface ApiScoringTriggerResponse {
    status: ScoringStatus;
    message?: string;
}

export interface ApiScoringSummary {
    all: number;
    errors: number;
    warnings: number;
    infos: number;
    hints: number;
    score: number;
}

export interface ScoringDiagnosticRange {
    start: { line: number; character: number };
    end: { line: number; character: number };
}

export interface ScoringDiagnostic {
    severity: ScoringSeverity;
    message: string;
    range: ScoringDiagnosticRange;
    path: string;
}

export interface ScoringError {
    code: string;
    path: string[];
}

export interface ScoringAsset {
    name: string;
    type: ScoringAssetType;
    diagnostics: ScoringDiagnostic[];
    errors?: ScoringError[];
}

export interface ApiScoring {
    createdAt: string;
    summary?: ApiScoringSummary;
    assets: ScoringAsset[];
}

export interface ScoringAsyncJob {
    id: string;
    sourceId: string;
    type: 'SCORING_REQUEST';
    status: ScoringAsyncJobStatus;
    errorMessage?: string;
    createdAt: string;
    updatedAt: string;
}

export interface ScoringAsyncJobsResponse {
    data: ScoringAsyncJob[];
}
