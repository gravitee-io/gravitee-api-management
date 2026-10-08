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

import { dataTableHarness, renderWithGraphene } from '@gravitee/graphene-core/testing';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { screen, waitFor, within } from '@testing-library/react';

import { IntegrationGroupInheritedMembers } from './IntegrationGroupInheritedMembers';
import { ApimApiError } from '../../../shared/api/apimClient';
import { searchEnvironmentGroupsByIds } from '../../shared/services/groupMembers';
import type { GroupMember } from '../../shared/types/groupMembers';
import { getIntegrationGroupMembership } from '../services/integrationGroupMembers';

jest.mock('../services/integrationGroupMembers', () => ({ getIntegrationGroupMembership: jest.fn() }));
jest.mock('../../shared/services/groupMembers', () => ({
    ...jest.requireActual('../../shared/services/groupMembers'),
    searchEnvironmentGroupsByIds: jest.fn(),
}));

const mockGetIntegrationGroupMembership = jest.mocked(getIntegrationGroupMembership);
const mockSearchEnvironmentGroupsByIds = jest.mocked(searchEnvironmentGroupsByIds);

type GroupFixture = { name: string; membership: { canViewMembers: false } | { canViewMembers: true; members: GroupMember[] } };

function member(id: string, displayName: string): GroupMember {
    return { id, displayName, roles: { INTEGRATION: 'USER' } };
}

function visibleGroup(name: string, members: GroupMember[]): GroupFixture {
    return { name, membership: { canViewMembers: true, members } };
}

const GROUP_A_WITH_ALICE = visibleGroup('Group A', [member('user-1', 'Alice')]);
const GROUP_B_WITH_BOB = visibleGroup('Group B', [member('user-2', 'Bob')]);

function givenGroups(groups: Record<string, GroupFixture>) {
    mockSearchEnvironmentGroupsByIds.mockImplementation(async (_environmentId, ids) => ids.map(id => ({ id, name: groups[id].name })));
    mockGetIntegrationGroupMembership.mockImplementation(async (_environmentId, groupId) => groups[groupId].membership);
}

function renderSection(groupIds: string[]) {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    renderWithGraphene(
        <QueryClientProvider client={queryClient}>
            <IntegrationGroupInheritedMembers integrationId="int-1" groupIds={groupIds} />
        </QueryClientProvider>,
    );
}

const READ_ONLY_GROUPS: Array<[groupName: string, memberNames: string[]]> = [
    ['Platform Team', ['Gina Group', 'Hal Group']],
    ['Data Engineers', ['Ivy Group']],
];

function givenOneVisibleGroup(groupName: string, memberNames: string[]) {
    givenGroups({
        'grp-1': visibleGroup(
            groupName,
            memberNames.map((name, index) => member(`user-${index}`, name)),
        ),
    });
}

describe('IntegrationGroupInheritedMembers', () => {
    beforeAll(() => {
        Object.defineProperty(window, 'matchMedia', {
            writable: true,
            value: jest.fn().mockImplementation((query: string) => ({
                matches: false,
                media: query,
                onchange: null,
                addListener: jest.fn(),
                removeListener: jest.fn(),
                addEventListener: jest.fn(),
                removeEventListener: jest.fn(),
                dispatchEvent: jest.fn(),
            })),
        });
    });

    beforeEach(() => {
        jest.clearAllMocks();
    });

    it.each<[scenario: string, groupIds: string[], expectedGroupNames: string[]]>([
        ['two associated groups', ['grp-a', 'grp-b'], ['Group A', 'Group B']],
        ['a single associated group', ['grp-a'], ['Group A']],
    ])('shows one group table per group with inherited members for %s', async (_scenario, groupIds, expectedGroupNames) => {
        givenGroups({ 'grp-a': GROUP_A_WITH_ALICE, 'grp-b': GROUP_B_WITH_BOB });

        renderSection(groupIds);

        await waitFor(() => expect(screen.getAllByRole('table')).toHaveLength(expectedGroupNames.length));
        for (const groupName of expectedGroupNames) {
            expect(screen.getByText(groupName)).toBeInTheDocument();
        }
    });

    it('shows the permission notice instead of a table for a group whose membership the viewer may not see', async () => {
        givenGroups({ 'grp-a': { name: 'Group A', membership: { canViewMembers: false } } });

        renderSection(['grp-a']);

        expect(await screen.findByText('You do not have the appropriate permissions to view members of this group.')).toBeInTheDocument();
        expect(screen.queryByRole('table')).toBeNull();
    });

    it.each(READ_ONLY_GROUPS)('offers no role selector on any member row of the %s group table', async (groupName, memberNames) => {
        givenOneVisibleGroup(groupName, memberNames);

        renderSection(['grp-1']);
        await screen.findByRole('table');

        for (const memberName of memberNames) {
            const row = dataTableHarness().getRow(memberName).getElement();
            expect(within(row).queryByRole('combobox')).toBeNull();
            expect(within(row).queryByRole('listbox')).toBeNull();
        }
        expect(dataTableHarness().getRowCount()).toBe(memberNames.length);
    });

    it.each(READ_ONLY_GROUPS)('offers no remove action on any member row of the %s group table', async (groupName, memberNames) => {
        givenOneVisibleGroup(groupName, memberNames);

        renderSection(['grp-1']);
        await screen.findByRole('table');

        for (const memberName of memberNames) {
            const row = dataTableHarness().getRow(memberName).getElement();
            expect(within(row).queryByRole('button')).toBeNull();
            expect(within(row).queryByRole('menuitem')).toBeNull();
        }
        expect(dataTableHarness().getRowCount()).toBe(memberNames.length);
    });

    it.each<[scenario: string, groupIds: string[]]>([
        ['its only associated group has no inherited member', ['grp-a']],
        ['the integration has no associated group', []],
    ])('shows a single No group members empty card, with no group table, when %s', async (_scenario, groupIds) => {
        givenGroups({ 'grp-a': visibleGroup('Group A', []) });

        renderSection(groupIds);

        expect(await screen.findAllByText('No group members')).toHaveLength(1);
        expect(screen.queryByRole('table')).toBeNull();
    });

    it('shows no group table for an associated group that has no inherited member next to one that has', async () => {
        givenGroups({ 'grp-a': GROUP_A_WITH_ALICE, 'grp-b': visibleGroup('Group B', []) });

        renderSection(['grp-a', 'grp-b']);

        await screen.findByRole('table');
        expect(screen.getByText('Group A')).toBeInTheDocument();
        expect(screen.getAllByRole('table')).toHaveLength(1);
        expect(dataTableHarness().getRow('Alice')).toBeDefined();
        expect(screen.queryByText('Group B')).toBeNull();
        expect(screen.queryByText('grp-b')).toBeNull();
    });

    it('shows a dash as the role of an inherited member who holds no integration role', async () => {
        givenGroups({ 'grp-a': visibleGroup('Group A', [{ id: 'user-1', displayName: 'Alice', roles: { GROUP: 'ADMIN' } }]) });

        renderSection(['grp-a']);

        await screen.findByRole('table');
        expect(dataTableHarness().getRow('Alice').getCellText('Role')).toBe('—');
    });

    it('lists the group tables in the order the integration associates the groups', async () => {
        givenGroups({ 'grp-a': GROUP_A_WITH_ALICE, 'grp-b': GROUP_B_WITH_BOB });

        renderSection(['grp-b', 'grp-a']);

        await waitFor(() => expect(screen.getAllByRole('table')).toHaveLength(2));
        const groupTitles = screen.getAllByText(/^Group [AB]$/).map(element => element.textContent);
        expect(groupTitles).toEqual(['Group B', 'Group A']);
    });

    it.each<[scenario: string, resolveGroupNames: () => Promise<Array<{ id: string; name: string }>>]>([
        ['is missing from the group search response', async () => []],
        ['cannot be searched', () => Promise.reject(new ApimApiError(403, 'Forbidden'))],
    ])('labels the group table with the group id when the group name %s', async (_scenario, resolveGroupNames) => {
        givenGroups({ 'grp-a': GROUP_A_WITH_ALICE });
        mockSearchEnvironmentGroupsByIds.mockImplementation(resolveGroupNames);

        renderSection(['grp-a']);

        await screen.findByRole('table');
        expect(screen.getByText('grp-a')).toBeInTheDocument();
        expect(screen.queryByText('Group A')).toBeNull();
    });

    it('shows a load failure alert naming the group, and no No group members card, for a group whose members fail to load', async () => {
        givenGroups({ 'grp-a': visibleGroup('Group A', []) });
        mockGetIntegrationGroupMembership.mockRejectedValue(new ApimApiError(404, 'Not Found'));

        renderSection(['grp-a']);

        expect(await screen.findByText('Failed to load members for group Group A.')).toBeInTheDocument();
        expect(screen.queryByText('No group members')).toBeNull();
    });

    it('shows the permission notice and no No group members card when the other associated group has no inherited member', async () => {
        givenGroups({
            'grp-a': { name: 'Group A', membership: { canViewMembers: false } },
            'grp-b': visibleGroup('Group B', []),
        });

        renderSection(['grp-a', 'grp-b']);

        expect(await screen.findByText('You do not have the appropriate permissions to view members of this group.')).toBeInTheDocument();
        expect(screen.getByText('Group A')).toBeInTheDocument();
        expect(screen.queryByText('No group members')).toBeNull();
    });

    it('shows neither the No group members card nor a group table while group memberships are still loading', async () => {
        givenGroups({ 'grp-a': visibleGroup('Group A', []) });
        mockGetIntegrationGroupMembership.mockReturnValue(new Promise(() => {}));

        renderSection(['grp-a']);

        expect(await screen.findByText('Group Inherited Members')).toBeInTheDocument();
        await waitFor(() => expect(mockGetIntegrationGroupMembership).toHaveBeenCalled());
        expect(screen.queryByText('No group members')).toBeNull();
        expect(screen.queryByRole('table')).toBeNull();
    });
});
