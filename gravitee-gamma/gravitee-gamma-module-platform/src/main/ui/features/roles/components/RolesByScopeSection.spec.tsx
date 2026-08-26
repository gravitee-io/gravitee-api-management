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
import userEvent from '@testing-library/user-event';

import { RolesByScopeSection } from './RolesByScopeSection';
import type { RolesByScopeGroup } from '../hooks/useRoles';
import { roleSectionId } from '../utils/roleSectionId';

const BASE_PROPS = {
    canCreate: true,
    canDelete: true,
    canManageMembers: true,
    hasCustomRolesLicense: true,
    onCreateRole: jest.fn(),
    onSelectRole: jest.fn(),
    onDeleteRole: jest.fn(),
    onViewMembers: jest.fn(),
    onShowLicenseDialog: jest.fn(),
};

function organizationGroup(overrides: Partial<RolesByScopeGroup> = {}): RolesByScopeGroup {
    return {
        scope: 'ORGANIZATION',
        label: 'Organization',
        isLoading: false,
        isError: false,
        roles: [
            { name: 'ADMIN', scope: 'ORGANIZATION', system: true, permissions: {} },
            { name: 'CUSTOM', scope: 'ORGANIZATION', description: 'A custom role', permissions: {} },
        ],
        ...overrides,
    };
}

describe('RolesByScopeSection', () => {
    beforeEach(() => {
        jest.clearAllMocks();
    });

    it('renders the scope label, role names, description, and System/Default badges', () => {
        render(
            <RolesByScopeSection
                group={organizationGroup({
                    roles: [{ name: 'ADMIN', scope: 'ORGANIZATION', system: true, default: true, permissions: {} }],
                })}
                {...BASE_PROPS}
            />,
        );

        expect(screen.getByText('Organization')).toBeInTheDocument();
        expect(screen.getByText('ADMIN')).toBeInTheDocument();
        expect(screen.getByText('System')).toBeInTheDocument();
        expect(screen.getByText('Default')).toBeInTheDocument();
    });

    it('exposes an anchor id matching the scope, for the table-of-contents to jump to', () => {
        const { container } = render(<RolesByScopeSection group={organizationGroup()} {...BASE_PROPS} />);

        expect(container.querySelector(`#${roleSectionId('ORGANIZATION')}`)).toBeInTheDocument();
    });

    it('truncates a long role name instead of overflowing the row and clipping its action button', () => {
        const longName =
            'API_ROLE_WITH_LONG_NAME_TO_TEST_THE_UI_WITH_LONG_NAMEAPI_ROLE_WITH_LONG_NAME_TO_TEST_THE_UI_WITH_LONG_NAMEAPI_ROLE_WITH_LONG_NAME';
        render(
            <RolesByScopeSection
                group={{
                    scope: 'API',
                    label: 'API',
                    isLoading: false,
                    isError: false,
                    roles: [{ name: longName, scope: 'API', permissions: {} }],
                }}
                {...BASE_PROPS}
            />,
        );

        const nameEl = screen.getByText(longName);
        expect(nameEl).toHaveClass('truncate');
        // The row must stay a single non-wrapping line with a bounded width, or `truncate` has nothing to clip against.
        expect(nameEl.parentElement).toHaveClass('min-w-0');
        expect(nameEl.parentElement).not.toHaveClass('flex-wrap');
        // The action button's column must not shrink so it stays fully visible next to a truncated name.
        expect(screen.getByRole('button', { name: 'Button to delete a role' }).parentElement).toHaveClass('shrink-0');
    });

    it('shows a skeleton while loading and "No role" once loaded with none', () => {
        const { rerender } = render(<RolesByScopeSection group={organizationGroup({ isLoading: true, roles: [] })} {...BASE_PROPS} />);
        expect(screen.queryByText('No role')).not.toBeInTheDocument();

        rerender(<RolesByScopeSection group={organizationGroup({ isLoading: false, roles: [] })} {...BASE_PROPS} />);
        expect(screen.getByText('No role')).toBeInTheDocument();
    });

    it('shows an error instead of "No role" when the scope failed to load', () => {
        render(<RolesByScopeSection group={organizationGroup({ isError: true, roles: [] })} {...BASE_PROPS} />);

        expect(screen.getByText('Failed to load roles for this scope. Please refresh and try again.')).toBeInTheDocument();
        expect(screen.queryByText('No role')).not.toBeInTheDocument();
    });

    it('selecting a role name calls onSelectRole with its scope and name', async () => {
        const user = userEvent.setup();
        render(<RolesByScopeSection group={organizationGroup()} {...BASE_PROPS} />);

        await user.click(screen.getByRole('button', { name: /ADMIN/ }));

        expect(BASE_PROPS.onSelectRole).toHaveBeenCalledWith('ORGANIZATION', 'ADMIN');
    });

    it('hides the create button when canCreate is false', () => {
        render(<RolesByScopeSection group={organizationGroup()} {...BASE_PROPS} canCreate={false} />);

        expect(screen.queryByRole('button', { name: /Add a role/ })).not.toBeInTheDocument();
    });

    it('creating a role navigates directly when licensed', async () => {
        const user = userEvent.setup();
        render(<RolesByScopeSection group={organizationGroup()} {...BASE_PROPS} />);

        await user.click(screen.getByRole('button', { name: /Add a role/ }));

        expect(BASE_PROPS.onCreateRole).toHaveBeenCalledWith('ORGANIZATION');
        expect(BASE_PROPS.onShowLicenseDialog).not.toHaveBeenCalled();
    });

    it('creating a role opens the license dialog instead when unlicensed', async () => {
        const user = userEvent.setup();
        render(<RolesByScopeSection group={organizationGroup()} {...BASE_PROPS} hasCustomRolesLicense={false} />);

        await user.click(screen.getByRole('button', { name: /Add a role/ }));

        expect(BASE_PROPS.onShowLicenseDialog).toHaveBeenCalled();
        expect(BASE_PROPS.onCreateRole).not.toHaveBeenCalled();
    });

    it('only offers delete for roles that are neither system nor default', () => {
        render(
            <RolesByScopeSection
                group={organizationGroup({
                    roles: [
                        { name: 'ADMIN', scope: 'ORGANIZATION', system: true, permissions: {} },
                        { name: 'DEFAULT_ROLE', scope: 'ORGANIZATION', default: true, permissions: {} },
                        { name: 'CUSTOM', scope: 'ORGANIZATION', permissions: {} },
                    ],
                })}
                {...BASE_PROPS}
            />,
        );

        // ADMIN and DEFAULT_ROLE have only one available action (see members), rendered as a lone button.
        expect(screen.getAllByRole('button', { name: 'Button to see members with this role' })).toHaveLength(2);
        expect(screen.queryByRole('button', { name: 'Button to delete a role' })).not.toBeInTheDocument();
        // CUSTOM has both actions, which collapses into a single dropdown trigger instead of two icon buttons.
        expect(screen.getByRole('button', { name: 'Actions for CUSTOM' })).toBeInTheDocument();
    });

    it('collapses two available row actions into a dropdown menu instead of two icon buttons', async () => {
        const user = userEvent.setup();
        render(
            <RolesByScopeSection
                group={organizationGroup({ roles: [{ name: 'CUSTOM', scope: 'ORGANIZATION', permissions: {} }] })}
                {...BASE_PROPS}
            />,
        );

        await user.click(screen.getByRole('button', { name: 'Actions for CUSTOM' }));
        await user.click(screen.getByRole('menuitem', { name: /See members/ }));
        expect(BASE_PROPS.onViewMembers).toHaveBeenCalledWith('ORGANIZATION', 'CUSTOM');

        await user.click(screen.getByRole('button', { name: 'Actions for CUSTOM' }));
        await user.click(screen.getByRole('menuitem', { name: /Delete role/ }));
        expect(BASE_PROPS.onDeleteRole).toHaveBeenCalledWith('ORGANIZATION', { name: 'CUSTOM', scope: 'ORGANIZATION', permissions: {} });
    });

    it('only offers "see members" for ORGANIZATION-scope roles', () => {
        render(
            <RolesByScopeSection
                group={{
                    scope: 'API',
                    label: 'API',
                    isLoading: false,
                    isError: false,
                    roles: [{ name: 'USER', scope: 'API', permissions: {} }],
                }}
                {...BASE_PROPS}
            />,
        );

        expect(screen.queryByRole('button', { name: 'Button to see members with this role' })).not.toBeInTheDocument();
        expect(screen.queryByRole('button', { name: /Actions for/ })).not.toBeInTheDocument();
        expect(screen.getByRole('button', { name: 'Button to delete a role' })).toBeInTheDocument();
    });

    it('hides delete and members actions without permission', () => {
        render(<RolesByScopeSection group={organizationGroup()} {...BASE_PROPS} canDelete={false} canManageMembers={false} />);

        expect(screen.queryByRole('button', { name: 'Button to delete a role' })).not.toBeInTheDocument();
        expect(screen.queryByRole('button', { name: 'Button to see members with this role' })).not.toBeInTheDocument();
        expect(screen.queryByRole('button', { name: /Actions for/ })).not.toBeInTheDocument();
    });
});
