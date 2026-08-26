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
    createRole: jest.fn(),
    deleteRole: jest.fn(),
    updateRole: jest.fn(),
}));

import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { act, renderHook } from '@testing-library/react';
import type { ReactNode } from 'react';

import { useCreateRole, useDeleteRole, useUpdateRole } from './useRoleMutations';
import { groupKeys } from '../../groups/utils/queryKeys';
import { organizationUserKeys } from '../../users/utils/queryKeys';
import { createRole, deleteRole, updateRole } from '../services/roles';
import { roleKeys } from '../utils/queryKeys';

const mockCreateRole = jest.mocked(createRole);
const mockUpdateRole = jest.mocked(updateRole);
const mockDeleteRole = jest.mocked(deleteRole);

describe('role mutation cross-feature invalidation', () => {
    let queryClient: QueryClient;
    let invalidateQueries: jest.SpiedFunction<QueryClient['invalidateQueries']>;

    beforeEach(() => {
        queryClient = new QueryClient({ defaultOptions: { mutations: { retry: false } } });
        invalidateQueries = jest.spyOn(queryClient, 'invalidateQueries').mockResolvedValue(undefined);
        mockCreateRole.mockImplementation(async role => ({ ...role, id: 'new-role' }));
        mockUpdateRole.mockImplementation(async role => role);
        mockDeleteRole.mockResolvedValue(undefined);
    });

    afterEach(() => {
        queryClient.clear();
        jest.clearAllMocks();
    });

    function wrapper({ children }: { children: ReactNode }) {
        return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
    }

    it('invalidates the organization role catalog the user detail page reads after creating an ORGANIZATION role', async () => {
        const { result } = renderHook(() => useCreateRole(), { wrapper });

        await act(() => result.current.mutateAsync({ name: 'CUSTOM', scope: 'ORGANIZATION', permissions: {} }));

        expect(invalidateQueries).toHaveBeenCalledWith({ queryKey: roleKeys.listByScope('ORGANIZATION') });
        expect(invalidateQueries).toHaveBeenCalledWith({ queryKey: organizationUserKeys.organizationRoles() });
        expect(invalidateQueries).not.toHaveBeenCalledWith({ queryKey: organizationUserKeys.environmentRoles() });
    });

    it('invalidates the environment role catalog the user detail page reads after updating an ENVIRONMENT role', async () => {
        const { result } = renderHook(() => useUpdateRole(), { wrapper });

        await act(() => result.current.mutateAsync({ name: 'CUSTOM', scope: 'ENVIRONMENT', permissions: {} }));

        expect(invalidateQueries).toHaveBeenCalledWith({ queryKey: roleKeys.listByScope('ENVIRONMENT') });
        expect(invalidateQueries).toHaveBeenCalledWith({ queryKey: organizationUserKeys.environmentRoles() });
    });

    it('invalidates the group role catalog for the matching scope after deleting an API role', async () => {
        const { result } = renderHook(() => useDeleteRole(), { wrapper });

        await act(() => result.current.mutateAsync({ scope: 'API', name: 'CUSTOM' }));

        expect(invalidateQueries).toHaveBeenCalledWith({ queryKey: roleKeys.listByScope('API') });
        expect(invalidateQueries).toHaveBeenCalledWith({ queryKey: groupKeys.roles('API') });
    });

    it('does not cross-invalidate user or group role catalogs for AI_WORKSPACE, which neither feature consumes', async () => {
        const { result } = renderHook(() => useDeleteRole(), { wrapper });

        await act(() => result.current.mutateAsync({ scope: 'AI_WORKSPACE', name: 'CUSTOM' }));

        expect(invalidateQueries).toHaveBeenCalledTimes(2);
        expect(invalidateQueries).toHaveBeenCalledWith({ queryKey: roleKeys.listByScope('AI_WORKSPACE') });
        expect(invalidateQueries).toHaveBeenCalledWith({ queryKey: roleKeys.detail('AI_WORKSPACE', 'CUSTOM') });
    });

    it('invalidates the role detail query after updating a role, so the form is no longer stuck dirty', async () => {
        const { result } = renderHook(() => useUpdateRole(), { wrapper });

        await act(() => result.current.mutateAsync({ name: 'CUSTOM', scope: 'API', permissions: {} }));

        expect(invalidateQueries).toHaveBeenCalledWith({ queryKey: roleKeys.detail('API', 'CUSTOM') });
    });
});
