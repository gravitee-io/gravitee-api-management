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
import { ScoringFunction, ScoringRuleset, ScoringRulesetsResponse } from './ruleset';

export function fakeRulesetsList(attribute?: Partial<ScoringRulesetsResponse>) {
  const base: ScoringRulesetsResponse = {
    data: [
      {
        name: 'Test ruleset name',
        description: 'Test ruleset description',
        payload: 'Ruleset payload',
        id: 'ruleset-id',
        createdAt: '2024-11-19T12:41:18.85Z',
        referenceId: 'DEFAULT',
        referenceType: 'ENVIRONMENT',
      },
    ],
  };

  return {
    ...base,
    ...attribute,
  };
}

export function fakeRuleset(attribute?: Partial<ScoringRuleset>): ScoringRuleset {
  const base: ScoringRuleset = {
    name: 'Test ruleset name',
    description: 'Test ruleset description',
    payload: 'Ruleset payload',
    id: 'ruleset-id',
    createdAt: '2024-11-19T12:41:18.85Z',
    referenceId: 'DEFAULT',
    referenceType: 'ENVIRONMENT',
  };

  return {
    ...base,
    ...attribute,
  };
}

export function fakeScoringFunction(attribute?: Partial<ScoringFunction>): ScoringFunction {
  const base: ScoringFunction = {
    name: 'Test ruleset name',
    payload: 'Ruleset payload',
    createdAt: '2024-11-19T12:41:18.85Z',
    referenceId: 'DEFAULT',
    referenceType: 'ENVIRONMENT',
  };

  return {
    ...base,
    ...attribute,
  };
}
