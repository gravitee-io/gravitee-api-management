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
import {
    createScoringRuleset,
    deleteScoringRuleset,
    getScoringRuleset,
    listScoringRulesets,
    updateScoringRuleset,
} from './scoringRulesets';
import { apimFetchJsonV2 } from '../../../shared/api/apimClient';

jest.mock('../../../shared/api/apimClient', () => ({
    apimFetchJsonV2: jest.fn(),
}));

const mockApimFetchJsonV2 = jest.mocked(apimFetchJsonV2);

const RULESET = {
    id: 'rs-1',
    name: 'Style',
    description: 'lint',
    format: 'OPENAPI' as const,
    payload: 'rules: []',
    createdAt: '2026-01-01T00:00:00Z',
    referenceId: 'DEFAULT',
    referenceType: 'ENVIRONMENT',
};

describe('scoringRulesets service', () => {
    beforeEach(() => {
        jest.clearAllMocks();
    });

    it('GETs v2 scoring/rulesets and normalizes missing data', async () => {
        mockApimFetchJsonV2.mockResolvedValueOnce({});
        const result = await listScoringRulesets('env-1');
        expect(mockApimFetchJsonV2).toHaveBeenCalledWith('env-1', '/scoring/rulesets');
        expect(result).toEqual([]);
    });

    it('GETs a single ruleset by id', async () => {
        mockApimFetchJsonV2.mockResolvedValueOnce(RULESET);
        await getScoringRuleset('env-1', 'rs-1');
        expect(mockApimFetchJsonV2).toHaveBeenCalledWith('env-1', '/scoring/rulesets/rs-1');
    });

    it('POSTs import body to scoring/rulesets', async () => {
        mockApimFetchJsonV2.mockResolvedValueOnce(undefined);
        const body = { name: 'Style', description: '', payload: 'x', format: 'OPENAPI' as const };
        await createScoringRuleset('env-1', body);
        expect(mockApimFetchJsonV2).toHaveBeenCalledWith('env-1', '/scoring/rulesets', {
            method: 'POST',
            body: JSON.stringify(body),
        });
    });

    it('PUTs name and description only', async () => {
        mockApimFetchJsonV2.mockResolvedValueOnce(RULESET);
        await updateScoringRuleset('env-1', 'rs-1', { name: 'New', description: 'd' });
        expect(mockApimFetchJsonV2).toHaveBeenCalledWith('env-1', '/scoring/rulesets/rs-1', {
            method: 'PUT',
            body: JSON.stringify({ name: 'New', description: 'd' }),
        });
    });

    it('DELETEs a ruleset by id', async () => {
        mockApimFetchJsonV2.mockResolvedValueOnce(undefined);
        await deleteScoringRuleset('env-1', 'rs/1');
        expect(mockApimFetchJsonV2).toHaveBeenCalledWith('env-1', '/scoring/rulesets/rs%2F1', { method: 'DELETE' });
    });
});
