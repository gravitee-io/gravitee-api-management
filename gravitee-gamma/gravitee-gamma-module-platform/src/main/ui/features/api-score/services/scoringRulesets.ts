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
import type { CreateRulesetRequest, EditRulesetRequest, ScoringRuleset, ScoringRulesetsResponse } from '../types/rulesets';

export async function listScoringRulesets(environmentId: string): Promise<ScoringRuleset[]> {
    const response = await apimFetchJsonV2<Partial<ScoringRulesetsResponse>>(environmentId, '/scoring/rulesets');
    return response.data ?? [];
}

export async function getScoringRuleset(environmentId: string, rulesetId: string): Promise<ScoringRuleset> {
    return apimFetchJsonV2<ScoringRuleset>(environmentId, `/scoring/rulesets/${encodeURIComponent(rulesetId)}`);
}

export async function createScoringRuleset(environmentId: string, body: CreateRulesetRequest): Promise<void> {
    await apimFetchJsonV2<void>(environmentId, '/scoring/rulesets', {
        method: 'POST',
        body: JSON.stringify(body),
    });
}

export async function updateScoringRuleset(environmentId: string, rulesetId: string, body: EditRulesetRequest): Promise<ScoringRuleset> {
    return apimFetchJsonV2<ScoringRuleset>(environmentId, `/scoring/rulesets/${encodeURIComponent(rulesetId)}`, {
        method: 'PUT',
        body: JSON.stringify(body),
    });
}

export async function deleteScoringRuleset(environmentId: string, rulesetId: string): Promise<void> {
    await apimFetchJsonV2<void>(environmentId, `/scoring/rulesets/${encodeURIComponent(rulesetId)}`, { method: 'DELETE' });
}
