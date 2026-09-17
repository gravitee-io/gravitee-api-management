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
import { apimFetchJsonV1Env } from '../../../shared/api/apimClient';
import type { QualityRule, QualityRuleWrite } from '../types/qualityRule';

const QUALITY_RULES_PATH = '/configuration/quality-rules';

/** Gamma has no rule weight, so the weight is stored as 0 on create and kept as-is on update. */
const NEW_RULE_WEIGHT = 0;

export async function listQualityRules(environmentId: string): Promise<QualityRule[]> {
    return apimFetchJsonV1Env<QualityRule[]>(environmentId, QUALITY_RULES_PATH);
}

export async function createQualityRule(environmentId: string, write: QualityRuleWrite): Promise<QualityRule> {
    return apimFetchJsonV1Env<QualityRule>(environmentId, QUALITY_RULES_PATH, {
        method: 'POST',
        body: JSON.stringify({ ...write, weight: NEW_RULE_WEIGHT }),
    });
}

export async function updateQualityRule(environmentId: string, rule: QualityRule, write: QualityRuleWrite): Promise<QualityRule> {
    return apimFetchJsonV1Env<QualityRule>(environmentId, `${QUALITY_RULES_PATH}/${encodeURIComponent(rule.id)}`, {
        method: 'PUT',
        body: JSON.stringify({ ...write, weight: rule.weight }),
    });
}

export async function deleteQualityRule(environmentId: string, ruleId: string): Promise<void> {
    await apimFetchJsonV1Env<void>(environmentId, `${QUALITY_RULES_PATH}/${encodeURIComponent(ruleId)}`, {
        method: 'DELETE',
    });
}
