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
import { getScoringOverview, listApisScoring } from './scoring';
import { apimFetchJsonV2 } from '../../../shared/api/apimClient';

jest.mock('../../../shared/api/apimClient', () => ({
    apimFetchJsonV2: jest.fn(),
}));

const mockApimFetchJsonV2 = jest.mocked(apimFetchJsonV2);

describe('environment scoring service', () => {
    beforeEach(() => {
        jest.clearAllMocks();
    });

    describe('getScoringOverview', () => {
        it('GETs v2 scoring/overview for the environment', async () => {
            mockApimFetchJsonV2.mockResolvedValueOnce({ id: 'env-1', score: 0.83, errors: 3, warnings: 5, infos: 2, hints: 1 });
            await getScoringOverview('env-1');
            expect(mockApimFetchJsonV2).toHaveBeenCalledWith('env-1', '/scoring/overview');
        });
    });

    describe('listApisScoring', () => {
        it('GETs v2 scoring/apis with page and perPage', async () => {
            mockApimFetchJsonV2.mockResolvedValueOnce({
                data: [],
                pagination: { page: 1, perPage: 10, pageCount: 0, pageItemsCount: 0, totalCount: 0 },
            });
            await listApisScoring('env-1', { page: 1, perPage: 10 });
            expect(mockApimFetchJsonV2).toHaveBeenCalledWith('env-1', '/scoring/apis?page=1&perPage=10');
        });

        it('normalizes a missing data array to an empty list', async () => {
            mockApimFetchJsonV2.mockResolvedValueOnce({});
            const result = await listApisScoring('env-1', { page: 2, perPage: 10 });
            expect(result.data).toEqual([]);
            expect(result.pagination).toEqual({ page: 2, perPage: 10, pageCount: 0, pageItemsCount: 0, totalCount: 0 });
        });
    });
});
