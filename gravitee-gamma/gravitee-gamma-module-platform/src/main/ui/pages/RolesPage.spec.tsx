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
const mockNavigate = jest.fn();

jest.mock('react-router-dom', () => ({
    ...jest.requireActual('react-router-dom'),
    useNavigate: () => mockNavigate,
}));
jest.mock('@gravitee/gamma-modules-sdk', () => ({
    useHasFeature: jest.fn(),
    useHasPermission: jest.fn(),
}));
jest.mock('../features/roles/hooks/useRoles');
jest.mock('../features/roles/hooks/useRoleMutations');
jest.mock('../shared/notify', () => ({
    notify: { success: jest.fn(), error: jest.fn() },
}));

import { useHasFeature, useHasPermission } from '@gravitee/gamma-modules-sdk';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';

import { RolesPage } from './RolesPage';
import { useDeleteRole } from '../features/roles/hooks/useRoleMutations';
import { useRolesByScope } from '../features/roles/hooks/useRoles';
import { notify } from '../shared/notify';

const mockUseHasFeature = jest.mocked(useHasFeature);
const mockUseHasPermission = jest.mocked(useHasPermission);
const mockUseRolesByScope = jest.mocked(useRolesByScope);
const mockUseDeleteRole = jest.mocked(useDeleteRole);

// eslint-disable-next-line @typescript-eslint/no-explicit-any
function makeMutation(mutateAsync = jest.fn()): any {
    return { mutateAsync, isPending: false };
}

function renderPage() {
    return render(
        <MemoryRouter initialEntries={['/roles']}>
            <RolesPage />
        </MemoryRouter>,
    );
}

describe('RolesPage', () => {
    beforeEach(() => {
        mockNavigate.mockClear();
        mockUseHasFeature.mockReturnValue(true);
        mockUseHasPermission.mockReturnValue(true);
        mockUseDeleteRole.mockReturnValue(makeMutation());
        mockUseRolesByScope.mockReturnValue({
            isLoading: false,
            groups: [
                {
                    scope: 'ORGANIZATION',
                    label: 'Organization',
                    isLoading: false,
                    roles: [
                        { name: 'ADMIN', scope: 'ORGANIZATION', system: true, permissions: {} },
                        { name: 'CUSTOM', scope: 'ORGANIZATION', permissions: {} },
                    ],
                },
                { scope: 'API', label: 'API', isLoading: false, roles: [{ name: 'USER', scope: 'API', permissions: {} }] },
            ],
        });
    });

    afterEach(() => {
        jest.clearAllMocks();
    });

    it('renders the page title and every scope section', () => {
        renderPage();

        expect(screen.getByRole('heading', { name: 'Roles' })).toBeInTheDocument();
        // Each scope label appears twice: once as the card title, once as a table-of-contents link.
        expect(screen.getAllByText('Organization')).toHaveLength(2);
        expect(screen.getAllByText('API')).toHaveLength(2);
    });

    it('renders a table-of-contents link for every scope', () => {
        renderPage();

        const toc = screen.getByRole('navigation', { name: 'Roles sections' });
        expect(within(toc).getByRole('link', { name: 'Organization' })).toBeInTheDocument();
        expect(within(toc).getByRole('link', { name: 'API' })).toBeInTheDocument();
    });

    it('navigates to the create route when adding a role while licensed', async () => {
        const user = userEvent.setup();
        renderPage();

        await user.click(screen.getAllByRole('button', { name: /Add a role/ })[0]);

        expect(mockNavigate).toHaveBeenCalledWith('ORGANIZATION');
    });

    it('opens the license dialog instead of navigating when unlicensed', async () => {
        mockUseHasFeature.mockReturnValue(false);
        const user = userEvent.setup();
        renderPage();

        await user.click(screen.getAllByRole('button', { name: /Add a role/ })[0]);

        expect(mockNavigate).not.toHaveBeenCalled();
        expect(screen.getByText('Custom Roles')).toBeInTheDocument();
    });

    it('navigates to the edit route when a role is selected', async () => {
        const user = userEvent.setup();
        renderPage();

        await user.click(screen.getByRole('button', { name: 'CUSTOM' }));

        expect(mockNavigate).toHaveBeenCalledWith('ORGANIZATION/CUSTOM');
    });

    it('navigates to the members route for an ORGANIZATION-scope role', async () => {
        const user = userEvent.setup();
        renderPage();

        // ADMIN only has "see members", but it's still behind the row's "..." dropdown.
        await user.click(screen.getByRole('button', { name: 'Actions for ADMIN' }));
        await user.click(screen.getByRole('menuitem', { name: /See members/ }));

        expect(mockNavigate).toHaveBeenCalledWith('ORGANIZATION/ADMIN/members');
    });

    it('deletes a role and shows a success toast', async () => {
        const mutateAsync = jest.fn().mockResolvedValue(undefined);
        mockUseDeleteRole.mockReturnValue(makeMutation(mutateAsync));
        const user = userEvent.setup();
        renderPage();

        // CUSTOM has both "see members" and "delete", so its row actions collapse into a dropdown.
        await user.click(screen.getByRole('button', { name: 'Actions for CUSTOM' }));
        await user.click(screen.getByRole('menuitem', { name: /Delete role/ }));
        expect(screen.getByRole('heading', { name: 'Delete a Role' })).toBeInTheDocument();
        await user.click(screen.getByRole('button', { name: 'Delete' }));

        await waitFor(() => {
            expect(mutateAsync).toHaveBeenCalledWith({ scope: 'ORGANIZATION', name: 'CUSTOM' });
            expect(notify.success).toHaveBeenCalledWith('Role CUSTOM successfully deleted!');
        });
    });

    it('shows an error toast when delete fails', async () => {
        const error = new Error('delete failed');
        mockUseDeleteRole.mockReturnValue(makeMutation(jest.fn().mockRejectedValue(error)));
        const user = userEvent.setup();
        renderPage();

        await user.click(screen.getByRole('button', { name: 'Actions for CUSTOM' }));
        await user.click(screen.getByRole('menuitem', { name: /Delete role/ }));
        await user.click(screen.getByRole('button', { name: 'Delete' }));

        await waitFor(() => {
            expect(notify.error).toHaveBeenCalledWith(error, 'Failed to delete Role CUSTOM');
        });
    });
});
