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

import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { renderHook, waitFor } from '@testing-library/react';
import type { ReactNode } from 'react';

import { useEnvironment } from '@gravitee/gamma-modules-sdk';

import { useIntegrationGroupMembers } from './useIntegrationGroupMembers';
import { ApimApiError } from '../../../shared/api/apimClient';
import { searchEnvironmentGroupsByIds } from '../../shared/services/groupMembers';
import type { GroupMember } from '../../shared/types/groupMembers';
import { getIntegrationGroupMembership } from '../services/integrationGroupMembers';

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    useEnvironment: jest.fn(),
}));

jest.mock('../services/integrationGroupMembers');
jest.mock('../../shared/services/groupMembers');

const mockUseEnvironment = jest.mocked(useEnvironment);
const mockGetIntegrationGroupMembership = jest.mocked(getIntegrationGroupMembership);
const mockSearchEnvironmentGroupsByIds = jest.mocked(searchEnvironmentGroupsByIds);

const GROUP_A = { id: 'grp-a', name: 'Group A' };
const ALICE: GroupMember = { id: 'user-1', displayName: 'Alice', roles: { INTEGRATION: 'USER' } };

function wrapper({ children }: { children: ReactNode }) {
    const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    return <QueryClientProvider client={client}>{children}</QueryClientProvider>;
}

describe('useIntegrationGroupMembers', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        mockSearchEnvironmentGroupsByIds.mockReturnValue(new Promise(() => {}));
    });

    it('stays loading without requesting groups or memberships when there is no environment', () => {
        mockUseEnvironment.mockReturnValue(undefined as unknown as ReturnType<typeof useEnvironment>);

        const { result } = renderHook(() => useIntegrationGroupMembers('int-1', ['grp-a']), { wrapper });

        expect(result.current.isLoading).toBe(true);
        expect(result.current.views).toEqual([{ group: { id: 'grp-a', name: 'grp-a' }, status: 'loading' }]);
        expect(mockGetIntegrationGroupMembership).not.toHaveBeenCalled();
        expect(mockSearchEnvironmentGroupsByIds).not.toHaveBeenCalled();
    });

    it('stays loading without requesting memberships when the integration id is empty', () => {
        mockUseEnvironment.mockReturnValue({ id: 'env-1' });

        const { result } = renderHook(() => useIntegrationGroupMembers('', ['grp-a']), { wrapper });

        expect(result.current.isLoading).toBe(true);
        expect(result.current.views).toHaveLength(1);
        expect(result.current.views[0].status).toBe('loading');
        expect(mockGetIntegrationGroupMembership).not.toHaveBeenCalled();
    });

    describe('with an environment and named groups', () => {
        beforeEach(() => {
            mockUseEnvironment.mockReturnValue({ id: 'env-1' });
            mockSearchEnvironmentGroupsByIds.mockResolvedValue([GROUP_A]);
        });

        it('shows the members of a group whose members can be viewed', async () => {
            mockGetIntegrationGroupMembership.mockResolvedValue({ canViewMembers: true, members: [ALICE] });

            const { result } = renderHook(() => useIntegrationGroupMembers('int-1', ['grp-a']), { wrapper });

            await waitFor(() => expect(result.current.isLoading).toBe(false));
            expect(result.current.views).toEqual([{ group: GROUP_A, status: 'loaded', members: [ALICE] }]);
        });

        it('marks a group whose members cannot be viewed as forbidden', async () => {
            mockGetIntegrationGroupMembership.mockResolvedValue({ canViewMembers: false });

            const { result } = renderHook(() => useIntegrationGroupMembers('int-1', ['grp-a']), { wrapper });

            await waitFor(() => expect(result.current.isLoading).toBe(false));
            expect(result.current.views[0]).toEqual({ group: GROUP_A, status: 'forbidden' });
        });

        it('marks a group whose membership request fails as an error', async () => {
            mockGetIntegrationGroupMembership.mockRejectedValue(new ApimApiError(404, 'Not Found'));

            const { result } = renderHook(() => useIntegrationGroupMembers('int-1', ['grp-a']), { wrapper });

            await waitFor(() => expect(result.current.isLoading).toBe(false));
            expect(result.current.views[0].status).toBe('error');
        });

        it('names a group by its id when the group search does not return it', async () => {
            mockSearchEnvironmentGroupsByIds.mockResolvedValue([]);
            mockGetIntegrationGroupMembership.mockResolvedValue({ canViewMembers: false });

            const { result } = renderHook(() => useIntegrationGroupMembers('int-1', ['grp-a']), { wrapper });

            await waitFor(() => expect(result.current.isLoading).toBe(false));
            expect(result.current.views[0].group).toEqual({ id: 'grp-a', name: 'grp-a' });
        });
    });
});
