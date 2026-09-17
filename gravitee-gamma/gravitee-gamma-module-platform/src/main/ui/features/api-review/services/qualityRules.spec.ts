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
import { createQualityRule, deleteQualityRule, listQualityRules, updateQualityRule } from './qualityRules';
import { apimFetchJsonV1Env } from '../../../shared/api/apimClient';
import type { QualityRule } from '../types/qualityRule';

jest.mock('../../../shared/api/apimClient', () => ({
    apimFetchJsonV1Env: jest.fn(),
}));

const mockApimFetchJsonV1Env = jest.mocked(apimFetchJsonV1Env);

const EXISTING_RULE: QualityRule = { id: 'rule-1', name: 'OpenAPI is complete', description: 'Every operation is documented.', weight: 7 };

describe('quality rules service', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        mockApimFetchJsonV1Env.mockResolvedValue(undefined);
    });

    it('lists the environment quality rules', async () => {
        await listQualityRules('env-1');
        expect(mockApimFetchJsonV1Env).toHaveBeenCalledWith('env-1', '/configuration/quality-rules');
    });

    it('creates a rule with a zero weight, which Gamma does not manage', async () => {
        await createQualityRule('env-1', { name: 'Owner is a group', description: 'A team owns the API.' });
        expect(mockApimFetchJsonV1Env).toHaveBeenCalledWith('env-1', '/configuration/quality-rules', {
            method: 'POST',
            body: JSON.stringify({ name: 'Owner is a group', description: 'A team owns the API.', weight: 0 }),
        });
    });

    it('updates a rule and preserves the weight Classic may have set', async () => {
        await updateQualityRule('env-1', EXISTING_RULE, { name: 'Renamed', description: 'Changed.' });
        expect(mockApimFetchJsonV1Env).toHaveBeenCalledWith('env-1', '/configuration/quality-rules/rule-1', {
            method: 'PUT',
            body: JSON.stringify({ name: 'Renamed', description: 'Changed.', weight: 7 }),
        });
    });

    it('deletes a rule by its URL-encoded id', async () => {
        await deleteQualityRule('env-1', 'rule with spaces');
        expect(mockApimFetchJsonV1Env).toHaveBeenCalledWith('env-1', '/configuration/quality-rules/rule%20with%20spaces', {
            method: 'DELETE',
        });
    });
});
