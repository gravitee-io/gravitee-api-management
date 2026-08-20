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

import { useCurrentUserIsGroupAdmin } from './useCurrentUserGroupAdmin';
import { fetchCurrentUser } from '../../../shared/services/currentUser';
import type { GroupMember } from '../types/group';

jest.mock('../../../shared/services/currentUser', () => ({
    fetchCurrentUser: jest.fn(),
}));

const mockFetchCurrentUser = jest.mocked(fetchCurrentUser);
const MEMBERS: GroupMember[] = [
    { id: 'current-user', displayName: 'Current User', roles: { GROUP: 'ADMIN' } },
    { id: 'other-user', displayName: 'Other User', roles: {} },
];

describe('useCurrentUserIsGroupAdmin', () => {
    function wrapper({ children }: { children: ReactNode }) {
        return (
            <QueryClientProvider client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}>
                {children}
            </QueryClientProvider>
        );
    }

    afterEach(() => {
        jest.clearAllMocks();
    });

    it('returns true only for the current group admin', async () => {
        mockFetchCurrentUser.mockResolvedValue({ id: 'current-user' });
        const { result } = renderHook(() => useCurrentUserIsGroupAdmin(MEMBERS), { wrapper });

        await waitFor(() => expect(result.current).toBe(true));
    });

    it('does not fetch the current user when every member action is already authorized', () => {
        const { result } = renderHook(() => useCurrentUserIsGroupAdmin(MEMBERS, { enabled: false }), { wrapper });

        expect(result.current).toBe(false);
        expect(mockFetchCurrentUser).not.toHaveBeenCalled();
    });
});
