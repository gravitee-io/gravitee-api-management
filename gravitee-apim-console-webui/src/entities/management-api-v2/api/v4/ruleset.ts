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
export interface ScoringRulesetsResponse {
  data: ScoringRuleset[];
}

export interface ScoringRuleset {
  id: string;
  name: string;
  description: string;
  format?: RulesetFormat;
  payload: string;
  createdAt: string;
  referenceId: string;
  referenceType: string;
}

export interface CreateRulesetRequestData {
  name: string;
  description: string;
  payload: string;
  format?: RulesetFormat;
}

export interface EditRulesetRequestData {
  name: string;
  description: string;
}

export enum RulesetFormat {
  GRAVITEE_PROXY = 'GRAVITEE_PROXY',
  GRAVITEE_MESSAGE = 'GRAVITEE_MESSAGE',
  GRAVITEE_FEDERATION = 'GRAVITEE_FEDERATION',
  KAFKA_NATIVE = 'KAFKA_NATIVE',
  GRAVITEE_V2 = 'GRAVITEE_V2',
  OPENAPI = 'OPENAPI',
  ASYNCAPI = 'ASYNCAPI',
}

export interface ScoringFunctionsResponse {
  data: ScoringFunction[];
}

export interface ScoringFunction {
  name: string;
  payload: string;
  createdAt: string;
  referenceId: string;
  referenceType: string;
}

export interface CreateFunctionRequestData {
  name: string;
  payload: string;
}
