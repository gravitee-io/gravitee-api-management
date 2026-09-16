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
import { evaluateApiScoring, getApiScoring, listScoringJobs } from './apiScoring';
import { ApimApiError, apimFetchJsonV2 } from '../../../shared/api/apimClient';

jest.mock('../../../shared/api/apimClient', () => {
    const actual = jest.requireActual('../../../shared/api/apimClient');
    return {
        ...actual,
        apimFetchJsonV2: jest.fn(),
    };
});

const mockApimFetchJsonV2 = jest.mocked(apimFetchJsonV2);

describe('apiScoring service', () => {
    beforeEach(() => {
        jest.clearAllMocks();
    });

    describe('getApiScoring', () => {
        it('GETs the v2 scoring report for the API', async () => {
            mockApimFetchJsonV2.mockResolvedValueOnce({ createdAt: '2026-01-01T00:00:00Z', assets: [] });
            await getApiScoring('env-1', 'api-1');
            expect(mockApimFetchJsonV2).toHaveBeenCalledWith('env-1', '/apis/api-1/scoring');
        });

        it('returns undefined when the API has never been scored (404)', async () => {
            mockApimFetchJsonV2.mockRejectedValueOnce(new ApimApiError(404, 'Not found'));
            await expect(getApiScoring('env-1', 'api-1')).resolves.toBeNull();
        });

        it('treats a duck-typed 404 as never scored', async () => {
            mockApimFetchJsonV2.mockRejectedValueOnce({ name: 'ApimApiError', status: 404, message: 'Not found' });
            await expect(getApiScoring('env-1', 'api-1')).resolves.toBeNull();
        });

        it('rethrows non-404 errors', async () => {
            mockApimFetchJsonV2.mockRejectedValueOnce(new ApimApiError(500, 'boom'));
            await expect(getApiScoring('env-1', 'api-1')).rejects.toThrow('boom');
        });

        it('URL-encodes the API id', async () => {
            mockApimFetchJsonV2.mockResolvedValueOnce({ createdAt: '2026-01-01T00:00:00Z', assets: [] });
            await getApiScoring('env-1', 'api/with spaces');
            expect(mockApimFetchJsonV2).toHaveBeenCalledWith('env-1', '/apis/api%2Fwith%20spaces/scoring');
        });
    });

    describe('evaluateApiScoring', () => {
        it('POSTs _evaluate with no body', async () => {
            mockApimFetchJsonV2.mockResolvedValueOnce({ status: 'PENDING' });
            await evaluateApiScoring('env-1', 'api-1');
            expect(mockApimFetchJsonV2).toHaveBeenCalledWith('env-1', '/apis/api-1/scoring/_evaluate', { method: 'POST' });
        });
    });

    describe('listScoringJobs', () => {
        it('lists SCORING_REQUEST jobs for the API', async () => {
            mockApimFetchJsonV2.mockResolvedValueOnce({ data: [] });
            await listScoringJobs('env-1', 'api-1');
            expect(mockApimFetchJsonV2).toHaveBeenCalledWith('env-1', '/async-jobs?page=1&perPage=10&type=SCORING_REQUEST&sourceId=api-1');
        });

        it('normalizes a missing data array to an empty list', async () => {
            mockApimFetchJsonV2.mockResolvedValueOnce({} as Awaited<ReturnType<typeof listScoringJobs>>);
            const result = await listScoringJobs('env-1', 'api-1');
            expect(result.data).toEqual([]);
        });
    });
});
