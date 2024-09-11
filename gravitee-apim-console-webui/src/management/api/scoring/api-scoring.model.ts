/*
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

export interface ApiScoringTriggerResponse {
  status: ScoringStatus;
  message?: string;
}

export enum ScoringStatus {
  SUCCESS = 'SUCCESS',
  PENDING = 'PENDING',
  ERROR = 'ERROR',
}
export enum ScoringAssetType {
  ASYNCAPI = 'ASYNCAPI',
  GRAVITEE_DEFINITION = 'GRAVITEE_DEFINITION',
  SWAGGER = 'SWAGGER',
}
export enum ScoringSeverity {
  ERROR = 'ERROR',
  HINT = 'HINT',
  INFO = 'INFO',
  WARN = 'WARN',
}

export interface ApiScoring {
  createdAt: Date;
  summary: ApiScoringSummary;
  assets: ScoringAsset[];
}

export interface ApiScoringSummary {
  all: number;
  errors: number;
  warnings: number;
  infos: number;
  hints: number;
}

export interface ScoringAsset {
  name: string;
  type: ScoringAssetType;
  diagnostics: ScoringDiagnostic[];
}

export interface ScoringDiagnostic {
  severity: ScoringSeverity;
  message: string;
  range: {
    start: { line: number; character: number };
    end: { line: number; character: number };
  };
  path: string;
}
