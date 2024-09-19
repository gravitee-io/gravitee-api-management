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

import { ApiScoring, ApiScoringTriggerResponse, ScoringAssetType, ScoringSeverity, ScoringStatus } from './api-scoring.model';

import { ApisScoring } from '../../api-score/api-score.model';

export const fakeApiScoringTriggerResponse = (attributes?: Partial<ApiScoringTriggerResponse>) => {
  const base: ApiScoringTriggerResponse = {
    status: ScoringStatus.PENDING,
  };

  return {
    ...base,
    ...attributes,
  };
};

export const fakeApiScoring = (attributes?: Partial<ApiScoring>) => {
  const base: ApiScoring = {
    createdAt: new Date(),
    summary: {
      all: 1,
      errors: 0,
      warnings: 1,
      infos: 0,
      hints: 0,
    },
    assets: [
      {
        name: 'echo-oas.json',
        type: ScoringAssetType.SWAGGER,
        diagnostics: [
          {
            range: {
              start: { line: 17, character: 12 },
              end: { line: 38, character: 25 },
            },
            severity: ScoringSeverity.WARN,
            message: 'Operation "description" must be present and non-empty string.',
            path: 'paths./echo.get',
          },
        ],
      },
    ],
  };

  return {
    ...base,
    ...attributes,
  };
};

export const fakeApisScoring = (attributes?: Partial<ApisScoring>): ApisScoring => {
  const base: ApisScoring = {
    id: 'test_id',
    name: 'test_name',
    pictureUrl: 'test_pictureUrl',
    score: 50,
    errors: 1,
    warnings: 1,
    infos: 1,
    hints: 1,
  };

  return {
    ...base,
    ...attributes,
  };
};
