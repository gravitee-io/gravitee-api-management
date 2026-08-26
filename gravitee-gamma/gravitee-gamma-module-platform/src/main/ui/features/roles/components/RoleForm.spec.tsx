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

import { RoleForm } from './RoleForm';
import { installFormActionTestEnvironment } from '../../../shared/testing/formAction';
import type { Role } from '../types/role';

let restoreTestEnvironment: () => void;

beforeAll(() => {
    restoreTestEnvironment = installFormActionTestEnvironment();
});

afterAll(() => {
    restoreTestEnvironment();
});

describe('RoleForm', () => {
    it('creates a role: submits the trimmed, upper-cased name and the checked permissions', async () => {
        const user = userEvent.setup();
        const onSubmit = jest.fn().mockResolvedValue(undefined);
        render(
            <RoleForm
                scope="API"
                permissionNames={['DEFINITION']}
                isReadOnly={false}
                isSaving={false}
                onSubmit={onSubmit}
                onCancel={jest.fn()}
            />,
        );

        await user.type(screen.getByLabelText('Role name'), '  custom  ');
        await user.click(screen.getByRole('checkbox', { name: 'Create permission for DEFINITION' }));
        await user.click(screen.getByRole('button', { name: 'Create role' }));

        expect(onSubmit).toHaveBeenCalledWith({
            name: 'CUSTOM',
            description: undefined,
            default: false,
            permissions: { DEFINITION: ['C'] },
        });
    });

    it('the name field is disabled once a role already exists (edit mode)', () => {
        const role: Role = { name: 'CUSTOM', scope: 'API', permissions: {} };
        render(
            <RoleForm
                scope="API"
                role={role}
                permissionNames={[]}
                isReadOnly={false}
                isSaving={false}
                onSubmit={jest.fn()}
                onCancel={jest.fn()}
            />,
        );

        expect(screen.getByLabelText('Role name')).toBeDisabled();
        expect(screen.getByRole('button', { name: 'Save' })).toBeInTheDocument();
    });

    it('mirrors gio-save-bar: Save stays disabled in edit mode until a field actually changes', async () => {
        const user = userEvent.setup();
        const role: Role = { name: 'CUSTOM', scope: 'API', description: 'Original', permissions: {} };
        render(
            <RoleForm
                scope="API"
                role={role}
                permissionNames={[]}
                isReadOnly={false}
                isSaving={false}
                onSubmit={jest.fn()}
                onCancel={jest.fn()}
            />,
        );

        expect(screen.getByRole('button', { name: 'Save' })).toBeDisabled();

        await user.type(screen.getByLabelText('Role description'), '!');

        expect(screen.getByRole('button', { name: 'Save' })).toBeEnabled();
    });

    it('disables description and default for a system role even when the permission matrix stays editable', () => {
        const role: Role = { name: 'USER', scope: 'API', system: true, permissions: {} };
        render(
            <RoleForm
                scope="API"
                role={role}
                permissionNames={['DEFINITION']}
                isReadOnly={false}
                isSaving={false}
                onSubmit={jest.fn()}
                onCancel={jest.fn()}
            />,
        );

        expect(screen.getByLabelText('Role description')).toBeDisabled();
        expect(screen.getByLabelText('Default role toggle')).toBeDisabled();
        expect(screen.getByRole('checkbox', { name: 'Create permission for DEFINITION' })).toBeEnabled();
    });

    it('shows the read-only banner and disables the permission matrix and submit when isReadOnly', () => {
        const role: Role = { name: 'ADMIN', scope: 'ORGANIZATION', system: true, permissions: {} };
        render(
            <RoleForm
                scope="ORGANIZATION"
                role={role}
                permissionNames={['SETTINGS']}
                isReadOnly
                isSaving={false}
                onSubmit={jest.fn()}
                onCancel={jest.fn()}
            />,
        );

        expect(screen.getByText('System role are not editable')).toBeInTheDocument();
        expect(screen.getByRole('checkbox', { name: 'Create permission for SETTINGS' })).toBeDisabled();
        expect(screen.getByRole('button', { name: 'Save' })).toBeDisabled();
    });

    it('requires a name before submitting', async () => {
        const user = userEvent.setup();
        const onSubmit = jest.fn();
        render(<RoleForm scope="API" permissionNames={[]} isReadOnly={false} isSaving={false} onSubmit={onSubmit} onCancel={jest.fn()} />);

        expect(screen.getByRole('button', { name: 'Create role' })).toBeDisabled();
        await user.type(screen.getByLabelText('Role name'), 'X');
        expect(screen.getByRole('button', { name: 'Create role' })).toBeEnabled();
        await user.clear(screen.getByLabelText('Role name'));
        expect(screen.getByRole('button', { name: 'Create role' })).toBeDisabled();
        expect(onSubmit).not.toHaveBeenCalled();
    });

    it('does not show "Name is required." on a pristine, untouched create form', () => {
        render(<RoleForm scope="API" permissionNames={[]} isReadOnly={false} isSaving={false} onSubmit={jest.fn()} onCancel={jest.fn()} />);

        expect(screen.queryByText('Name is required.')).not.toBeInTheDocument();
        expect(screen.getByLabelText('Role name')).not.toHaveAttribute('aria-invalid', 'true');
    });

    it('shows "Name is required." once the user has typed into and cleared the name field', async () => {
        const user = userEvent.setup();
        render(<RoleForm scope="API" permissionNames={[]} isReadOnly={false} isSaving={false} onSubmit={jest.fn()} onCancel={jest.fn()} />);

        await user.type(screen.getByLabelText('Role name'), 'X');
        await user.clear(screen.getByLabelText('Role name'));

        expect(screen.getByText('Name is required.')).toBeInTheDocument();
        expect(screen.getByLabelText('Role name')).toHaveAttribute('aria-invalid', 'true');
    });

    it('calls onCancel from the Cancel button', async () => {
        const user = userEvent.setup();
        const onCancel = jest.fn();
        render(<RoleForm scope="API" permissionNames={[]} isReadOnly={false} isSaving={false} onSubmit={jest.fn()} onCancel={onCancel} />);

        await user.click(screen.getByRole('button', { name: 'Cancel' }));

        expect(onCancel).toHaveBeenCalled();
    });
});
