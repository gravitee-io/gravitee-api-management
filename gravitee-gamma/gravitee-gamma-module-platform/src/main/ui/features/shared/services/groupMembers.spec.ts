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

import { getGroupMembers, listEnvironmentGroups, searchEnvironmentGroupsByIds } from './groupMembers';
import { apimFetchJsonV2 } from '../../../shared/api/apimClient';

jest.mock('../../../shared/api/apimClient', () => ({
    apimFetchJsonV2: jest.fn(),
}));

const mockApimFetchJsonV2 = jest.mocked(apimFetchJsonV2);

describe('environment group search service', () => {
    beforeEach(() => {
        jest.clearAllMocks();
    });

    it('resolves to no groups without requesting the Management API when no group ids are given', async () => {
        const groups = await searchEnvironmentGroupsByIds('env-1', []);

        expect(groups).toEqual([]);
        expect(mockApimFetchJsonV2).not.toHaveBeenCalled();
    });

    it('searches the environment groups by the given ids in a single page sized to the ids', async () => {
        mockApimFetchJsonV2.mockResolvedValue({
            data: [
                { id: 'g1', name: 'Group 1' },
                { id: 'g2', name: 'Group 2' },
            ],
        });

        const groups = await searchEnvironmentGroupsByIds('env-1', ['g1', 'g2']);

        expect(mockApimFetchJsonV2).toHaveBeenCalledTimes(1);
        expect(mockApimFetchJsonV2).toHaveBeenCalledWith('env-1', '/groups/_search?page=1&perPage=2', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ ids: ['g1', 'g2'] }),
        });
        expect(groups).toEqual([
            { id: 'g1', name: 'Group 1' },
            { id: 'g2', name: 'Group 2' },
        ]);
    });

    it('resolves to no groups when the search response has no data', async () => {
        mockApimFetchJsonV2.mockResolvedValue({});

        const groups = await searchEnvironmentGroupsByIds('env-1', ['g1']);

        expect(groups).toEqual([]);
    });
});

describe('environment group listing service', () => {
    beforeEach(() => {
        jest.clearAllMocks();
    });

    it('lists every group of the environment in a single page', async () => {
        mockApimFetchJsonV2.mockResolvedValue({
            data: [
                { id: 'g1', name: 'Group 1' },
                { id: 'g2', name: 'Group 2' },
            ],
        });

        const groups = await listEnvironmentGroups('env-1');

        expect(mockApimFetchJsonV2).toHaveBeenCalledTimes(1);
        expect(mockApimFetchJsonV2).toHaveBeenCalledWith('env-1', '/groups?page=1&perPage=9999');
        expect(groups).toEqual([
            { id: 'g1', name: 'Group 1' },
            { id: 'g2', name: 'Group 2' },
        ]);
    });

    it('resolves to no groups when the listing response has no data', async () => {
        mockApimFetchJsonV2.mockResolvedValue({});

        const groups = await listEnvironmentGroups('env-1');

        expect(groups).toEqual([]);
    });
});

describe('group members listing service', () => {
    beforeEach(() => {
        jest.clearAllMocks();
    });

    it('lists the first page of members of the url-encoded group and maps their roles by scope', async () => {
        mockApimFetchJsonV2.mockResolvedValue({
            data: [
                {
                    id: 'user-1',
                    displayName: 'Jane Doe',
                    roles: [
                        { scope: 'API', name: 'OWNER' },
                        { scope: 'APPLICATION', name: 'USER' },
                    ],
                },
            ],
        });

        const members = await getGroupMembers('env-1', 'a/b');

        expect(mockApimFetchJsonV2).toHaveBeenCalledTimes(1);
        expect(mockApimFetchJsonV2).toHaveBeenCalledWith('env-1', '/groups/a%2Fb/members?page=1&perPage=100');
        expect(members).toEqual([{ id: 'user-1', displayName: 'Jane Doe', roles: { API: 'OWNER', APPLICATION: 'USER' } }]);
    });

    it('resolves a member without id, display name or roles to empty values', async () => {
        mockApimFetchJsonV2.mockResolvedValue({ data: [{}] });

        const members = await getGroupMembers('env-1', 'g1');

        expect(members).toEqual([{ id: '', displayName: '', roles: {} }]);
    });

    it('resolves a role without scope or name to an empty scope mapped to an empty name', async () => {
        mockApimFetchJsonV2.mockResolvedValue({ data: [{ id: 'user-1', displayName: 'Jane Doe', roles: [{}] }] });

        const members = await getGroupMembers('env-1', 'g1');

        expect(members[0].roles).toEqual({ '': '' });
    });

    it('resolves to no members when the members response has no data', async () => {
        mockApimFetchJsonV2.mockResolvedValue({});

        const members = await getGroupMembers('env-1', 'g1');

        expect(members).toEqual([]);
    });
});
