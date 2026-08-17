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
import { useHasPermission } from '@gravitee/gamma-modules-sdk';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';

import { TenantsPage } from './TenantsPage';
import { useCreateTenant, useDeleteTenant, useUpdateTenant } from '../features/tenants/hooks/useTenantMutations';
import { useTenants } from '../features/tenants/hooks/useTenants';
import type { Tenant } from '../features/tenants/types/tenant';
import { ApimApiError } from '../shared/api/apimClient';
import { notify } from '../shared/notify';

const mockNavigate = jest.fn();

jest.mock('react-router-dom', () => ({
    ...jest.requireActual('react-router-dom'),
    useNavigate: () => mockNavigate,
}));

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    useHasPermission: jest.fn(),
    useEnvironment: () => ({ id: 'env-1' }),
}));
jest.mock('../features/tenants/hooks/useTenants');
jest.mock('../features/tenants/hooks/useTenantMutations');
jest.mock('../shared/notify', () => ({
    notify: { success: jest.fn(), error: jest.fn() },
}));

jest.mock('../features/tenants/components/TenantsTable', () => ({
    TenantsTable: ({
        rows,
        canEdit,
        canDelete,
        onEdit,
        onDelete,
    }: {
        rows: Tenant[];
        canEdit: boolean;
        canDelete: boolean;
        onEdit: (tenant: Tenant) => void;
        onDelete: (tenant: Tenant) => void;
    }) => (
        <div>
            {rows.map(tenant => (
                <div key={tenant.key} data-testid={`row-${tenant.key}`}>
                    <span>{tenant.name}</span>
                    {canEdit && (
                        <button type="button" onClick={() => onEdit(tenant)}>
                            Edit {tenant.name}
                        </button>
                    )}
                    {canDelete && (
                        <button type="button" onClick={() => onDelete(tenant)}>
                            Delete {tenant.name}
                        </button>
                    )}
                </div>
            ))}
        </div>
    ),
}));

const mockUseHasPermission = jest.mocked(useHasPermission);
const mockUseTenants = jest.mocked(useTenants);
const mockUseCreateTenant = jest.mocked(useCreateTenant);
const mockUseUpdateTenant = jest.mocked(useUpdateTenant);
const mockUseDeleteTenant = jest.mocked(useDeleteTenant);

const STUB_TENANTS: Tenant[] = [
    { id: 't-1', key: 'us-east', name: 'US East', description: 'Virginia gateway cluster' },
    { id: 't-2', key: 'eu-west', name: 'EU West', description: 'Frankfurt gateway cluster' },
];

function makeQueryResult(overrides: Partial<ReturnType<typeof useTenants>> = {}): ReturnType<typeof useTenants> {
    return {
        data: STUB_TENANTS,
        isLoading: false,
        isError: false,
        ...overrides,
    } as ReturnType<typeof useTenants>;
}

// eslint-disable-next-line @typescript-eslint/no-explicit-any
function makeMutation(mutateAsync = jest.fn()): any {
    return { mutateAsync, isPending: false };
}

function renderPage() {
    const queryClient = new QueryClient();
    queryClient.setQueryData(['environment-permissions', 'env-1'], ['organization-tenant-r']);
    return render(
        <QueryClientProvider client={queryClient}>
            <MemoryRouter initialEntries={['/tenants']}>
                <TenantsPage />
            </MemoryRouter>
        </QueryClientProvider>,
    );
}

describe('TenantsPage', () => {
    beforeEach(() => {
        mockNavigate.mockClear();
        mockUseHasPermission.mockReturnValue(true);
        mockUseTenants.mockReturnValue(makeQueryResult());
        mockUseCreateTenant.mockReturnValue(makeMutation());
        mockUseUpdateTenant.mockReturnValue(makeMutation());
        mockUseDeleteTenant.mockReturnValue(makeMutation());
        Element.prototype.scrollIntoView = jest.fn();
        global.ResizeObserver = class ResizeObserver {
            observe() {}
            unobserve() {}
            disconnect() {}
        } as typeof ResizeObserver;
    });

    afterEach(() => {
        jest.clearAllMocks();
    });

    describe('page header', () => {
        it('renders the page title, subtitle, and gateway config banner', () => {
            renderPage();
            expect(screen.queryByRole('heading', { name: 'Tenants' })).not.toBeNull();
            expect(
                screen.queryByText('Segment which API endpoints a gateway will proxy to, so one API can stay local to a region.'),
            ).not.toBeNull();
            expect(
                screen.queryByText(
                    'Copy the tenant key into the API gateway configuration file so the gateway only receives endpoints tagged with that tenant.',
                ),
            ).not.toBeNull();
        });

        it('shows the Add a tenant button when the user can create and tenants exist', () => {
            renderPage();
            expect(screen.queryByRole('button', { name: /Add a tenant/i })).not.toBeNull();
        });

        it('shows a single header Add a tenant button on the empty page when the user can create', () => {
            mockUseTenants.mockReturnValue(makeQueryResult({ data: [] }));
            renderPage();
            expect(screen.getAllByRole('button', { name: /Add a tenant/i })).toHaveLength(1);
        });

        it('hides the Add a tenant button when the user cannot create', () => {
            mockUseHasPermission.mockImplementation(
                ({ anyOf }) => !!(anyOf?.includes('organization-tenant-u') || anyOf?.includes('environment-tenant-u')),
            );
            renderPage();
            expect(screen.queryByRole('button', { name: /Add a tenant/i })).toBeNull();
        });
    });

    describe('loading and error states', () => {
        it('shows skeleton rows while data is loading', () => {
            mockUseTenants.mockReturnValue(makeQueryResult({ data: undefined, isLoading: true }));
            const { container } = renderPage();
            expect(container.querySelectorAll('[data-slot="skeleton"]').length).toBeGreaterThan(0);
        });

        it('shows an error message when the query fails', () => {
            mockUseTenants.mockReturnValue(makeQueryResult({ data: undefined, isError: true }));
            renderPage();
            expect(screen.queryByText('Failed to load tenants. Please refresh and try again.')).not.toBeNull();
        });

        it('redirects away on a 403 without showing the generic error', async () => {
            mockUseTenants.mockReturnValue(makeQueryResult({ data: undefined, isError: true, error: new ApimApiError(403, 'Forbidden') }));

            renderPage();

            await waitFor(() => expect(mockNavigate).toHaveBeenCalledWith('../applications', { replace: true }));
            expect(screen.queryByText('Failed to load tenants. Please refresh and try again.')).toBeNull();
        });
    });

    describe('empty state', () => {
        it('shows the educational empty state instead of the table and banner', () => {
            mockUseTenants.mockReturnValue(makeQueryResult({ data: [] }));
            renderPage();
            expect(screen.getByText('Why create a tenant?')).not.toBeNull();
            expect(
                screen.queryByText(
                    'Copy the tenant key into the API gateway configuration file so the gateway only receives endpoints tagged with that tenant.',
                ),
            ).toBeNull();
        });
    });

    describe('create and edit', () => {
        it('opens the create sheet from the header button', () => {
            renderPage();
            fireEvent.click(screen.getByRole('button', { name: /Add a tenant/i }));
            expect(screen.queryByRole('heading', { name: 'Create a tenant' })).not.toBeNull();
        });

        it('calls createMutation and shows a success toast', async () => {
            const mutateAsync = jest.fn().mockResolvedValue([]);
            mockUseCreateTenant.mockReturnValue(makeMutation(mutateAsync));
            renderPage();

            fireEvent.click(screen.getByRole('button', { name: /Add a tenant/i }));
            fireEvent.change(screen.getByLabelText(/^Name/), { target: { value: 'AP South' } });
            fireEvent.click(screen.getByRole('button', { name: 'Create tenant' }));

            await waitFor(() => {
                expect(mutateAsync).toHaveBeenCalledWith({ name: 'AP South', key: 'ap-south', description: undefined });
                expect(notify.success).toHaveBeenCalledWith('Tenant successfully created!');
            });
        });

        it('opens the edit sheet and saves name changes', async () => {
            const mutateAsync = jest.fn().mockResolvedValue([{ ...STUB_TENANTS[0], name: 'US East 2' }]);
            mockUseUpdateTenant.mockReturnValue(makeMutation(mutateAsync));
            renderPage();

            fireEvent.click(screen.getByRole('button', { name: 'Edit US East' }));
            expect(screen.queryByRole('heading', { name: 'Edit a tenant' })).not.toBeNull();
            fireEvent.change(screen.getByLabelText(/^Name/), { target: { value: 'US East 2' } });
            fireEvent.click(screen.getByRole('button', { name: 'Save' }));

            await waitFor(() => {
                expect(mutateAsync).toHaveBeenCalledWith({
                    key: 'us-east',
                    name: 'US East 2',
                    description: 'Virginia gateway cluster',
                });
                expect(notify.success).toHaveBeenCalledWith('Tenant successfully updated!');
            });
        });

        it('reports an error instead of success when the API updates nothing', async () => {
            const mutateAsync = jest.fn().mockResolvedValue([]);
            mockUseUpdateTenant.mockReturnValue(makeMutation(mutateAsync));
            renderPage();

            fireEvent.click(screen.getByRole('button', { name: 'Edit US East' }));
            fireEvent.change(screen.getByLabelText(/^Name/), { target: { value: 'US East 2' } });
            fireEvent.click(screen.getByRole('button', { name: 'Save' }));

            await waitFor(() => {
                expect(screen.queryByText('This tenant no longer exists. Refresh the page and try again.')).not.toBeNull();
            });
            expect(notify.success).not.toHaveBeenCalled();
            expect(screen.queryByRole('heading', { name: 'Edit a tenant' })).not.toBeNull();
        });
    });

    describe('delete', () => {
        it('confirms delete and shows a success toast', async () => {
            const mutateAsync = jest.fn().mockResolvedValue(undefined);
            mockUseDeleteTenant.mockReturnValue(makeMutation(mutateAsync));
            renderPage();

            fireEvent.click(screen.getByRole('button', { name: 'Delete US East' }));
            expect(screen.queryByRole('heading', { name: 'Delete a tenant' })).not.toBeNull();
            fireEvent.click(screen.getByRole('button', { name: 'Delete' }));

            await waitFor(() => {
                expect(mutateAsync).toHaveBeenCalledWith('us-east');
                expect(notify.success).toHaveBeenCalledWith('Tenant successfully deleted!');
            });
        });

        it('shows an error toast when delete fails', async () => {
            const error = new Error('delete failed');
            const mutateAsync = jest.fn().mockRejectedValue(error);
            mockUseDeleteTenant.mockReturnValue(makeMutation(mutateAsync));
            renderPage();

            fireEvent.click(screen.getByRole('button', { name: 'Delete US East' }));
            fireEvent.click(screen.getByRole('button', { name: 'Delete' }));

            await waitFor(() => {
                expect(notify.error).toHaveBeenCalledWith(error, 'Failed to delete tenant');
            });
        });
    });
});
