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
import { askApiReview, listApiQualityRuleChecks, listQualityRules, submitApiReview } from './apiReview';
import { resetApimClientForTests } from '../../../shared/api/apimClient';
import { TEST_CONFIG, TEST_V2_BASE } from '../../../testing/factories';
import { trackHandler } from '../../../testing/helpers';

const V1_ENV_BASE = `${TEST_CONFIG.managementBaseURL}/organizations/${TEST_CONFIG.organizationId}/environments/${TEST_CONFIG.environmentId}`;

describe('apiReview service', () => {
    beforeEach(() => {
        resetApimClientForTests();
    });

    it('asks for a review through the v2 reviews endpoint', async () => {
        const tracker = trackHandler('post', `${TEST_V2_BASE}/apis/:apiId/reviews/_ask`, undefined, 204);

        await askApiReview('DEFAULT', 'api-1');

        expect(tracker.callCount).toBe(1);
        expect(new URL(tracker.lastCall!.url).pathname).toContain('/apis/api-1/reviews/_ask');
        expect(tracker.lastCall?.body).toEqual({});
    });

    it('lists the environment manual rules and the checks already recorded on the API', async () => {
        const rules = trackHandler('get', `${V1_ENV_BASE}/configuration/quality-rules`, [{ id: 'r1', name: 'Rule', description: 'd' }]);
        const checks = trackHandler('get', `${V1_ENV_BASE}/apis/:apiId/quality-rules`, [
            { api: 'api-1', quality_rule: 'r1', checked: true },
        ]);

        await expect(listQualityRules('DEFAULT')).resolves.toEqual([{ id: 'r1', name: 'Rule', description: 'd' }]);
        await expect(listApiQualityRuleChecks('DEFAULT', 'api-1')).resolves.toEqual([{ api: 'api-1', quality_rule: 'r1', checked: true }]);
        expect(rules.callCount).toBe(1);
        expect(checks.callCount).toBe(1);
    });

    it('records the rule checks, creating or updating each, before accepting with the comment', async () => {
        const create = trackHandler('post', `${V1_ENV_BASE}/apis/:apiId/quality-rules`, {});
        const update = trackHandler('put', `${V1_ENV_BASE}/apis/:apiId/quality-rules/:ruleId`, {});
        const accept = trackHandler('post', `${TEST_V2_BASE}/apis/:apiId/reviews/_accept`, undefined, 204);

        await submitApiReview('DEFAULT', 'api-1', {
            decision: 'accept',
            message: 'Looks good',
            checks: [
                { qualityRuleId: 'r-new', checked: true, exists: false },
                { qualityRuleId: 'r-old', checked: false, exists: true },
            ],
        });

        expect(create.lastCall?.body).toEqual({ api: 'api-1', quality_rule: 'r-new', checked: true });
        expect(new URL(update.lastCall!.url).pathname).toContain('/apis/api-1/quality-rules/r-old');
        expect(update.lastCall?.body).toEqual({ checked: false });
        expect(accept.callCount).toBe(1);
        expect(accept.lastCall?.body).toEqual({ message: 'Looks good' });
    });

    it('rejects without a body message when the reviewer left no comment', async () => {
        const reject = trackHandler('post', `${TEST_V2_BASE}/apis/:apiId/reviews/_reject`, undefined, 204);

        await submitApiReview('DEFAULT', 'api-1', { decision: 'reject', message: '', checks: [] });

        expect(reject.callCount).toBe(1);
        expect(reject.lastCall?.body).toEqual({});
    });

    it('does not accept when a rule check could not be saved', async () => {
        trackHandler('post', `${V1_ENV_BASE}/apis/:apiId/quality-rules`, { message: 'nope' }, 500);
        const accept = trackHandler('post', `${TEST_V2_BASE}/apis/:apiId/reviews/_accept`, undefined, 204);

        await expect(
            submitApiReview('DEFAULT', 'api-1', { decision: 'accept', checks: [{ qualityRuleId: 'r1', checked: true, exists: false }] }),
        ).rejects.toThrow();
        expect(accept.callCount).toBe(0);
    });
});
