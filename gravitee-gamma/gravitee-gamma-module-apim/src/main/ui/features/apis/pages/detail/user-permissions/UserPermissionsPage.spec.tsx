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
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    ...jest.requireActual<object>('@gravitee/gamma-modules-sdk'),
    useEnvironment: jest.fn(() => ({ id: 'DEFAULT' })),
}));

jest.mock('@gravitee/graphene-core/icons', () => new Proxy({}, { get: () => () => null }));

jest.mock('../../../../../shared/notify', () => ({ notify: { success: jest.fn(), error: jest.fn() } }));

jest.mock('../../../services/members', () => ({
    addApiMember: jest.fn(),
    deleteApiMember: jest.fn(),
    transferApiOwnership: jest.fn(),
    updateApiGroups: jest.fn(),
    updateApiMember: jest.fn(),
    updateApiNotifications: jest.fn(),
}));

jest.mock('../../../context/ApiDetailContext', () => ({ useApiDetailContext: jest.fn() }));
jest.mock('../../../hooks/useApiMembers', () => ({ useApiMembers: jest.fn(() => ({ data: { data: [] }, isLoading: false })) }));
jest.mock('../../../hooks/useApiGroupMembers', () => ({ useApiGroupMembers: jest.fn(() => ({ data: {}, isLoading: false })) }));
jest.mock('../../../hooks/useApiRoles', () => ({ useApiRoles: jest.fn(() => ({ data: [] })) }));
jest.mock('../../../hooks/useGroups', () => ({ useGroups: jest.fn() }));

import { UserPermissionsPage } from './UserPermissionsPage';
import { useApiDetailContext } from '../../../context/ApiDetailContext';
import { useGroups } from '../../../hooks/useGroups';
import { updateApiGroups } from '../../../services/members';
import { apiDetailKeys, apiMemberKeys } from '../../../utils/queryKeys';

// jsdom ships no ResizeObserver, and the checkbox indicator mounts one when a group gets ticked.
globalThis.ResizeObserver ??= class {
    observe() {}
    unobserve() {}
    disconnect() {}
};

const mockUseApiDetailContext = useApiDetailContext as jest.Mock;
const mockUseGroups = useGroups as jest.Mock;
const mockUpdateApiGroups = updateApiGroups as jest.Mock;

const ENV_GROUPS = [
    { id: 'group-a', name: 'Partners' },
    { id: 'group-b', name: 'Internal' },
];

const FEDERATED_API = { id: 'api-1', name: 'Federated Orders API', definitionVersion: 'FEDERATED', groups: ['group-a'] };
const V4_API = { id: 'api-1', name: 'Orders API', definitionVersion: 'V4', groups: ['group-a'] };

function renderUserPermissionsPage(api: object) {
    mockUseApiDetailContext.mockReturnValue({ api, isLoading: false, permissionsReady: true });
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } });
    const invalidateQueries = jest.spyOn(queryClient, 'invalidateQueries');
    render(
        <QueryClientProvider client={queryClient}>
            <MemoryRouter initialEntries={['/apis/api-1/user-permissions']}>
                <Routes>
                    <Route path="apis/:apiId/user-permissions" element={<UserPermissionsPage />} />
                </Routes>
            </MemoryRouter>
        </QueryClientProvider>,
    );
    return { invalidateQueries };
}

async function openManageGroups(user: ReturnType<typeof userEvent.setup>) {
    await user.click(screen.getByRole('button', { name: /manage groups/i }));
    await screen.findByRole('checkbox', { name: /Partners/ });
}

describe.each([
    ['a federated API', FEDERATED_API],
    ['a natively managed API', V4_API],
])('UserPermissionsPage group management on %s', (_label, api) => {
    beforeEach(() => {
        jest.clearAllMocks();
        mockUseGroups.mockReturnValue({ data: { data: ENV_GROUPS } });
        mockUpdateApiGroups.mockResolvedValue(undefined);
    });

    it('offers Manage groups listing the environment groups with the API current ones checked', async () => {
        const user = userEvent.setup();
        renderUserPermissionsPage(api);

        await openManageGroups(user);

        expect(screen.getByRole('checkbox', { name: /Partners/ })).toBeChecked();
        expect(screen.getByRole('checkbox', { name: /Internal/ })).not.toBeChecked();
    });

    it('saves the selection through the groups endpoint and refreshes the group and API detail queries', async () => {
        const user = userEvent.setup();
        const { invalidateQueries } = renderUserPermissionsPage(api);

        await openManageGroups(user);
        await user.click(screen.getByRole('checkbox', { name: /Internal/ }));
        await user.click(screen.getByRole('button', { name: 'Save' }));

        await waitFor(() => expect(mockUpdateApiGroups).toHaveBeenCalledWith('DEFAULT', 'api-1', ['group-a', 'group-b']));
        await waitFor(() => expect(invalidateQueries).toHaveBeenCalledWith({ queryKey: apiMemberKeys.groups('DEFAULT', 'api-1') }));
        expect(invalidateQueries).toHaveBeenCalledWith({ queryKey: apiDetailKeys.detail('DEFAULT', 'api-1') });
    });
});
