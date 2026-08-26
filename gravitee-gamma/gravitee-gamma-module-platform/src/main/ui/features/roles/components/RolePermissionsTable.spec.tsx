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

import { RolePermissionsTable } from './RolePermissionsTable';
import type { Role } from '../types/role';
import { toFormPermissions } from '../utils/rolePermissions';

describe('RolePermissionsTable', () => {
    it('renders one row per permission with C/R/U/D checkboxes', () => {
        const value = toFormPermissions(undefined, ['DEFINITION', 'MEMBER']);
        render(<RolePermissionsTable scope="API" permissionNames={['DEFINITION', 'MEMBER']} value={value} onChange={jest.fn()} />);

        expect(screen.getByText('DEFINITION')).toBeInTheDocument();
        expect(screen.getByText('MEMBER')).toBeInTheDocument();
        expect(screen.getByRole('checkbox', { name: 'Create permission for DEFINITION' })).not.toBeChecked();
    });

    it('shows "No permission" instead of a table when there are none', () => {
        render(<RolePermissionsTable scope="API" permissionNames={[]} value={{}} onChange={jest.fn()} />);

        expect(screen.getByText('No permissions can be managed for this scope yet.')).toBeInTheDocument();
        expect(screen.queryByRole('table')).not.toBeInTheDocument();
    });

    it('toggles a single cell without touching other rights or rows', async () => {
        const user = userEvent.setup();
        const value = toFormPermissions(undefined, ['DEFINITION', 'MEMBER']);
        const onChange = jest.fn();
        render(<RolePermissionsTable scope="API" permissionNames={['DEFINITION', 'MEMBER']} value={value} onChange={onChange} />);

        await user.click(screen.getByRole('checkbox', { name: 'Create permission for DEFINITION' }));

        expect(onChange).toHaveBeenCalledWith({
            DEFINITION: { C: true, R: false, U: false, D: false },
            MEMBER: { C: false, R: false, U: false, D: false },
        });
    });

    it('select-all header checkbox checks every row for that right', async () => {
        const user = userEvent.setup();
        const value = toFormPermissions(undefined, ['DEFINITION', 'MEMBER']);
        const onChange = jest.fn();
        render(<RolePermissionsTable scope="API" permissionNames={['DEFINITION', 'MEMBER']} value={value} onChange={onChange} />);

        await user.click(screen.getByRole('checkbox', { name: 'Select all Read' }));

        expect(onChange).toHaveBeenCalledWith({
            DEFINITION: { C: false, R: true, U: false, D: false },
            MEMBER: { C: false, R: true, U: false, D: false },
        });
    });

    it('shows the select-all checkbox as indeterminate when only some rows are checked', () => {
        const role: Role = { name: 'CUSTOM', scope: 'API', permissions: { DEFINITION: ['R'] } };
        const value = toFormPermissions(role, ['DEFINITION', 'MEMBER']);
        render(<RolePermissionsTable scope="API" permissionNames={['DEFINITION', 'MEMBER']} value={value} onChange={jest.fn()} />);

        expect(screen.getByRole('checkbox', { name: /all Read/ })).toHaveAttribute('data-state', 'indeterminate');
    });

    it('disables every checkbox when the table is read-only', () => {
        const value = toFormPermissions(undefined, ['DEFINITION']);
        render(<RolePermissionsTable scope="API" permissionNames={['DEFINITION']} value={value} onChange={jest.fn()} disabled />);

        screen.getAllByRole('checkbox').forEach(checkbox => expect(checkbox).toBeDisabled());
    });

    it('disables and annotates a permission that moved to the ORGANIZATION scope under ENVIRONMENT', () => {
        const value = toFormPermissions(undefined, ['TAG', 'GROUP']);
        render(<RolePermissionsTable scope="ENVIRONMENT" permissionNames={['TAG', 'GROUP']} value={value} onChange={jest.fn()} />);

        expect(screen.getByText('This permission has been moved to ORGANIZATION scope')).toBeInTheDocument();
        expect(screen.getByRole('checkbox', { name: 'Create permission for TAG' })).toBeDisabled();
        expect(screen.getByRole('checkbox', { name: 'Create permission for GROUP' })).toBeEnabled();
    });

    it('select-all skips moved-to-organization-scope rows when toggling', async () => {
        const user = userEvent.setup();
        const value = toFormPermissions(undefined, ['TAG', 'GROUP']);
        const onChange = jest.fn();
        render(<RolePermissionsTable scope="ENVIRONMENT" permissionNames={['TAG', 'GROUP']} value={value} onChange={onChange} />);

        await user.click(screen.getByRole('checkbox', { name: 'Select all Create' }));

        expect(onChange).toHaveBeenCalledWith({
            TAG: { C: false, R: false, U: false, D: false },
            GROUP: { C: true, R: false, U: false, D: false },
        });
    });

    it('reads the select-all header as fully checked once every manageable row is checked, ignoring moved-to-organization-scope rows', () => {
        // TAG is moved to ORGANIZATION scope under ENVIRONMENT and can never be checked here — only GROUP is
        // manageable, so checking GROUP's Create right is "all selected", not "indeterminate".
        const role: Role = { name: 'CUSTOM', scope: 'ENVIRONMENT', permissions: { GROUP: ['C'] } };
        const value = toFormPermissions(role, ['TAG', 'GROUP']);
        render(<RolePermissionsTable scope="ENVIRONMENT" permissionNames={['TAG', 'GROUP']} value={value} onChange={jest.fn()} />);

        expect(screen.getByRole('checkbox', { name: 'Deselect all Create' })).toHaveAttribute('data-state', 'checked');
    });
});
