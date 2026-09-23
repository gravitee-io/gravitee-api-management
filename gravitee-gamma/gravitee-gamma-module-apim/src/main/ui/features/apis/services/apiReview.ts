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
import { apimFetchJsonV1Env, apimFetchJsonV2 } from '../../../shared/api/apimClient';
import type { ApiQualityRuleCheck, QualityRule } from '../types';

const JSON_HEADERS = { 'Content-Type': 'application/json' };

export type ApiReviewDecision = 'accept' | 'reject';

function reviewPath(apiId: string, action: 'ask' | ApiReviewDecision): string {
    return `/apis/${encodeURIComponent(apiId)}/reviews/_${action}`;
}

async function postReview(environmentId: string, apiId: string, action: 'ask' | ApiReviewDecision, message?: string): Promise<void> {
    await apimFetchJsonV2(environmentId, reviewPath(apiId, action), {
        method: 'POST',
        headers: JSON_HEADERS,
        body: JSON.stringify(message ? { message } : {}),
    });
}

export async function askApiReview(environmentId: string, apiId: string, message?: string): Promise<void> {
    await postReview(environmentId, apiId, 'ask', message);
}

/** Manual rules defined for the environment (Platform → Environment → API Review). */
export async function listQualityRules(environmentId: string): Promise<QualityRule[]> {
    return apimFetchJsonV1Env<QualityRule[]>(environmentId, '/configuration/quality-rules');
}

/** Which manual rules a reviewer has already ticked on this API. */
export async function listApiQualityRuleChecks(environmentId: string, apiId: string): Promise<ApiQualityRuleCheck[]> {
    return apimFetchJsonV1Env<ApiQualityRuleCheck[]>(environmentId, `/apis/${encodeURIComponent(apiId)}/quality-rules`);
}

export interface ApiQualityRuleCheckWrite {
    readonly qualityRuleId: string;
    readonly checked: boolean;
    /** True when the API already has a record for this rule (PUT); false creates one (POST). */
    readonly exists: boolean;
}

async function saveApiQualityRuleCheck(environmentId: string, apiId: string, check: ApiQualityRuleCheckWrite): Promise<void> {
    const apiPath = `/apis/${encodeURIComponent(apiId)}/quality-rules`;
    if (check.exists) {
        await apimFetchJsonV1Env(environmentId, `${apiPath}/${encodeURIComponent(check.qualityRuleId)}`, {
            method: 'PUT',
            body: JSON.stringify({ checked: check.checked }),
        });
        return;
    }
    await apimFetchJsonV1Env(environmentId, apiPath, {
        method: 'POST',
        body: JSON.stringify({ api: apiId, quality_rule: check.qualityRuleId, checked: check.checked }),
    });
}

export interface SubmitApiReviewInput {
    readonly decision: ApiReviewDecision;
    readonly message?: string;
    readonly checks: readonly ApiQualityRuleCheckWrite[];
}

/**
 * Records the rule checks first so the decision is made on what the reviewer actually ticked.
 * The checks are written one at a time, in the order the reviewer sees them: they all target the same
 * API resource, so a failure stops the run at a predictable point instead of part-way through a batch.
 */
export async function submitApiReview(environmentId: string, apiId: string, input: SubmitApiReviewInput): Promise<void> {
    for (const check of input.checks) {
        await saveApiQualityRuleCheck(environmentId, apiId, check);
    }
    await postReview(environmentId, apiId, input.decision, input.message);
}
