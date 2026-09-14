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
import { createScoringFunction, deleteScoringFunction, listScoringFunctions } from './scoringFunctions';
import { apimFetchJsonV2 } from '../../../shared/api/apimClient';

jest.mock('../../../shared/api/apimClient', () => ({
    apimFetchJsonV2: jest.fn(),
}));

const mockApimFetchJsonV2 = jest.mocked(apimFetchJsonV2);

describe('scoringFunctions service', () => {
    beforeEach(() => {
        jest.clearAllMocks();
    });

    it('GETs v2 scoring/functions and normalizes missing data', async () => {
        mockApimFetchJsonV2.mockResolvedValueOnce({});
        const result = await listScoringFunctions('env-1');
        expect(mockApimFetchJsonV2).toHaveBeenCalledWith('env-1', '/scoring/functions');
        expect(result).toEqual([]);
    });

    it('POSTs name and payload to scoring/functions', async () => {
        mockApimFetchJsonV2.mockResolvedValueOnce(undefined);
        const body = { name: 'checkTag.js', payload: 'module.exports = {}' };
        await createScoringFunction('env-1', body);
        expect(mockApimFetchJsonV2).toHaveBeenCalledWith('env-1', '/scoring/functions', {
            method: 'POST',
            body: JSON.stringify(body),
        });
    });

    it('DELETEs a function by filename', async () => {
        mockApimFetchJsonV2.mockResolvedValueOnce(undefined);
        await deleteScoringFunction('env-1', 'checkTag.js');
        expect(mockApimFetchJsonV2).toHaveBeenCalledWith('env-1', '/scoring/functions/checkTag.js', { method: 'DELETE' });
    });
});
