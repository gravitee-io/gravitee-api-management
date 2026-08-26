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
}));
jest.mock('../features/roles/hooks/useRoles');
jest.mock('../features/roles/hooks/useRoleMutations');
jest.mock('../shared/console-settings', () => ({
    useConsoleSettings: jest.fn(),
}));
jest.mock('../shared/notify', () => ({
    notify: { success: jest.fn(), error: jest.fn() },
}));

import { useHasFeature } from '@gravitee/gamma-modules-sdk';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { createMemoryRouter, MemoryRouter, Route, RouterProvider, Routes } from 'react-router-dom';

import { RoleFormPage } from './RoleFormPage';
import { useCreateRole, useUpdateRole } from '../features/roles/hooks/useRoleMutations';
import { usePermissionsByScopes, useRole } from '../features/roles/hooks/useRoles';
import { useConsoleSettings } from '../shared/console-settings';
import { notify } from '../shared/notify';
import { installFormActionTestEnvironment } from '../shared/testing/formAction';

const mockUseHasFeature = jest.mocked(useHasFeature);
const mockUseRole = jest.mocked(useRole);
const mockUsePermissionsByScopes = jest.mocked(usePermissionsByScopes);
const mockUseConsoleSettings = jest.mocked(useConsoleSettings);
const mockUseCreateRole = jest.mocked(useCreateRole);
const mockUseUpdateRole = jest.mocked(useUpdateRole);

let restoreTestEnvironment: () => void;

beforeAll(() => {
    restoreTestEnvironment = installFormActionTestEnvironment();
});

afterAll(() => {
    restoreTestEnvironment();
});

// eslint-disable-next-line @typescript-eslint/no-explicit-any
function makeMutation(mutateAsync = jest.fn()): any {
    return { mutateAsync, isPending: false };
}

function renderPage(path: string) {
    return render(
        <MemoryRouter initialEntries={[path]}>
            <Routes>
                <Route path="roles/:roleScope" element={<RoleFormPage />} />
                <Route path="roles/:roleScope/:roleName" element={<RoleFormPage />} />
            </Routes>
        </MemoryRouter>,
    );
}

describe('RoleFormPage', () => {
    beforeEach(() => {
        mockNavigate.mockClear();
        mockUseHasFeature.mockReturnValue(true);
        mockUsePermissionsByScopes.mockReturnValue({ data: { API: ['DEFINITION', 'PLAN'] }, isLoading: false } as ReturnType<
            typeof usePermissionsByScopes
        >);
        mockUseConsoleSettings.mockReturnValue(null);
        mockUseCreateRole.mockReturnValue(makeMutation());
        mockUseUpdateRole.mockReturnValue(makeMutation());
    });

    afterEach(() => {
        jest.clearAllMocks();
    });

    describe('create mode', () => {
        beforeEach(() => {
            mockUseRole.mockReturnValue({ data: undefined, isLoading: false } as ReturnType<typeof useRole>);
        });

        it('shows the create heading with the permission matrix ready to fill in', () => {
            renderPage('/roles/API');

            expect(screen.getByRole('heading', { name: 'Create role in the API scope' })).toBeInTheDocument();
            expect(screen.getByLabelText('Role name')).toBeEnabled();
        });

        it('redirects to the list when unlicensed', () => {
            mockUseHasFeature.mockReturnValue(false);
            renderPage('/roles/API');

            expect(screen.queryByRole('heading', { name: 'Create role in the API scope' })).not.toBeInTheDocument();
        });

        it('creates the role and navigates to its new edit page', async () => {
            const mutateAsync = jest.fn().mockResolvedValue({ name: 'CUSTOM', scope: 'API', permissions: {} });
            mockUseCreateRole.mockReturnValue(makeMutation(mutateAsync));
            const user = userEvent.setup();
            renderPage('/roles/API');

            await user.type(screen.getByLabelText('Role name'), 'custom');
            await user.click(screen.getByRole('button', { name: 'Create role' }));

            await waitFor(() => {
                expect(mutateAsync).toHaveBeenCalledWith({
                    name: 'CUSTOM',
                    description: undefined,
                    default: false,
                    permissions: { DEFINITION: [], PLAN: [] },
                    scope: 'API',
                    system: false,
                });
                expect(notify.success).toHaveBeenCalledWith('Role successfully saved!');
                expect(mockNavigate).toHaveBeenCalledWith('CUSTOM', { replace: true });
            });
        });

        it('does not interpolate a missing role name into the description', () => {
            renderPage('/roles/API');

            expect(
                screen.getByText('Manage CRUD (Create, Read, Update, Delete) permissions for this role in the API scope.'),
            ).toBeInTheDocument();
        });

        it('treats EXPLORER as having no eligible permissions instead of crashing', () => {
            renderPage('/roles/EXPLORER');

            expect(screen.getByRole('heading', { name: 'Create role in the Explorer scope' })).toBeInTheDocument();
            expect(screen.getByText('No permissions can be managed for this scope yet.')).toBeInTheDocument();
        });

        it('redirects to the list for a scope outside ROLE_SCOPES', () => {
            renderPage('/roles/NOT_A_SCOPE');

            expect(screen.queryByRole('heading', { name: /scope/ })).not.toBeInTheDocument();
        });
    });

    describe('edit mode', () => {
        beforeEach(() => {
            mockUseRole.mockReturnValue({
                data: { name: 'CUSTOM', scope: 'API', description: 'A custom role', permissions: { DEFINITION: ['R'] } },
                isLoading: false,
            } as ReturnType<typeof useRole>);
        });

        it('shows the update heading with the name field locked', () => {
            renderPage('/roles/API/CUSTOM');

            expect(screen.getByRole('heading', { name: 'Update role in the API scope' })).toBeInTheDocument();
            expect(screen.getByLabelText('Role name')).toBeDisabled();
        });

        it('includes the role name in the description', () => {
            renderPage('/roles/API/CUSTOM');

            expect(
                screen.getByText('Manage CRUD (Create, Read, Update, Delete) permissions for this role CUSTOM in the API scope.'),
            ).toBeInTheDocument();
        });

        it('does not redirect for a missing license — only the create route requires it', () => {
            mockUseHasFeature.mockReturnValue(false);
            renderPage('/roles/API/CUSTOM');

            expect(screen.getByRole('heading', { name: 'Update role in the API scope' })).toBeInTheDocument();
        });

        it('fails loudly instead of silently creating a role when the edited role failed to load', async () => {
            mockUseRole.mockReturnValue({ data: undefined, isLoading: false } as ReturnType<typeof useRole>);
            const createMutateAsync = jest.fn();
            const updateMutateAsync = jest.fn();
            mockUseCreateRole.mockReturnValue(makeMutation(createMutateAsync));
            mockUseUpdateRole.mockReturnValue(makeMutation(updateMutateAsync));
            const user = userEvent.setup();
            renderPage('/roles/API/CUSTOM');

            await user.type(screen.getByLabelText('Role name'), 'custom');
            await user.click(screen.getByRole('button', { name: 'Create role' }));

            await waitFor(() => {
                expect(screen.getByText('Role data is unavailable for editing.')).toBeInTheDocument();
            });
            expect(createMutateAsync).not.toHaveBeenCalled();
            expect(updateMutateAsync).not.toHaveBeenCalled();
        });

        it('shows an error instead of falling through to create mode when the role fetch fails', () => {
            mockUseRole.mockReturnValue({ data: undefined, isLoading: false, isError: true } as ReturnType<typeof useRole>);
            renderPage('/roles/API/CUSTOM');

            expect(screen.getByText('Failed to load this role. Please refresh and try again.')).toBeInTheDocument();
            expect(screen.queryByLabelText('Role name')).not.toBeInTheDocument();
        });

        it('saves changes without navigating away', async () => {
            const mutateAsync = jest.fn().mockResolvedValue({ name: 'CUSTOM', scope: 'API', permissions: {} });
            mockUseUpdateRole.mockReturnValue(makeMutation(mutateAsync));
            const user = userEvent.setup();
            renderPage('/roles/API/CUSTOM');

            // Save stays disabled until something actually changes (mirrors gio-save-bar's dirty gating).
            await user.type(screen.getByLabelText('Role description'), '!');
            await user.click(screen.getByRole('button', { name: 'Save' }));

            await waitFor(() => {
                expect(mutateAsync).toHaveBeenCalledWith(
                    expect.objectContaining({ name: 'CUSTOM', scope: 'API', description: 'A custom role!' }),
                );
                expect(notify.success).toHaveBeenCalledWith('Role successfully saved!');
            });
            expect(mockNavigate).not.toHaveBeenCalled();
        });

        it('resets the form when the route param switches to a different role', async () => {
            mockUseRole.mockImplementation(
                (_scope, roleName) =>
                    ({
                        data:
                            roleName === 'SECOND'
                                ? { name: 'SECOND', scope: 'API', description: 'Second role', permissions: {} }
                                : { name: 'FIRST', scope: 'API', description: 'First role', permissions: {} },
                        isLoading: false,
                    }) as ReturnType<typeof useRole>,
            );

            const router = createMemoryRouter([{ path: 'roles/:roleScope/:roleName', element: <RoleFormPage /> }], {
                initialEntries: ['/roles/API/FIRST'],
            });
            render(<RouterProvider router={router} />);

            expect(screen.getByLabelText('Role description')).toHaveValue('First role');

            await router.navigate('/roles/API/SECOND');

            await waitFor(() => {
                expect(screen.getByLabelText('Role description')).toHaveValue('Second role');
            });
        });
    });
});
