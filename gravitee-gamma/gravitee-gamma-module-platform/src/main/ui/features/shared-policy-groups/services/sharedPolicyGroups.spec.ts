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

import { deploySharedPolicyGroup, undeploySharedPolicyGroup } from './sharedPolicyGroups';
import { apimFetchJsonV2 } from '../../../shared/api/apimClient';

jest.mock('../../../shared/api/apimClient', () => ({
    apimFetchJsonV2: jest.fn(),
}));

const mockApimFetchJsonV2 = jest.mocked(apimFetchJsonV2);

describe('sharedPolicyGroups service — deploy / undeploy', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        mockApimFetchJsonV2.mockResolvedValue(undefined);
    });

    it('deploySharedPolicyGroup POSTs to /_deploy', async () => {
        await deploySharedPolicyGroup('env-1', 'spg-1');
        expect(mockApimFetchJsonV2).toHaveBeenCalledWith('env-1', '/shared-policy-groups/spg-1/_deploy', {
            method: 'POST',
            body: JSON.stringify({}),
        });
    });

    it('undeploySharedPolicyGroup POSTs to /_undeploy', async () => {
        await undeploySharedPolicyGroup('env-1', 'spg-1');
        expect(mockApimFetchJsonV2).toHaveBeenCalledWith('env-1', '/shared-policy-groups/spg-1/_undeploy', {
            method: 'POST',
            body: JSON.stringify({}),
        });
    });

    it('encodes the shared policy group id in the path', async () => {
        await deploySharedPolicyGroup('env-1', 'spg/with spaces');
        expect(mockApimFetchJsonV2).toHaveBeenCalledWith(
            'env-1',
            '/shared-policy-groups/spg%2Fwith%20spaces/_deploy',
            expect.any(Object),
        );
    });
});
