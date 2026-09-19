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
import { fireEvent, render, screen, waitFor } from '@testing-library/react';

import { useHasPermission } from '@gravitee/gamma-modules-sdk';

import { UserFieldsPage } from './UserFieldsPage';
import { useCreateUserField, useDeleteUserField, useUpdateUserField } from '../features/user-fields/hooks/useUserFieldMutations';
import { useUserFields } from '../features/user-fields/hooks/useUserFields';
import type { UserField } from '../features/user-fields/types/userField';
import { notify } from '../shared/notify';

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    useHasPermission: jest.fn(),
}));
jest.mock('../features/user-fields/hooks/useUserFields');
jest.mock('../features/user-fields/hooks/useUserFieldMutations');
jest.mock('../shared/notify', () => ({
    notify: { success: jest.fn(), error: jest.fn() },
}));

// Stub the table to avoid Radix dropdown pointer-event complexity in jsdom; it exposes the
// page's edit/delete callbacks as plain buttons.
jest.mock('../features/user-fields/components/UserFieldsTable', () => ({
    UserFieldsTable: ({
        fields,
        canEdit,
        canDelete,
        onEdit,
        onDelete,
    }: {
        fields: UserField[];
        canEdit: boolean;
        canDelete: boolean;
        onEdit: (field: UserField) => void;
        onDelete: (field: UserField) => void;
    }) => (
        <div data-testid="user-fields-table">
            {fields.map(field => (
                <div key={field.key}>
                    <span>{field.label}</span>
                    {canEdit && (
                        <button type="button" onClick={() => onEdit(field)}>
                            Edit {field.key}
                        </button>
                    )}
                    {canDelete && (
                        <button type="button" onClick={() => onDelete(field)}>
                            Delete {field.key}
                        </button>
                    )}
                </div>
            ))}
        </div>
    ),
}));

// Radix Switch measures its thumb via ResizeObserver, which jsdom does not implement.
beforeAll(() => {
    global.ResizeObserver = class ResizeObserver {
        observe() {}
        unobserve() {}
        disconnect() {}
    } as typeof ResizeObserver;
});

const mockUseHasPermission = jest.mocked(useHasPermission);
const mockUseUserFields = jest.mocked(useUserFields);
const mockUseCreateUserField = jest.mocked(useCreateUserField);
const mockUseUpdateUserField = jest.mocked(useUpdateUserField);
const mockUseDeleteUserField = jest.mocked(useDeleteUserField);

const FIELDS: UserField[] = [
    { key: 'department', label: 'Department', required: true, values: ['Engineering'] },
    { key: 'country', label: 'Country', required: false, values: [] },
];

function makeQueryResult(overrides: Partial<ReturnType<typeof useUserFields>> = {}): ReturnType<typeof useUserFields> {
    return {
        data: FIELDS,
        isLoading: false,
        isError: false,
        refetch: jest.fn(),
        ...overrides,
    } as ReturnType<typeof useUserFields>;
}

// eslint-disable-next-line @typescript-eslint/no-explicit-any
function makeMutation(mutateAsync = jest.fn()): any {
    return { mutateAsync, isPending: false };
}

function grantOnly(...permissions: string[]) {
    mockUseHasPermission.mockImplementation(({ anyOf }) => (anyOf ?? []).some(permission => permissions.includes(permission)));
}

describe('UserFieldsPage', () => {
    beforeEach(() => {
        mockUseHasPermission.mockReturnValue(true);
        mockUseUserFields.mockReturnValue(makeQueryResult());
        mockUseCreateUserField.mockReturnValue(makeMutation());
        mockUseUpdateUserField.mockReturnValue(makeMutation());
        mockUseDeleteUserField.mockReturnValue(makeMutation());
    });

    afterEach(() => {
        jest.clearAllMocks();
    });

    describe('header', () => {
        it('renders the title and the Add custom field button for a creator', () => {
            render(<UserFieldsPage />);
            expect(screen.getByRole('heading', { name: 'User Fields' })).not.toBeNull();
            expect(screen.getByRole('button', { name: /Add custom field/ })).not.toBeNull();
        });

        it('hides the Add custom field button without organization-custom_user_fields-c', () => {
            grantOnly('organization-custom_user_fields-u', 'organization-custom_user_fields-d');
            render(<UserFieldsPage />);
            expect(screen.queryByRole('button', { name: /Add custom field/ })).toBeNull();
        });
    });

    describe('loading, error and empty states', () => {
        it('shows skeletons while loading', () => {
            mockUseUserFields.mockReturnValue(makeQueryResult({ data: undefined, isLoading: true }));
            const { container } = render(<UserFieldsPage />);
            expect(container.querySelectorAll('[data-slot="skeleton"]').length).toBeGreaterThan(0);
        });

        it('shows an error with a retry action when the list fails', () => {
            const refetch = jest.fn();
            mockUseUserFields.mockReturnValue(makeQueryResult({ data: undefined, isError: true, refetch }));
            render(<UserFieldsPage />);

            expect(screen.getByText('Could not load user fields.')).not.toBeNull();
            fireEvent.click(screen.getByRole('button', { name: 'Try again' }));
            expect(refetch).toHaveBeenCalledTimes(1);
        });

        it('shows the empty landing instead of the table when no field exists', () => {
            mockUseUserFields.mockReturnValue(makeQueryResult({ data: [] }));
            render(<UserFieldsPage />);

            expect(screen.getByRole('heading', { name: 'Why create a user field?' })).not.toBeNull();
            expect(screen.queryByTestId('user-fields-table')).toBeNull();
        });
    });

    describe('table', () => {
        it('lists the fields', () => {
            render(<UserFieldsPage />);
            expect(screen.getByText('Department')).not.toBeNull();
            expect(screen.getByText('Country')).not.toBeNull();
        });

        it('passes edit and delete permissions down', () => {
            grantOnly('organization-custom_user_fields-d');
            render(<UserFieldsPage />);
            expect(screen.queryByRole('button', { name: 'Edit department' })).toBeNull();
            expect(screen.getByRole('button', { name: 'Delete department' })).not.toBeNull();
        });
    });

    describe('create', () => {
        it('opens the create sheet and toasts on success', async () => {
            const mutateAsync = jest.fn().mockResolvedValue({ key: 'job_position', label: 'Job position', required: false, values: [] });
            mockUseCreateUserField.mockReturnValue(makeMutation(mutateAsync));
            render(<UserFieldsPage />);

            fireEvent.click(screen.getByRole('button', { name: /Add custom field/ }));
            expect(screen.getByRole('heading', { name: 'Create user field' })).not.toBeNull();
            fireEvent.change(screen.getByLabelText(/^Key/), { target: { value: 'Job_Position' } });
            fireEvent.change(screen.getByLabelText(/^Label/), { target: { value: 'Job position' } });
            fireEvent.click(screen.getByRole('button', { name: 'Create field' }));

            await waitFor(() => {
                expect(mutateAsync).toHaveBeenCalledWith({ key: 'job_position', label: 'Job position', required: false, values: [] });
                expect(notify.success).toHaveBeenCalledWith('Field job_position created successfully');
            });
            expect(screen.queryByRole('heading', { name: 'Create user field' })).toBeNull();
        });

        it('keeps the sheet open and toasts the server message when creation fails', async () => {
            const error = new Error('CustomUserField [department] already exists.');
            mockUseCreateUserField.mockReturnValue(makeMutation(jest.fn().mockRejectedValue(error)));
            render(<UserFieldsPage />);

            fireEvent.click(screen.getByRole('button', { name: /Add custom field/ }));
            fireEvent.change(screen.getByLabelText(/^Key/), { target: { value: 'department' } });
            fireEvent.change(screen.getByLabelText(/^Label/), { target: { value: 'Department' } });
            fireEvent.click(screen.getByRole('button', { name: 'Create field' }));

            await waitFor(() => expect(notify.error).toHaveBeenCalledWith(error, 'Error during field creation!'));
            expect(screen.getByRole('heading', { name: 'Create user field' })).not.toBeNull();
        });
    });

    describe('edit', () => {
        it('opens the edit sheet pre-filled and toasts on success', async () => {
            const mutateAsync = jest.fn().mockResolvedValue(FIELDS[0]);
            mockUseUpdateUserField.mockReturnValue(makeMutation(mutateAsync));
            render(<UserFieldsPage />);

            fireEvent.click(screen.getByRole('button', { name: 'Edit department' }));
            expect(screen.getByRole('heading', { name: 'Update user field' })).not.toBeNull();
            expect((screen.getByLabelText(/^Key/) as HTMLInputElement).value).toBe('department');
            fireEvent.change(screen.getByLabelText(/^Label/), { target: { value: 'Team' } });
            fireEvent.click(screen.getByRole('button', { name: 'Save changes' }));

            await waitFor(() => {
                expect(mutateAsync).toHaveBeenCalledWith({ key: 'department', label: 'Team', required: true, values: ['Engineering'] });
                expect(notify.success).toHaveBeenCalledWith('Field department updated successfully');
            });
        });

        it('toasts the fallback message when the update fails', async () => {
            const error = new Error('boom');
            mockUseUpdateUserField.mockReturnValue(makeMutation(jest.fn().mockRejectedValue(error)));
            render(<UserFieldsPage />);

            fireEvent.click(screen.getByRole('button', { name: 'Edit department' }));
            fireEvent.change(screen.getByLabelText(/^Label/), { target: { value: 'Team' } });
            fireEvent.click(screen.getByRole('button', { name: 'Save changes' }));

            await waitFor(() => expect(notify.error).toHaveBeenCalledWith(error, 'Error during field update!'));
        });
    });

    describe('delete', () => {
        it('confirms, deletes by key and toasts on success', async () => {
            const mutateAsync = jest.fn().mockResolvedValue(undefined);
            mockUseDeleteUserField.mockReturnValue(makeMutation(mutateAsync));
            render(<UserFieldsPage />);

            fireEvent.click(screen.getByRole('button', { name: 'Delete country' }));
            expect(screen.getByText('Delete custom user field')).not.toBeNull();
            fireEvent.click(screen.getByRole('button', { name: 'Delete' }));

            await waitFor(() => {
                expect(mutateAsync).toHaveBeenCalledWith('country');
                expect(notify.success).toHaveBeenCalledWith('Field country deleted successfully');
            });
            expect(screen.queryByText('Delete custom user field')).toBeNull();
        });

        it('toasts the fallback message when the deletion fails', async () => {
            const error = new Error('boom');
            mockUseDeleteUserField.mockReturnValue(makeMutation(jest.fn().mockRejectedValue(error)));
            render(<UserFieldsPage />);

            fireEvent.click(screen.getByRole('button', { name: 'Delete country' }));
            fireEvent.click(screen.getByRole('button', { name: 'Delete' }));

            await waitFor(() => expect(notify.error).toHaveBeenCalledWith(error, 'Error during field deletion!'));
        });
    });
});
