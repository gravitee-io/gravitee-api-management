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
import { getPolicyDocumentation, getPolicySchema, listPolicies } from './sharedPolicyGroupPolicies';
import { apimFetchJsonV2Org } from '../../../shared/api/apimClient';

jest.mock('../../../shared/api/apimClient', () => ({
    apimFetchJsonV2Org: jest.fn(),
}));

const mockApimFetchJsonV2Org = jest.mocked(apimFetchJsonV2Org);

describe('shared policy group policy catalog service', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        mockApimFetchJsonV2Org.mockResolvedValue(undefined);
    });

    it('loads the same policy catalog endpoint as Classic Console', async () => {
        await listPolicies();

        expect(mockApimFetchJsonV2Org).toHaveBeenCalledWith('/plugins/policies');
    });

    it('loads an encoded policy schema for the Shared Policy Group API protocol', async () => {
        await getPolicySchema('policy/with spaces', 'HTTP_PROXY');

        expect(mockApimFetchJsonV2Org).toHaveBeenCalledWith('/plugins/policies/policy%2Fwith%20spaces/schema?apiProtocolType=HTTP_PROXY');
    });

    it('loads encoded policy documentation for the Shared Policy Group API protocol', async () => {
        await getPolicyDocumentation('policy/with spaces', 'HTTP_MESSAGE');

        expect(mockApimFetchJsonV2Org).toHaveBeenCalledWith(
            '/plugins/policies/policy%2Fwith%20spaces/documentation-ext?apiProtocolType=HTTP_MESSAGE',
        );
    });
});
