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

import { SharedPolicyGroupDetailPage } from './SharedPolicyGroupDetailPage';
import { useSharedPolicyGroupDetail } from '../features/shared-policy-groups/hooks/useSharedPolicyGroups';
import type { SharedPolicyGroup } from '../features/shared-policy-groups/types/sharedPolicyGroup';
import { ApimApiError } from '../shared/api/apimClient';
import { useForbiddenResourceRedirect } from '../shared/hooks/useForbiddenResourceRedirect';

jest.mock('../features/shared-policy-groups/hooks/useSharedPolicyGroups');
jest.mock('../shared/hooks/useForbiddenResourceRedirect');

const mockUseSharedPolicyGroupDetail = jest.mocked(useSharedPolicyGroupDetail);
const mockUseForbiddenResourceRedirect = jest.mocked(useForbiddenResourceRedirect);

const SPG: SharedPolicyGroup = {
    id: 'spg-1',
    name: 'Auth Bundle',
    description: 'Reusable auth policies',
    prerequisiteMessage: 'Requires the "auth-cache" resource',
    lifecycleState: 'DEPLOYED',
    apiType: 'PROXY',
    phase: 'REQUEST',
    updatedAt: '2024-01-01T00:00:00.000Z',
    deployedAt: '2024-01-02T00:00:00.000Z',
};

function renderPage() {
    return render(
        <MemoryRouter initialEntries={['/spg-1']}>
            <Routes>
                <Route path=":sharedPolicyGroupId" element={<SharedPolicyGroupDetailPage />} />
            </Routes>
        </MemoryRouter>,
    );
}

describe('SharedPolicyGroupDetailPage', () => {
    afterEach(() => {
        jest.clearAllMocks();
    });

    it('renders name, status, description, API type, and phase', () => {
        mockUseSharedPolicyGroupDetail.mockReturnValue({
            data: SPG,
            isLoading: false,
            isError: false,
        } as ReturnType<typeof useSharedPolicyGroupDetail>);

        renderPage();

        expect(screen.queryByRole('heading', { name: 'Auth Bundle' })).not.toBeNull();
        expect(screen.queryByText('Deployed')).not.toBeNull();
        expect(screen.queryByText('Reusable auth policies')).not.toBeNull();
        expect(screen.queryByText('Proxy')).not.toBeNull();
        expect(screen.queryByText('Request')).not.toBeNull();
        expect(screen.queryByText('Requires the "auth-cache" resource')).not.toBeNull();
    });

    it('shows a not-found message when the shared policy group fails to load', () => {
        mockUseSharedPolicyGroupDetail.mockReturnValue({
            data: undefined,
            isLoading: false,
            isError: true,
            error: new Error('load failed'),
        } as ReturnType<typeof useSharedPolicyGroupDetail>);

        renderPage();

        expect(screen.queryByText('Shared Policy Group not found or failed to load.')).not.toBeNull();
    });

    it('redirects and removes stale permissions when the detail request is forbidden', () => {
        mockUseSharedPolicyGroupDetail.mockReturnValue({
            data: undefined,
            isLoading: false,
            isError: true,
            error: new ApimApiError(403, 'Forbidden'),
        } as unknown as ReturnType<typeof useSharedPolicyGroupDetail>);

        renderPage();

        expect(mockUseForbiddenResourceRedirect).toHaveBeenCalledWith({
            isForbidden: true,
            permissionPrefix: 'environment-shared_policy_group-',
            redirectTo: '../../applications',
        });
        expect(screen.queryByText('Shared Policy Group not found or failed to load.')).toBeNull();
    });

    it('shows a loading skeleton while fetching', () => {
        mockUseSharedPolicyGroupDetail.mockReturnValue({
            data: undefined,
            isLoading: true,
            isError: false,
        } as ReturnType<typeof useSharedPolicyGroupDetail>);

        renderPage();

        expect(screen.queryByRole('heading', { name: 'Auth Bundle' })).toBeNull();
    });

    it('renders a back link to the list', () => {
        mockUseSharedPolicyGroupDetail.mockReturnValue({
            data: SPG,
            isLoading: false,
            isError: false,
        } as ReturnType<typeof useSharedPolicyGroupDetail>);

        renderPage();

        expect(screen.getByRole('link', { name: /Back to Shared Policy Groups/ }).getAttribute('href')).toBe('/');
    });
});
