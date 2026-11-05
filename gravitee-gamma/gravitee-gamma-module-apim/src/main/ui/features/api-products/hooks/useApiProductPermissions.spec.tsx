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
import { permissionService } from '@gravitee/gamma-modules-sdk';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { renderHook, waitFor } from '@testing-library/react';
import type { ReactNode } from 'react';

import { useApiProductResourcePermissions } from './useApiProductPermissions';
import { getCurrentUser } from '../../apis/services/currentUser';
import { currentUserKeys } from '../../apis/utils/queryKeys';
import { getApiProductPermissions } from '../services/apiProductMembers';
import { apiProductKeys } from '../utils/queryKeys';

const mockSubscribeListeners = new Set<() => void>();

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    useEnvironment: () => ({ id: 'DEFAULT', hrids: ['DEFAULT'] }),
    permissionService: {
        load: jest.fn(),
        clear: jest.fn(),
        reset: jest.fn(),
        getAllPermissions: jest.fn(() => []),
        hasAnyOf: jest.fn(() => false),
        hasAllOf: jest.fn(() => false),
        subscribe: jest.fn((listener: () => void) => {
            mockSubscribeListeners.add(listener);
            return () => mockSubscribeListeners.delete(listener);
        }),
        getSnapshot: jest.fn(() => 0),
    },
}));

jest.mock('../../apis/services/currentUser', () => ({
    getCurrentUser: jest.fn(),
}));

jest.mock('../services/apiProductMembers', () => ({
    getApiProductPermissions: jest.fn(),
}));

const mockGetCurrentUser = getCurrentUser as jest.Mock;
const mockGetApiProductPermissions = getApiProductPermissions as jest.Mock;
const mockPermissionService = permissionService as jest.Mocked<typeof permissionService>;

function emitPermissionServiceChange() {
    mockSubscribeListeners.forEach(listener => listener());
}

describe('useApiProductResourcePermissions', () => {
    let queryClient: QueryClient;

    function wrapper({ children }: { children: ReactNode }) {
        return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
    }

    beforeEach(() => {
        queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
        jest.clearAllMocks();
        mockGetCurrentUser.mockResolvedValue({ id: 'admin-id' });
        mockPermissionService.getAllPermissions.mockReturnValue([]);
    });

    it('scopes the permissions query to the signed-in user', async () => {
        mockGetApiProductPermissions.mockResolvedValue(['api_product-definition-u', 'api_product-definition-r']);

        const { result } = renderHook(() => useApiProductResourcePermissions('product-1', 'DEFINITION'), { wrapper });

        await waitFor(() => expect(result.current.isLoading).toBe(false));
        expect(result.current.canUpdate).toBe(true);

        const entries = queryClient.getQueryCache().findAll({
            queryKey: apiProductKeys.permissions('DEFAULT', 'product-1', 'admin-id'),
        });
        expect(entries).toHaveLength(1);
        expect(queryClient.getQueryData(currentUserKeys.forSession())).toEqual({ id: 'admin-id' });
    });

    it('drops update rights when the session user changes to read-only without unmounting', async () => {
        mockGetApiProductPermissions
            .mockResolvedValueOnce(['api_product-definition-u', 'api_product-definition-r'])
            .mockResolvedValueOnce(['api_product-definition-r']);

        const { result, rerender } = renderHook(() => useApiProductResourcePermissions('product-1', 'DEFINITION'), { wrapper });

        await waitFor(() => expect(result.current.canUpdate).toBe(true));

        mockGetCurrentUser.mockResolvedValue({ id: 'api1-id' });
        mockPermissionService.getAllPermissions.mockReturnValue([]);
        emitPermissionServiceChange();
        rerender();

        await waitFor(() => expect(mockGetCurrentUser).toHaveBeenCalledTimes(2));
        await waitFor(() => expect(mockGetApiProductPermissions).toHaveBeenCalledTimes(2));
        await waitFor(() => expect(result.current.isLoading).toBe(false));
        expect(result.current.canUpdate).toBe(false);
    });

    it('grants update rights when switching from read-only to admin without unmounting', async () => {
        mockGetCurrentUser.mockResolvedValue({ id: 'api1-id' });
        mockGetApiProductPermissions
            .mockResolvedValueOnce(['api_product-definition-r'])
            .mockResolvedValueOnce(['api_product-definition-u', 'api_product-definition-r']);

        const { result, rerender } = renderHook(() => useApiProductResourcePermissions('product-1', 'DEFINITION'), { wrapper });

        await waitFor(() => expect(result.current.canUpdate).toBe(false));

        mockGetCurrentUser.mockResolvedValue({ id: 'admin-id' });
        mockPermissionService.getAllPermissions.mockReturnValue([]);
        emitPermissionServiceChange();
        rerender();

        await waitFor(() => expect(mockGetCurrentUser).toHaveBeenCalledTimes(2));
        await waitFor(() => expect(mockGetApiProductPermissions).toHaveBeenCalledTimes(2));
        await waitFor(() => expect(result.current.isLoading).toBe(false));
        expect(result.current.canUpdate).toBe(true);
    });

    it('ends loading after a session reset and refetch', async () => {
        mockGetApiProductPermissions.mockResolvedValue(['api_product-definition-r']);

        const { result, rerender } = renderHook(() => useApiProductResourcePermissions('product-1', 'DEFINITION'), { wrapper });

        await waitFor(() => expect(result.current.isLoading).toBe(false));

        mockGetCurrentUser.mockResolvedValue({ id: 'api1-id' });
        mockPermissionService.getAllPermissions.mockReturnValue([]);
        emitPermissionServiceChange();
        rerender();

        await waitFor(() => expect(mockGetCurrentUser).toHaveBeenCalledTimes(2));
        await waitFor(() => expect(mockGetApiProductPermissions).toHaveBeenCalledTimes(2));
        await waitFor(() => expect(result.current.isLoading).toBe(false));
        await waitFor(() => expect(result.current.canRead).toBe(true));
    });

    it('stays idle while permissions refetch in the background', async () => {
        mockGetApiProductPermissions.mockResolvedValue(['api_product-definition-u', 'api_product-definition-r']);

        const { result } = renderHook(() => useApiProductResourcePermissions('product-1', 'DEFINITION'), { wrapper });

        await waitFor(() => expect(result.current.isLoading).toBe(false));
        expect(result.current.canUpdate).toBe(true);

        let resolveRefetch: (value: string[]) => void = () => undefined;
        mockGetApiProductPermissions.mockImplementation(
            () =>
                new Promise<string[]>(resolve => {
                    resolveRefetch = resolve;
                }),
        );

        void queryClient.invalidateQueries({
            queryKey: apiProductKeys.permissions('DEFAULT', 'product-1', 'admin-id'),
        });

        await waitFor(() => expect(mockGetApiProductPermissions).toHaveBeenCalledTimes(2));
        expect(result.current.isLoading).toBe(false);
        expect(result.current.canUpdate).toBe(true);

        resolveRefetch(['api_product-definition-r']);
        await waitFor(() => expect(result.current.canUpdate).toBe(false));
        expect(result.current.isLoading).toBe(false);
    });
});
