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
import { useEnvironment } from '@gravitee/gamma-modules-sdk';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { renderHook, waitFor } from '@testing-library/react';
import type { ReactNode } from 'react';
import { MemoryRouter } from 'react-router-dom';

import { useForbiddenResourceRedirect } from './useForbiddenResourceRedirect';

const mockNavigate = jest.fn();

jest.mock('react-router-dom', () => ({
    ...jest.requireActual('react-router-dom'),
    useNavigate: () => mockNavigate,
}));

const mockLoad = jest.fn();
const mockMarkNavItemDenied = jest.fn();

jest.mock('../nav/deniedNavItems', () => ({
    markNavItemDenied: (itemKey: string) => mockMarkNavItemDenied(itemKey),
}));

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    useEnvironment: jest.fn(),
    permissionService: {
        load: (...args: unknown[]) => mockLoad(...args),
    },
}));

const mockUseEnvironment = jest.mocked(useEnvironment);

function renderWithClient(
    isForbidden: boolean,
    seedPermissions: string[],
    permissionPrefix: string | readonly string[] = 'environment-dictionary-',
    navItemKey = 'dictionaries',
) {
    const queryClient = new QueryClient();
    queryClient.setQueryData(['environment-permissions', 'env-1'], seedPermissions);
    const invalidateSpy = jest.spyOn(queryClient, 'invalidateQueries');

    const wrapper = ({ children }: { children: ReactNode }) => (
        <QueryClientProvider client={queryClient}>
            <MemoryRouter>{children}</MemoryRouter>
        </QueryClientProvider>
    );

    renderHook(
        () =>
            useForbiddenResourceRedirect({
                isForbidden,
                navItemKey,
                permissionPrefix,
                redirectTo: '../applications',
            }),
        { wrapper },
    );

    return { queryClient, invalidateSpy };
}

describe('useForbiddenResourceRedirect', () => {
    beforeEach(() => {
        mockNavigate.mockClear();
        mockLoad.mockClear();
        mockMarkNavItemDenied.mockClear();
        mockUseEnvironment.mockReturnValue({ id: 'env-1' } as ReturnType<typeof useEnvironment>);
    });

    it('does nothing when not forbidden', () => {
        const { queryClient } = renderWithClient(false, ['environment-dictionary-r']);

        expect(mockNavigate).not.toHaveBeenCalled();
        expect(queryClient.getQueryData(['environment-permissions', 'env-1'])).toEqual(['environment-dictionary-r']);
    });

    it('strips only the matching permission prefix and navigates away when forbidden', async () => {
        const { queryClient } = renderWithClient(true, ['environment-metadata-r', 'environment-dictionary-r', 'environment-dictionary-c']);

        await waitFor(() => expect(mockNavigate).toHaveBeenCalledWith('../applications', { replace: true }));
        expect(queryClient.getQueryData(['environment-permissions', 'env-1'])).toEqual(['environment-metadata-r']);
        expect(mockLoad).toHaveBeenCalledWith('environment', ['environment-metadata-r']);
    });

    it('does not invalidate the permissions query, so a stale backend grant cannot silently restore access', async () => {
        const { invalidateSpy } = renderWithClient(true, ['environment-dictionary-r']);

        await waitFor(() => expect(mockNavigate).toHaveBeenCalled());
        expect(invalidateSpy).not.toHaveBeenCalled();
    });

    // Stripping "everything" out of an empty cache would push an empty grant into the permission
    // service and drop every environment permission the host had already loaded.
    it('does not touch the permission service when nothing is cached yet', async () => {
        const queryClient = new QueryClient();
        const wrapper = ({ children }: { children: ReactNode }) => (
            <QueryClientProvider client={queryClient}>
                <MemoryRouter>{children}</MemoryRouter>
            </QueryClientProvider>
        );

        renderHook(
            () =>
                useForbiddenResourceRedirect({
                    isForbidden: true,
                    navItemKey: 'dictionaries',
                    permissionPrefix: 'environment-dictionary-',
                    redirectTo: '../applications',
                }),
            { wrapper },
        );

        await waitFor(() => expect(mockNavigate).toHaveBeenCalledWith('../applications', { replace: true }));
        expect(mockLoad).not.toHaveBeenCalled();
        expect(queryClient.getQueryData(['environment-permissions', 'env-1'])).toBeUndefined();
    });

    it('still navigates away when the environment id is unavailable', async () => {
        mockUseEnvironment.mockReturnValue(undefined as unknown as ReturnType<typeof useEnvironment>);

        renderWithClient(true, []);

        await waitFor(() => expect(mockNavigate).toHaveBeenCalledWith('../applications', { replace: true }));
    });

    it('strips multiple permission prefixes in one redirect', async () => {
        const { queryClient } = renderWithClient(
            true,
            ['organization-tenant-r', 'environment-tenant-c', 'environment-metadata-r'],
            ['organization-tenant-', 'environment-tenant-'],
        );

        await waitFor(() => expect(mockNavigate).toHaveBeenCalledTimes(1));
        expect(queryClient.getQueryData(['environment-permissions', 'env-1'])).toEqual(['environment-metadata-r']);
    });

    // The strip only rewrites the environment scope, so an organization-scoped grant survives it and the
    // item stays visible — and stays the landing key that '../applications' bounces back to.
    it('marks the nav item denied even when the strip leaves the permissions untouched', async () => {
        const { queryClient } = renderWithClient(true, ['environment-metadata-r'], ['organization-tenant-'], 'tenants');

        await waitFor(() => expect(mockNavigate).toHaveBeenCalled());
        expect(queryClient.getQueryData(['environment-permissions', 'env-1'])).toEqual(['environment-metadata-r']);
        expect(mockMarkNavItemDenied).toHaveBeenCalledWith('tenants');
    });

    it('does not mark the nav item denied when the resource is allowed', () => {
        renderWithClient(false, ['organization-tenant-r'], ['organization-tenant-'], 'tenants');

        expect(mockMarkNavItemDenied).not.toHaveBeenCalled();
    });

    it('keeps every permission when no prefix is given, rather than stripping them all', async () => {
        const { queryClient } = renderWithClient(true, ['organization-tenant-r', 'environment-metadata-r'], []);

        await waitFor(() => expect(mockNavigate).toHaveBeenCalledTimes(1));
        expect(queryClient.getQueryData(['environment-permissions', 'env-1'])).toEqual(['organization-tenant-r', 'environment-metadata-r']);
    });
});
