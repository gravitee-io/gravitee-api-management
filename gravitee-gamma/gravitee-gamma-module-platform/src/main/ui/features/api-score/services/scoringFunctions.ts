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
import { apimFetchJsonV2 } from '../../../shared/api/apimClient';
import type { CreateFunctionRequest, ScoringFunction, ScoringFunctionsResponse } from '../types/rulesets';

export async function listScoringFunctions(environmentId: string): Promise<ScoringFunction[]> {
    const response = await apimFetchJsonV2<Partial<ScoringFunctionsResponse>>(environmentId, '/scoring/functions');
    return response.data ?? [];
}

export async function createScoringFunction(environmentId: string, body: CreateFunctionRequest): Promise<void> {
    await apimFetchJsonV2<void>(environmentId, '/scoring/functions', {
        method: 'POST',
        body: JSON.stringify(body),
    });
}

export async function deleteScoringFunction(environmentId: string, functionName: string): Promise<void> {
    await apimFetchJsonV2<void>(environmentId, `/scoring/functions/${encodeURIComponent(functionName)}`, { method: 'DELETE' });
}
