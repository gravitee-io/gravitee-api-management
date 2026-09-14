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

/** MAPI v2 ScoringRuleset.format */
export type RulesetFormat =
    | 'GRAVITEE_PROXY'
    | 'GRAVITEE_MESSAGE'
    | 'GRAVITEE_FEDERATION'
    | 'GRAVITEE_NATIVE'
    | 'GRAVITEE_V2'
    | 'OPENAPI'
    | 'ASYNCAPI';

/** UI-only value for the Gravitee parent card; nested cards pick a real {@link RulesetFormat}. */
export const GRAVITEE_API_DEFINITION = 'GraviteeAPI' as const;
export type DefinitionFormatSelection = 'OPENAPI' | 'ASYNCAPI' | typeof GRAVITEE_API_DEFINITION;

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

export interface ScoringRulesetsResponse {
    data: ScoringRuleset[];
}

export interface CreateRulesetRequest {
    name: string;
    description: string;
    payload: string;
    format?: RulesetFormat;
}

export interface EditRulesetRequest {
    name: string;
    description: string;
}

export interface ScoringFunction {
    name: string;
    payload: string;
    createdAt: string;
    referenceId: string;
    referenceType: string;
}

export interface ScoringFunctionsResponse {
    data: ScoringFunction[];
}

export interface CreateFunctionRequest {
    name: string;
    payload: string;
}
