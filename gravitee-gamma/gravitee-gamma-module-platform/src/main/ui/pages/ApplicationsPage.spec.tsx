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
import { useHasPermission } from '@gravitee/gamma-modules-sdk';
import { TooltipProvider } from '@gravitee/graphene-core';
import { act, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';

import { ApplicationsPage } from './ApplicationsPage';
import { useApplicationList } from '../features/applications/hooks/useApplicationList';
import { useApplicationStats } from '../features/applications/hooks/useApplicationStats';
import { useOrganizationAdmin } from '../features/applications/hooks/useOrganizationAdmin';
import { useRestoreApplication } from '../features/applications/hooks/useRestoreApplication';

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    useHasPermission: jest.fn(),
}));
jest.mock('../features/applications/hooks/useApplicationList');
jest.mock('../features/applications/hooks/useApplicationStats');
jest.mock('../features/applications/hooks/useOrganizationAdmin');
jest.mock('../features/applications/hooks/useRestoreApplication');

const mockUseHasPermission = jest.mocked(useHasPermission);
const mockUseApplicationList = jest.mocked(useApplicationList);
const mockUseApplicationStats = jest.mocked(useApplicationStats);
const mockUseOrganizationAdmin = jest.mocked(useOrganizationAdmin);
const mockUseRestoreApplication = jest.mocked(useRestoreApplication);

const STUB_STATS = {
    active: 0,
    archived: 0,
    total: 0,
    isLoading: false,
    isLoadingActive: false,
    isLoadingArchived: false,
};

const EMPTY_PAGE = { current: 1, size: 0, per_page: 25, total_pages: 0, total_elements: 0 };

const STUB_APPLICATION = {
    id: 'app-1',
    name: 'Billing App',
    status: 'ACTIVE' as const,
    created_at: 0,
    updated_at: 0,
};

function statsResult(overrides: Partial<ReturnType<typeof useApplicationStats>> = {}): ReturnType<typeof useApplicationStats> {
    return { ...STUB_STATS, ...overrides } as ReturnType<typeof useApplicationStats>;
}

function listHookResult(overrides: Partial<ReturnType<typeof useApplicationList>> = {}): ReturnType<typeof useApplicationList> {
    return {
        data: { data: [], page: EMPTY_PAGE },
        isLoading: false,
        isFetching: false,
        isError: false,
        ...overrides,
    } as ReturnType<typeof useApplicationList>;
}

function renderPage() {
    return render(
        <MemoryRouter>
            <TooltipProvider>
                <ApplicationsPage />
            </TooltipProvider>
        </MemoryRouter>,
    );
}

describe('ApplicationsPage', () => {
    beforeEach(() => {
        mockUseHasPermission.mockReturnValue(true);
        mockUseOrganizationAdmin.mockReturnValue({ isAdmin: true, isLoading: false });
        mockUseApplicationStats.mockReturnValue(STUB_STATS);
        mockUseRestoreApplication.mockReturnValue({
            mutate: jest.fn(),
            isPending: false,
        } as ReturnType<typeof useRestoreApplication>);
    });

    afterEach(() => {
        jest.clearAllMocks();
        jest.useRealTimers();
    });

    it('shows the empty landing when there are no applications and no active search', () => {
        mockUseApplicationList.mockReturnValue(listHookResult());
        renderPage();

        expect(screen.queryByText('Why register an application?')).not.toBeNull();
        expect(screen.queryByPlaceholderText('Search applications...')).toBeNull();
    });

    it('hides the Register Application button on the empty landing when the user cannot create', () => {
        mockUseHasPermission.mockReturnValue(false);
        mockUseApplicationList.mockReturnValue(listHookResult());
        renderPage();

        expect(screen.queryByRole('button', { name: /Register Application/i })).toBeNull();
    });

    it('hides the Register Application button on the list view when the user cannot create', () => {
        mockUseHasPermission.mockReturnValue(false);
        mockUseApplicationList.mockReturnValue(
            listHookResult({
                data: { data: [STUB_APPLICATION], page: { ...EMPTY_PAGE, total_elements: 1, total_pages: 1 } },
            }),
        );
        mockUseApplicationStats.mockReturnValue(statsResult({ active: 1, archived: 0, total: 1 }));
        renderPage();

        expect(screen.queryByRole('button', { name: /Register Application/i })).toBeNull();
        expect(screen.queryByPlaceholderText('Search applications...')).not.toBeNull();
    });

    it('shows the list view when only archived applications exist', () => {
        mockUseApplicationList.mockReturnValue(listHookResult());
        mockUseApplicationStats.mockReturnValue(statsResult({ active: 0, archived: 3, total: 3 }));
        renderPage();

        expect(screen.queryByText('Why register an application?')).toBeNull();
        expect(screen.queryByPlaceholderText('Search applications...')).not.toBeNull();
    });

    it('shows the list view while stats are loading — does not flash the empty landing', () => {
        mockUseApplicationList.mockReturnValue(listHookResult());
        mockUseApplicationStats.mockReturnValue(
            statsResult({
                active: null,
                archived: null,
                total: null,
                isLoading: true,
                isLoadingActive: true,
                isLoadingArchived: true,
            }),
        );
        renderPage();

        expect(screen.queryByText('Why register an application?')).toBeNull();
        expect(screen.queryByPlaceholderText('Search applications...')).not.toBeNull();
    });

    it('shows the list view when applications exist', () => {
        mockUseApplicationList.mockReturnValue(
            listHookResult({
                data: { data: [STUB_APPLICATION], page: { ...EMPTY_PAGE, total_elements: 1, total_pages: 1 } },
            }),
        );
        mockUseApplicationStats.mockReturnValue(statsResult({ active: 1, archived: 0, total: 1 }));
        renderPage();

        expect(screen.queryByText('Why register an application?')).toBeNull();
        expect(screen.queryByPlaceholderText('Search applications...')).not.toBeNull();
        expect(screen.queryByText('Billing App')).not.toBeNull();
    });

    it('shows the list view while loading — does not flash the empty landing', () => {
        mockUseApplicationList.mockReturnValue(listHookResult({ data: undefined, isLoading: true }));
        renderPage();

        expect(screen.queryByText('Why register an application?')).toBeNull();
        expect(screen.queryByPlaceholderText('Search applications...')).not.toBeNull();
    });

    it('shows the list view while fetching — does not flash the empty landing', () => {
        mockUseApplicationList.mockReturnValue(
            listHookResult({
                data: { data: [], page: EMPTY_PAGE },
                isFetching: true,
            }),
        );
        renderPage();

        expect(screen.queryByText('Why register an application?')).toBeNull();
        expect(screen.queryByPlaceholderText('Search applications...')).not.toBeNull();
    });

    it('shows the list view when the search term is active but there are no matches', () => {
        mockUseApplicationList.mockReturnValue(
            listHookResult({
                data: { data: [STUB_APPLICATION], page: { ...EMPTY_PAGE, total_elements: 1, total_pages: 1 } },
            }),
        );
        mockUseApplicationStats.mockReturnValue(statsResult({ active: 1, archived: 0, total: 1 }));
        renderPage();

        mockUseApplicationList.mockReturnValue(listHookResult());
        fireEvent.change(screen.getByPlaceholderText('Search applications...'), { target: { value: 'billing' } });

        expect(screen.queryByText('Why register an application?')).toBeNull();
        expect(screen.queryByPlaceholderText('Search applications...')).not.toBeNull();
    });

    it('shows an error message when the list query fails', () => {
        mockUseApplicationList.mockReturnValue(listHookResult({ data: undefined, isError: true }));
        renderPage();

        expect(screen.queryByText('Failed to load applications. Please refresh and try again.')).not.toBeNull();
        expect(screen.queryByText('Why register an application?')).toBeNull();
    });

    it('resets page to 1 when the search term changes', async () => {
        jest.useFakeTimers();
        mockUseApplicationList.mockReturnValue(
            listHookResult({
                data: { data: [STUB_APPLICATION], page: { ...EMPTY_PAGE, total_elements: 1, total_pages: 1 } },
            }),
        );
        mockUseApplicationStats.mockReturnValue(statsResult({ active: 1, archived: 0, total: 1 }));
        renderPage();

        fireEvent.change(screen.getByPlaceholderText('Search applications...'), { target: { value: 'billing' } });
        act(() => {
            jest.advanceTimersByTime(300);
        });

        await waitFor(() => {
            const searchCall = mockUseApplicationList.mock.calls.find(([params]) => params.query === 'billing');
            expect(searchCall).toBeDefined();
            expect(searchCall![0].page).toBe(1);
        });
    });
});
