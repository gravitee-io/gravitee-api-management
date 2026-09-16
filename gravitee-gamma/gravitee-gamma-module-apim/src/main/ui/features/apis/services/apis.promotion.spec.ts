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
import { getPendingPromotions, getPromotionTargets, promoteApi } from './apis';
import { apimFetchJsonOrg, apimFetchJsonV1Env, apimFetchJsonV2 } from '../../../shared/api/apimClient';

jest.mock('../../../shared/api/apimClient', () => ({
    apimFetchJsonV1Env: jest.fn(),
    apimFetchJsonOrg: jest.fn(),
    apimFetchJsonV2: jest.fn(),
}));

const mockApimFetchJsonV1Env = jest.mocked(apimFetchJsonV1Env);
const mockApimFetchJsonOrg = jest.mocked(apimFetchJsonOrg);
const mockApimFetchJsonV2 = jest.mocked(apimFetchJsonV2);

describe('apis promotion', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        mockApimFetchJsonV1Env.mockResolvedValue([]);
        mockApimFetchJsonOrg.mockResolvedValue([]);
        mockApimFetchJsonV2.mockResolvedValue(undefined);
    });

    describe('getPromotionTargets', () => {
        it('fetches the env-scoped v1 promotion-targets endpoint', async () => {
            await getPromotionTargets('env-1');

            expect(mockApimFetchJsonV1Env).toHaveBeenCalledWith('env-1', '/promotion-targets');
        });
    });

    describe('getPendingPromotions', () => {
        it('posts the org-scoped promotions search with apiId and pending statuses', async () => {
            await getPendingPromotions('api-1');

            expect(mockApimFetchJsonOrg).toHaveBeenCalledWith('/promotions/_search?apiId=api-1&statuses=CREATED&statuses=TO_BE_VALIDATED', {
                method: 'POST',
            });
        });

        it('encodes the apiId', async () => {
            await getPendingPromotions('api/1');

            expect(mockApimFetchJsonOrg).toHaveBeenCalledWith(
                '/promotions/_search?apiId=api%2F1&statuses=CREATED&statuses=TO_BE_VALIDATED',
                { method: 'POST' },
            );
        });
    });

    describe('promoteApi', () => {
        it('posts the target env to the v2 promote endpoint', async () => {
            await promoteApi('env-1', 'api-1', { targetEnvCockpitId: 'env#2', targetEnvName: 'Production' });

            expect(mockApimFetchJsonV2).toHaveBeenCalledWith('env-1', '/apis/api-1/_promote', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ targetEnvCockpitId: 'env#2', targetEnvName: 'Production' }),
            });
        });
    });
});
