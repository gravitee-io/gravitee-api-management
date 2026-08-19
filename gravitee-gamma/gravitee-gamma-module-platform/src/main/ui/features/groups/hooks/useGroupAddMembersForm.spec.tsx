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
import { act, renderHook } from '@testing-library/react';
import type { ReactNode } from 'react';

import { useGroupAddMembersForm } from './useGroupAddMembersForm';
import { searchUsers } from '../../../shared/services/userSearch';
import { GROUP_SEARCH_DEBOUNCE_MS } from '../utils/paginationConstants';

jest.mock('../../../shared/services/userSearch', () => ({
    searchUsers: jest.fn(),
}));

const mockSearchUsers = jest.mocked(searchUsers);

describe('useGroupAddMembersForm', () => {
    let queryClient: QueryClient;

    beforeEach(() => {
        jest.useFakeTimers();
        queryClient = new QueryClient({
            defaultOptions: {
                queries: { retry: false },
            },
        });
        mockSearchUsers.mockResolvedValue([]);
    });

    afterEach(() => {
        queryClient.clear();
        jest.useRealTimers();
        jest.clearAllMocks();
    });

    function wrapper({ children }: { children: ReactNode }) {
        return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
    }

    function renderForm(open: boolean, initialSearch?: string) {
        return renderHook(
            ({ isOpen, seed }) =>
                useGroupAddMembersForm({
                    open: isOpen,
                    groupRoles: undefined,
                    members: [],
                    lockApiRole: false,
                    lockApiProductRole: false,
                    lockApplicationRole: false,
                    canOverrideLocks: true,
                    maxInvitation: null,
                    apiPrimaryOwnerMode: 'GROUP',
                    apiProductPrimaryOwnerMode: 'GROUP',
                    initialSearch: seed,
                    onSubmit: jest.fn(),
                }),
            { initialProps: { isOpen: open, seed: initialSearch }, wrapper },
        );
    }

    it('collapses rapid search input into one request for the final query', async () => {
        const { result } = renderForm(true);

        act(() => result.current.setSearch('an'));
        act(() => {
            jest.advanceTimersByTime(GROUP_SEARCH_DEBOUNCE_MS - 1);
        });
        act(() => result.current.setSearch('anna'));
        act(() => {
            jest.advanceTimersByTime(GROUP_SEARCH_DEBOUNCE_MS - 1);
        });
        expect(mockSearchUsers).not.toHaveBeenCalled();

        await act(async () => {
            jest.advanceTimersByTime(1);
            await Promise.resolve();
        });

        expect(mockSearchUsers).toHaveBeenCalledTimes(1);
        expect(mockSearchUsers).toHaveBeenCalledWith('anna');
    });

    it('does not debounce or query while the sheet is closed', async () => {
        const { result } = renderForm(false);

        act(() => {
            result.current.setSearch('anna');
            jest.advanceTimersByTime(GROUP_SEARCH_DEBOUNCE_MS);
        });
        await act(async () => {
            await Promise.resolve();
        });

        expect(result.current.debouncedQuery).toBe('');
        expect(mockSearchUsers).not.toHaveBeenCalled();
    });

    it('seeds search from initialSearch when the sheet opens', async () => {
        const { result } = renderForm(true, 'anna@lufthansa.com');

        expect(result.current.search).toBe('anna@lufthansa.com');

        await act(async () => {
            jest.advanceTimersByTime(GROUP_SEARCH_DEBOUNCE_MS);
            await Promise.resolve();
        });

        expect(mockSearchUsers).toHaveBeenCalledWith('anna@lufthansa.com');
    });
});
