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

import { fireEvent, render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';

import { SharedPolicyGroupDetailLayout } from './SharedPolicyGroupDetailLayout';
import { useSharedPolicyGroupDetail } from '../hooks/useSharedPolicyGroups';
import type { SharedPolicyGroup } from '../types/sharedPolicyGroup';
import { ApimApiError } from '../../../shared/api/apimClient';
import { useForbiddenResourceRedirect } from '../../../shared/hooks/useForbiddenResourceRedirect';

const mockNavigate = jest.fn();

jest.mock('react-router-dom', () => ({
    ...jest.requireActual('react-router-dom'),
    useNavigate: () => mockNavigate,
}));

jest.mock('../hooks/useSharedPolicyGroups');
jest.mock('../../../shared/hooks/useForbiddenResourceRedirect');

const mockUseDetail = jest.mocked(useSharedPolicyGroupDetail);
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
                <Route path="/shared-policy-groups/:sharedPolicyGroupId" element={<SharedPolicyGroupDetailLayout />}>
                    <Route path="overview" element={<div>Overview content</div>} />
                    <Route path="studio" element={<div>Studio content</div>} />
                    <Route path="history" element={<div>History content</div>} />
                </Route>
            </Routes>
        </MemoryRouter>,
    );
}

describe('SharedPolicyGroupDetailLayout', () => {
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
        expect(screen.getByTestId('shared-policy-group-tab-overview')).not.toBeNull();
        expect(screen.getByTestId('shared-policy-group-tab-studio')).not.toBeNull();
        expect(screen.getByTestId('shared-policy-group-tab-history')).not.toBeNull();
        expect(screen.getByText('Studio content')).not.toBeNull();
    });

    it('navigates between detail tabs', () => {
        mockUseDetail.mockReturnValue({ data: SPG, isLoading: false, isError: false } as never);
        renderLayout();

        fireEvent.click(screen.getByRole('tab', { name: 'Overview' }));

        expect(mockNavigate).toHaveBeenCalledWith('/shared-policy-groups/spg-1/overview');
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
});
