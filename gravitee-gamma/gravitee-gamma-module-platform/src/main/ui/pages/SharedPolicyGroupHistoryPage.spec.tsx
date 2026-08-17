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

import { render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';

import { SharedPolicyGroupHistoryPage } from './SharedPolicyGroupHistoryPage';
import { useSharedPolicyGroupHistoryList } from '../features/shared-policy-groups/hooks/useSharedPolicyGroupHistoryList';
import type { SharedPolicyGroup } from '../features/shared-policy-groups/types/sharedPolicyGroup';

jest.mock('../features/shared-policy-groups/hooks/useSharedPolicyGroupHistoryList');

const mockUseSharedPolicyGroupHistoryList = jest.mocked(useSharedPolicyGroupHistoryList);

const HISTORY: SharedPolicyGroup = {
    id: 'spg-1',
    name: 'Auth Bundle',
    description: 'Reusable auth policies',
    lifecycleState: 'DEPLOYED',
    version: 2,
    apiType: 'PROXY',
    phase: 'REQUEST',
    updatedAt: '2024-01-01T00:00:00.000Z',
    deployedAt: '2024-01-02T00:00:00.000Z',
};

function listState(overrides: Partial<ReturnType<typeof useSharedPolicyGroupHistoryList>> = {}) {
    return {
        page: 1,
        pageSize: 25,
        sorting: [],
        setPage: jest.fn(),
        setPageSize: jest.fn(),
        setSorting: jest.fn(),
        histories: [HISTORY],
        totalCount: 1,
        isLoading: false,
        isError: false,
        ...overrides,
    };
}

function renderPage() {
    return render(
        <MemoryRouter initialEntries={['/spg-1/history']}>
            <Routes>
                <Route path=":sharedPolicyGroupId/history" element={<SharedPolicyGroupHistoryPage />} />
            </Routes>
        </MemoryRouter>,
    );
}

describe('SharedPolicyGroupHistoryPage', () => {
    afterEach(() => {
        jest.clearAllMocks();
    });

    it('loads histories for the route sharedPolicyGroupId', () => {
        mockUseSharedPolicyGroupHistoryList.mockReturnValue(listState());
        renderPage();
        expect(mockUseSharedPolicyGroupHistoryList).toHaveBeenCalledWith('spg-1');
        expect(screen.getByTestId('shared-policy-group-history')).not.toBeNull();
        expect(screen.queryByText('Auth Bundle')).not.toBeNull();
        expect(screen.queryByText('2')).not.toBeNull();
    });

    it('shows the empty state when there are no history entries', () => {
        mockUseSharedPolicyGroupHistoryList.mockReturnValue(listState({ histories: [], totalCount: 0 }));
        renderPage();
        expect(screen.getByTestId('shared-policy-group-history-empty')).not.toBeNull();
        expect(screen.getByText('No version history yet')).not.toBeNull();
    });

    it('shows an error message when the histories request fails', () => {
        mockUseSharedPolicyGroupHistoryList.mockReturnValue(listState({ isError: true, histories: [], totalCount: 0 }));
        renderPage();
        expect(screen.getByTestId('shared-policy-group-history-error')).not.toBeNull();
        expect(screen.getByText('Failed to load version history.')).not.toBeNull();
    });
});
