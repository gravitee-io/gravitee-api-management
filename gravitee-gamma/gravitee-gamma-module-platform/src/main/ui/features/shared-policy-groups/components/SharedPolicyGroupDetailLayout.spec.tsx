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
import { useHasEnvironmentPermission } from '../../../shared/hooks/useEnvironmentPermissions';
import { useForbiddenResourceRedirect } from '../../../shared/hooks/useForbiddenResourceRedirect';
import {
    useDeleteSharedPolicyGroup,
    useUndeploySharedPolicyGroup,
    useUpdateSharedPolicyGroup,
} from '../hooks/useSharedPolicyGroupMutations';
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
jest.mock('../../../shared/hooks/useEnvironmentPermissions');

const mockUseDetail = jest.mocked(useSharedPolicyGroupDetail);
const mockUseUpdateSharedPolicyGroup = jest.mocked(useUpdateSharedPolicyGroup);
const mockUseUndeploySharedPolicyGroup = jest.mocked(useUndeploySharedPolicyGroup);
const mockUseDeleteSharedPolicyGroup = jest.mocked(useDeleteSharedPolicyGroup);
const mockUseForbiddenResourceRedirect = jest.mocked(useForbiddenResourceRedirect);
const mockUseHasEnvironmentPermission = jest.mocked(useHasEnvironmentPermission);

const SPG: SharedPolicyGroup = {
    id: 'spg-1',
    name: 'Auth Bundle',
    description: 'Reusable auth policies',
    prerequisiteMessage: 'Configure an identity provider',
    lifecycleState: 'DEPLOYED',
    apiType: 'PROXY',
    phase: 'REQUEST',
    updatedAt: '2026-08-24T10:15:00.000Z',
    deployedAt: '2026-08-25T11:30:00.000Z',
};

function renderLayout(path = '/shared-policy-groups/spg-1/studio') {
    return render(
        <MemoryRouter initialEntries={[path]}>
            <Routes>
                <Route path="/shared-policy-groups">
                    <Route path=":sharedPolicyGroupId" element={<SharedPolicyGroupDetailLayout />}>
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
        mockUseHasEnvironmentPermission.mockReturnValue(true);
        mockUseUpdateSharedPolicyGroup.mockReturnValue({ mutateAsync: jest.fn() } as never);
        mockUseUndeploySharedPolicyGroup.mockReturnValue({ mutateAsync: jest.fn(), isPending: false } as never);
        mockUseDeleteSharedPolicyGroup.mockReturnValue({ mutateAsync: jest.fn(), isPending: false } as never);
    });

    afterEach(() => {
        jest.clearAllMocks();
    });

    it('renders the header, overflow actions, and studio without tabs', async () => {
        const user = userEvent.setup();
        mockUseDetail.mockReturnValue({ data: SPG, isLoading: false, isError: false } as never);
        renderLayout();

        expect(screen.getByTestId('shared-policy-group-detail')).not.toBeNull();
        expect(screen.getByRole('heading', { name: 'Auth Bundle' })).not.toBeNull();
        expect(screen.getByText('Deployed')).not.toBeNull();
        expect(screen.getByText('Reusable auth policies')).not.toBeNull();
        expect(screen.getByText('Proxy · Request')).not.toBeNull();
        expect(screen.getByText('Configure an identity provider')).not.toBeNull();
        expect(screen.getByText('Last updated')).not.toBeNull();
        expect(screen.getByText('Last deployed')).not.toBeNull();
        expect(screen.queryByRole('navigation', { name: 'Shared Policy Group sections' })).toBeNull();
        await user.click(screen.getByRole('button', { name: 'Shared Policy Group actions' }));
        expect(screen.getByRole('menuitem', { name: 'Edit' })).not.toBeNull();
        expect(screen.getByRole('menuitem', { name: 'Version History' })).not.toBeNull();
        expect(screen.getByText('Studio content')).not.toBeNull();
        expect(capturedLayoutConfig).toMatchObject({
            breadcrumbs: [{ label: 'Shared Policy Groups', href: '/shared-policy-groups' }, { label: 'Auth Bundle' }],
        });
    });

    it('opens version history from the overflow menu', async () => {
        const user = userEvent.setup();
        mockUseDetail.mockReturnValue({ data: SPG, isLoading: false, isError: false } as never);
        renderLayout();

        await user.click(screen.getByRole('button', { name: 'Shared Policy Group actions' }));
        await user.click(screen.getByRole('menuitem', { name: 'Version History' }));

        expect(mockNavigate).toHaveBeenCalledWith('/shared-policy-groups/spg-1/history');
    });

    it('navigates back to the shared policy group list from the studio tab', () => {
        mockUseDetail.mockReturnValue({ data: SPG, isLoading: false, isError: false } as never);
        renderLayout('/shared-policy-groups/spg-1/studio');
        fireEvent.click(screen.getByRole('button', { name: 'Back to Shared Policy Group list' }));
        expect(mockNavigate).toHaveBeenCalledWith('/shared-policy-groups');
    });

    it('navigates back to the studio tab from the history tab', () => {
        mockUseDetail.mockReturnValue({ data: SPG, isLoading: false, isError: false } as never);
        renderLayout('/shared-policy-groups/spg-1/history');
        fireEvent.click(screen.getByRole('button', { name: 'Back to Shared Policy Group' }));
        expect(mockNavigate).toHaveBeenCalledWith('/shared-policy-groups/spg-1/studio');
    });

    it('truncates a long description instead of letting it distort the header, keeping the full text available via title', () => {
        const longDescription = 'A'.repeat(400);
        mockUseDetail.mockReturnValue({ data: { ...SPG, description: longDescription }, isLoading: false, isError: false } as never);
        renderLayout();

        const description = screen.getByTitle(longDescription);
        expect(description.textContent).toBe(longDescription);
        expect(description.className).toContain('truncate');
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

    it('resets outlet state when navigating to a different Shared Policy Group', async () => {
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
                    children: [{ path: 'studio', element: <StatefulOutlet /> }],
                },
            ],
            { initialEntries: ['/shared-policy-groups/spg-1/studio'] },
        );
        render(<RouterProvider router={router} />);
        fireEvent.click(screen.getByRole('button', { name: 'Mark dirty' }));
        expect(screen.getByText('Dirty')).not.toBeNull();

        await act(() => router.navigate('/shared-policy-groups/spg-2/studio'));

        expect(screen.getByText('Clean')).not.toBeNull();
    });
});
