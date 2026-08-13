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

import { createSharedPolicyGroup, deleteSharedPolicyGroup, getSharedPolicyGroup, listSharedPolicyGroupsPaged } from './sharedPolicyGroups';
import { apimFetchJsonV2 } from '../../../shared/api/apimClient';
import type { CreateSharedPolicyGroupPayload } from '../types/sharedPolicyGroup';

jest.mock('../../../shared/api/apimClient', () => ({
    apimFetchJsonV2: jest.fn(),
}));

const mockApimFetchJsonV2 = jest.mocked(apimFetchJsonV2);

describe('shared policy groups service', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        mockApimFetchJsonV2.mockResolvedValue(undefined);
    });

    it('lists groups with classic Console pagination, search, and sorting parameters', async () => {
        await listSharedPolicyGroupsPaged('env-1', { query: ' auth & rate limit ', page: 2, perPage: 25, sortBy: '-name' });

        expect(mockApimFetchJsonV2).toHaveBeenCalledWith(
            'env-1',
            '/shared-policy-groups?page=2&perPage=25&q=auth+%26+rate+limit&sortBy=-name',
        );
    });

    it('URL-encodes an id when loading a group', async () => {
        await getSharedPolicyGroup('env-1', 'group/with spaces');

        expect(mockApimFetchJsonV2).toHaveBeenCalledWith('env-1', '/shared-policy-groups/group%2Fwith%20spaces');
    });

    it('creates a group with the serialized Management API v2 payload', async () => {
        const payload: CreateSharedPolicyGroupPayload = {
            name: 'Authentication',
            apiType: 'PROXY',
            phase: 'REQUEST',
        };

        await createSharedPolicyGroup('env-1', payload);

        expect(mockApimFetchJsonV2).toHaveBeenCalledWith('env-1', '/shared-policy-groups', {
            method: 'POST',
            body: JSON.stringify(payload),
        });
    });

    it('deletes the encoded group resource', async () => {
        await deleteSharedPolicyGroup('env-1', 'group/1');

        expect(mockApimFetchJsonV2).toHaveBeenCalledWith('env-1', '/shared-policy-groups/group%2F1', {
            method: 'DELETE',
        });
    });
});
