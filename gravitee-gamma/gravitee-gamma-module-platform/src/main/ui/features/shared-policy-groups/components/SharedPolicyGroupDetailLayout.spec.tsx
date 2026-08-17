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

import { act, fireEvent, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { useState } from 'react';
import { createMemoryRouter, MemoryRouter, Route, RouterProvider, Routes } from 'react-router-dom';

import { SharedPolicyGroupDetailLayout } from './SharedPolicyGroupDetailLayout';
import { ApimApiError } from '../../../shared/api/apimClient';
import { useForbiddenResourceRedirect } from '../../../shared/hooks/useForbiddenResourceRedirect';
import { useUpdateSharedPolicyGroup } from '../hooks/useSharedPolicyGroupMutations';
import { useSharedPolicyGroupDetail } from '../hooks/useSharedPolicyGroups';
import type { SharedPolicyGroup } from '../types/sharedPolicyGroup';

const mockNavigate = jest.fn();
let capturedLayoutConfig: Record<string, unknown> | null = null;

jest.mock('react-router-dom', () => ({
    ...jest.requireActual('react-router-dom'),
    useNavigate: () => mockNavigate,
}));

jest.mock('@gravitee/graphene-core', () => ({
    ...jest.requireActual<object>('@gravitee/graphene-core'),
    useLayoutConfig: jest.fn((config: Record<string, unknown>) => {
        capturedLayoutConfig = config;
    }),
}));

jest.mock('../hooks/useSharedPolicyGroups');
jest.mock('../hooks/useSharedPolicyGroupMutations');
jest.mock('../../../shared/hooks/useForbiddenResourceRedirect');

const mockUseDetail = jest.mocked(useSharedPolicyGroupDetail);
const mockUseUpdateSharedPolicyGroup = jest.mocked(useUpdateSharedPolicyGroup);
const mockUseForbiddenResourceRedirect = jest.mocked(useForbiddenResourceRedirect);

const SPG: SharedPolicyGroup = {
    id: 'spg-1',
    name: 'Auth Bundle',
    description: 'Reusable auth policies',
    lifecycleState: 'DEPLOYED',
    apiType: 'PROXY',
    phase: 'REQUEST',
};

function renderLayout(path = '/shared-policy-groups/spg-1/studio') {
    return render(
        <MemoryRouter initialEntries={[path]}>
            <Routes>
                <Route path="/shared-policy-groups">
                    <Route path=":sharedPolicyGroupId" element={<SharedPolicyGroupDetailLayout />}>
                        <Route path="overview" element={<div>Overview content</div>} />
                        <Route path="studio" element={<div>Studio content</div>} />
                        <Route path="history" element={<div>History content</div>} />
                    </Route>
                </Route>
            </Routes>
        </MemoryRouter>,
    );
}

function StatefulOutlet() {
    const [dirty, setDirty] = useState(false);
    return (
        <>
            <span>{dirty ? 'Dirty' : 'Clean'}</span>
            <button type="button" onClick={() => setDirty(true)}>
                Mark dirty
            </button>
        </>
    );
}

describe('SharedPolicyGroupDetailLayout', () => {
    beforeEach(() => {
        mockUseUpdateSharedPolicyGroup.mockReturnValue({ mutateAsync: jest.fn() } as never);
    });

    afterEach(() => {
        jest.clearAllMocks();
    });

    it('renders header, status badge, tabs, and outlet content', () => {
        mockUseDetail.mockReturnValue({ data: SPG, isLoading: false, isError: false } as never);
        renderLayout();

        expect(screen.getByTestId('shared-policy-group-detail')).not.toBeNull();
        expect(screen.getByRole('heading', { name: 'Auth Bundle' })).not.toBeNull();
        expect(screen.getByText('Deployed')).not.toBeNull();
        expect(screen.getByText('Reusable auth policies')).not.toBeNull();
        expect(screen.getByText('Proxy · Request')).not.toBeNull();
        expect(screen.getByRole('button', { name: 'Edit' })).not.toBeNull();
        expect(screen.getByRole('link', { name: 'Overview' })).not.toBeNull();
        expect(screen.getByRole('link', { name: 'Studio' }).getAttribute('aria-current')).toBe('page');
        expect(screen.getByRole('link', { name: 'History' })).not.toBeNull();
        expect(screen.getByText('Studio content')).not.toBeNull();
        expect(capturedLayoutConfig).toMatchObject({
            breadcrumbs: [{ label: 'Shared Policy Groups', href: '/shared-policy-groups' }, { label: 'Auth Bundle' }],
        });
    });

    it('builds route links correctly when the Shared Policy Group id is encoded', async () => {
        mockUseDetail.mockReturnValue({ data: SPG, isLoading: false, isError: false } as never);
        renderLayout('/shared-policy-groups/shared%20policy/studio');

        const overviewLink = screen.getByRole('link', { name: 'Overview' });
        expect((overviewLink as HTMLAnchorElement).pathname).toBe('/shared-policy-groups/shared%20policy/overview');
        await userEvent.setup().click(overviewLink);

        expect(screen.getByText('Overview content')).not.toBeNull();
    });

    it('navigates back to the shared policy groups list', () => {
        mockUseDetail.mockReturnValue({ data: SPG, isLoading: false, isError: false } as never);
        renderLayout();
        fireEvent.click(screen.getByRole('button', { name: /Back to Shared Policy Groups/i }));
        expect(mockNavigate).toHaveBeenCalledWith('/shared-policy-groups');
    });

    it('shows not-found state when the detail query fails', () => {
        mockUseDetail.mockReturnValue({ data: undefined, isLoading: false, isError: true } as never);
        renderLayout();
        expect(screen.getByText(/Shared Policy Group not found or failed to load/i)).not.toBeNull();
    });

    it('redirects and hides the not-found state when the detail request is forbidden', () => {
        mockUseDetail.mockReturnValue({
            data: undefined,
            isLoading: false,
            isError: true,
            error: new ApimApiError(403, 'Forbidden'),
        } as never);

        renderLayout();

        expect(mockUseForbiddenResourceRedirect).toHaveBeenCalledWith({
            isForbidden: true,
            permissionPrefix: 'environment-shared_policy_group-',
            redirectTo: '../../applications',
        });
        expect(screen.queryByText(/Shared Policy Group not found or failed to load/i)).toBeNull();
    });

    it('shows a loading skeleton while fetching', () => {
        mockUseDetail.mockReturnValue({ data: undefined, isLoading: true, isError: false } as never);
        renderLayout();
        expect(screen.queryByRole('heading', { name: 'Auth Bundle' })).toBeNull();
        expect(screen.queryByTestId('shared-policy-group-detail')).toBeNull();
    });

    it('resets tab state when navigating to a different Shared Policy Group', async () => {
        mockUseDetail.mockImplementation(
            sharedPolicyGroupId =>
                ({
                    data: { ...SPG, id: sharedPolicyGroupId ?? '', name: sharedPolicyGroupId ?? '' },
                    isLoading: false,
                    isError: false,
                }) as never,
        );
        const router = createMemoryRouter(
            [
                {
                    path: '/shared-policy-groups/:sharedPolicyGroupId',
                    element: <SharedPolicyGroupDetailLayout />,
                    children: [{ path: 'overview', element: <StatefulOutlet /> }],
                },
            ],
            { initialEntries: ['/shared-policy-groups/spg-1/overview'] },
        );
        render(<RouterProvider router={router} />);
        fireEvent.click(screen.getByRole('button', { name: 'Mark dirty' }));
        expect(screen.getByText('Dirty')).not.toBeNull();

        await act(() => router.navigate('/shared-policy-groups/spg-2/overview'));

        expect(screen.getByText('Clean')).not.toBeNull();
    });
});
