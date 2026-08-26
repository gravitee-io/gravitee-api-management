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
jest.mock('../services/roles', () => ({
    getPermissionsByScopes: jest.fn(),
    getRole: jest.fn(),
    listRolesByScope: jest.fn(),
}));

import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { renderHook, waitFor } from '@testing-library/react';
import type { ReactNode } from 'react';

import { usePermissionsByScopes, useRole, useRolesByScope } from './useRoles';
import { getPermissionsByScopes, getRole, listRolesByScope } from '../services/roles';
import { ROLE_SCOPES } from '../types/role';

const mockListRolesByScope = jest.mocked(listRolesByScope);
const mockGetRole = jest.mocked(getRole);
const mockGetPermissionsByScopes = jest.mocked(getPermissionsByScopes);

function wrapper({ children }: { children: ReactNode }) {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
}

describe('useRolesByScope', () => {
    beforeEach(() => {
        mockListRolesByScope.mockReset();
    });

    it('fetches every role scope in parallel and groups the results with their label', async () => {
        mockListRolesByScope.mockImplementation(async scope =>
            scope === 'ORGANIZATION' ? [{ name: 'ADMIN', scope: 'ORGANIZATION' }] : [],
        );

        const { result } = renderHook(() => useRolesByScope(), { wrapper });

        await waitFor(() => expect(result.current.isLoading).toBe(false));

        expect(mockListRolesByScope).toHaveBeenCalledTimes(ROLE_SCOPES.length);
        const organizationGroup = result.current.groups.find(group => group.scope === 'ORGANIZATION');
        expect(organizationGroup).toEqual({
            scope: 'ORGANIZATION',
            label: 'Organization',
            roles: [{ name: 'ADMIN', scope: 'ORGANIZATION' }],
            isLoading: false,
            isError: false,
        });
        const explorerGroup = result.current.groups.find(group => group.scope === 'EXPLORER');
        expect(explorerGroup?.label).toBe('Explorer');
        expect(explorerGroup?.roles).toEqual([]);
    });

    it('surfaces a failed scope as an error instead of an empty roles list', async () => {
        mockListRolesByScope.mockImplementation(async scope => {
            if (scope === 'ORGANIZATION') throw new Error('forbidden');
            return [];
        });

        const { result } = renderHook(() => useRolesByScope(), { wrapper });

        await waitFor(() => expect(result.current.isLoading).toBe(false));

        const organizationGroup = result.current.groups.find(group => group.scope === 'ORGANIZATION');
        expect(organizationGroup?.isError).toBe(true);
        expect(organizationGroup?.roles).toEqual([]);
        const explorerGroup = result.current.groups.find(group => group.scope === 'EXPLORER');
        expect(explorerGroup?.isError).toBe(false);
    });
});

describe('useRole', () => {
    beforeEach(() => {
        mockGetRole.mockReset();
    });

    it('fetches a single role by scope and name', async () => {
        mockGetRole.mockResolvedValue({ name: 'ADMIN', scope: 'ORGANIZATION' });

        const { result } = renderHook(() => useRole('ORGANIZATION', 'ADMIN'), { wrapper });

        await waitFor(() => expect(result.current.data).toEqual({ name: 'ADMIN', scope: 'ORGANIZATION' }));
        expect(mockGetRole).toHaveBeenCalledWith('ORGANIZATION', 'ADMIN');
    });

    it('stays disabled without a role name (create mode)', () => {
        const { result } = renderHook(() => useRole('ORGANIZATION', undefined), { wrapper });

        expect(result.current.fetchStatus).toBe('idle');
        expect(mockGetRole).not.toHaveBeenCalled();
    });
});

describe('usePermissionsByScopes', () => {
    it('fetches the permissions-by-scope catalog', async () => {
        mockGetPermissionsByScopes.mockResolvedValue({ ORGANIZATION: ['ROLE'] });

        const { result } = renderHook(() => usePermissionsByScopes(), { wrapper });

        await waitFor(() => expect(result.current.data).toEqual({ ORGANIZATION: ['ROLE'] }));
    });
});
