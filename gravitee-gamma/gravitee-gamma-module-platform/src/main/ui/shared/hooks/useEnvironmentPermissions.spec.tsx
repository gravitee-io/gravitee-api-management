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
import { permissionService, useEnvironment } from '@gravitee/gamma-modules-sdk';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { act, renderHook, waitFor } from '@testing-library/react';
import type { ReactNode } from 'react';

import { useEnvironmentPermissions, useEnvironmentPermissionsReady, useHasEnvironmentPermission } from './useEnvironmentPermissions';
import { getEnvironmentPermissions } from '../services/environmentPermissions';
import { environmentPermissionKeys } from '../utils/queryKeys';

jest.mock('@gravitee/gamma-modules-sdk', () => {
    const listeners = new Set<() => void>();
    let granted: string[] = [];
    let environmentGranted: string[] = [];
    return {
        useEnvironment: jest.fn(),
        permissionService: {
            load: jest.fn((_scope: string, next: string[]) => {
                environmentGranted = next;
            }),
            getAllPermissions: () => [...granted, ...environmentGranted],
            subscribe: (listener: () => void) => {
                listeners.add(listener);
                return () => listeners.delete(listener);
            },
            __setGranted: (next: string[]) => {
                granted = next;
                environmentGranted = [];
            },
            __hostLoadEnvironment: (next: string[]) => {
                environmentGranted = next;
                listeners.forEach(listener => listener());
            },
            __emitReset: () => {
                granted = [];
                environmentGranted = [];
                listeners.forEach(listener => listener());
            },
        },
    };
});

jest.mock('../services/environmentPermissions', () => ({
    getEnvironmentPermissions: jest.fn(),
}));

const mockNotifyError = jest.fn();

jest.mock('../notify', () => ({
    notify: { error: (error: unknown, fallback?: string) => mockNotifyError(error, fallback) },
}));

const mockUseEnvironment = jest.mocked(useEnvironment);
const mockGetEnvironmentPermissions = jest.mocked(getEnvironmentPermissions);
const mockLoad = jest.mocked(permissionService.load);
const permissionServiceTest = permissionService as typeof permissionService & {
    __setGranted: (next: string[]) => void;
    __hostLoadEnvironment: (next: string[]) => void;
    __emitReset: () => void;
};

const ENV_ID = 'env-1';
const PREVIOUS_USER_PERMISSIONS = ['environment-application-r'];
const CURRENT_USER_PERMISSIONS: string[] = [];
const PATCHED_AFTER_403 = ['environment-metadata-r'];
const STALE_BACKEND_GRANT = ['environment-metadata-r', 'environment-dictionary-r'];

function wrapperFor(queryClient: QueryClient) {
    return function Wrapper({ children }: { children: ReactNode }) {
        return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
    };
}

describe('useEnvironmentPermissions', () => {
    beforeEach(() => {
        mockLoad.mockClear();
        mockGetEnvironmentPermissions.mockReset();
        mockUseEnvironment.mockReturnValue({ id: ENV_ID } as ReturnType<typeof useEnvironment>);
        mockGetEnvironmentPermissions.mockResolvedValue(CURRENT_USER_PERMISSIONS);
        permissionServiceTest.__setGranted([]);
        mockNotifyError.mockClear();
    });

    it('does not treat a previous login cache as ready after the permission service resets', async () => {
        permissionServiceTest.__setGranted(['organization-user-r']);
        const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
        queryClient.setQueryData(environmentPermissionKeys.detail(ENV_ID), PREVIOUS_USER_PERMISSIONS);

        const { result } = renderHook(
            () => {
                useEnvironmentPermissions();
                return useEnvironmentPermissionsReady();
            },
            { wrapper: wrapperFor(queryClient) },
        );

        expect(result.current).toBe(true);
        expect(queryClient.getQueryData(environmentPermissionKeys.detail(ENV_ID))).toEqual(PREVIOUS_USER_PERMISSIONS);

        act(() => {
            permissionServiceTest.__emitReset();
        });

        await waitFor(() => expect(mockGetEnvironmentPermissions).toHaveBeenCalledWith(ENV_ID));
        await waitFor(() => expect(result.current).toBe(true));

        expect(mockLoad).toHaveBeenCalledWith('environment', CURRENT_USER_PERMISSIONS);
    });

    it('keeps a 403-patched cache when the layout remounts without a login change', async () => {
        const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
        queryClient.setQueryData(environmentPermissionKeys.detail(ENV_ID), PATCHED_AFTER_403);
        mockGetEnvironmentPermissions.mockResolvedValue(STALE_BACKEND_GRANT);

        const { result, unmount } = renderHook(
            () => {
                useEnvironmentPermissions();
                return useEnvironmentPermissionsReady();
            },
            { wrapper: wrapperFor(queryClient) },
        );

        await waitFor(() => expect(result.current).toBe(true));
        expect(queryClient.getQueryData(environmentPermissionKeys.detail(ENV_ID))).toEqual(PATCHED_AFTER_403);
        expect(mockGetEnvironmentPermissions).not.toHaveBeenCalled();

        unmount();
        mockGetEnvironmentPermissions.mockClear();
        mockLoad.mockClear();

        const { result: remounted } = renderHook(
            () => {
                useEnvironmentPermissions();
                return useEnvironmentPermissionsReady();
            },
            { wrapper: wrapperFor(queryClient) },
        );

        expect(remounted.current).toBe(true);
        expect(mockGetEnvironmentPermissions).not.toHaveBeenCalled();
        expect(queryClient.getQueryData(environmentPermissionKeys.detail(ENV_ID))).toEqual(PATCHED_AFTER_403);
        expect(mockLoad).toHaveBeenCalledWith('environment', PATCHED_AFTER_403);
        expect(mockLoad).not.toHaveBeenCalledWith('environment', STALE_BACKEND_GRANT);
    });

    it('reloads environment permissions when the environment id changes', async () => {
        const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
        mockGetEnvironmentPermissions.mockImplementation(async (envId: string) =>
            envId === ENV_ID ? PREVIOUS_USER_PERMISSIONS : ['environment-metadata-r'],
        );

        const { rerender } = renderHook(
            () => {
                useEnvironmentPermissions();
                return useEnvironmentPermissionsReady();
            },
            { wrapper: wrapperFor(queryClient) },
        );

        await waitFor(() => expect(mockLoad).toHaveBeenCalledWith('environment', PREVIOUS_USER_PERMISSIONS));

        mockUseEnvironment.mockReturnValue({ id: 'env-2' } as ReturnType<typeof useEnvironment>);
        rerender();

        await waitFor(() => expect(mockGetEnvironmentPermissions).toHaveBeenCalledWith('env-2'));
        await waitFor(() => expect(mockLoad).toHaveBeenCalledWith('environment', ['environment-metadata-r']));
    });

    it('writes the cached map back when a host refetch restores stripped environment grants', async () => {
        const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
        queryClient.setQueryData(environmentPermissionKeys.detail(ENV_ID), PATCHED_AFTER_403);
        mockGetEnvironmentPermissions.mockResolvedValue(STALE_BACKEND_GRANT);

        renderHook(
            () => {
                useEnvironmentPermissions();
                return useEnvironmentPermissionsReady();
            },
            { wrapper: wrapperFor(queryClient) },
        );

        await waitFor(() => expect(mockLoad).toHaveBeenCalledWith('environment', PATCHED_AFTER_403));
        mockLoad.mockClear();

        act(() => {
            permissionServiceTest.__hostLoadEnvironment(STALE_BACKEND_GRANT);
        });

        expect(mockLoad).toHaveBeenCalledWith('environment', PATCHED_AFTER_403);
        expect(mockGetEnvironmentPermissions).not.toHaveBeenCalled();
    });

    it('becomes ready with an empty load skipped when the environment permission fetch fails', async () => {
        const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
        mockGetEnvironmentPermissions.mockRejectedValue(new Error('unavailable'));

        const { result } = renderHook(
            () => {
                useEnvironmentPermissions();
                return useEnvironmentPermissionsReady();
            },
            { wrapper: wrapperFor(queryClient) },
        );

        await waitFor(() => expect(result.current).toBe(true));
        expect(mockLoad).not.toHaveBeenCalled();
    });

    // An empty menu after a failed fetch otherwise reads as "your role grants nothing", which is the
    // wrong advice. Classic draws the same line with a toast from its HTTP error interceptor.
    it('reports a failed permission fetch as an error rather than silently emptying the menu', async () => {
        const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
        mockGetEnvironmentPermissions.mockRejectedValue(new Error('unavailable'));

        renderHook(() => useEnvironmentPermissions(), { wrapper: wrapperFor(queryClient) });

        await waitFor(() => expect(mockNotifyError).toHaveBeenCalled());
        expect(mockNotifyError.mock.calls[0]?.[1]).toBe('Your permissions could not be loaded. Some menu items may be missing.');
    });

    it('does not report an error when the permission fetch succeeds', async () => {
        const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
        mockGetEnvironmentPermissions.mockResolvedValue(['environment-application-r']);

        renderHook(() => useEnvironmentPermissions(), { wrapper: wrapperFor(queryClient) });

        await waitFor(() => expect(mockLoad).toHaveBeenCalled());
        expect(mockNotifyError).not.toHaveBeenCalled();
    });

    it('stays ready for a later observer without refetching', async () => {
        const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });

        const { result: layout } = renderHook(
            () => {
                useEnvironmentPermissions();
                return useEnvironmentPermissionsReady();
            },
            { wrapper: wrapperFor(queryClient) },
        );

        await waitFor(() => expect(layout.current).toBe(true));
        expect(mockGetEnvironmentPermissions).toHaveBeenCalledTimes(1);

        const { result: laterGuard } = renderHook(() => useEnvironmentPermissionsReady(), {
            wrapper: wrapperFor(queryClient),
        });

        expect(laterGuard.current).toBe(true);
        expect(mockGetEnvironmentPermissions).toHaveBeenCalledTimes(1);
    });
});

describe('useHasEnvironmentPermission', () => {
    beforeEach(() => {
        mockLoad.mockClear();
        mockGetEnvironmentPermissions.mockReset();
        mockUseEnvironment.mockReturnValue({ id: ENV_ID } as ReturnType<typeof useEnvironment>);
        permissionServiceTest.__setGranted([]);
    });

    it('returns false until the environment permissions have loaded', () => {
        mockGetEnvironmentPermissions.mockReturnValue(new Promise(() => {}));

        const { result } = renderHook(() => useHasEnvironmentPermission(['environment-shared_policy_group-r']), {
            wrapper: wrapperFor(new QueryClient({ defaultOptions: { queries: { retry: false } } })),
        });

        expect(result.current).toBe(false);
    });

    it('returns true once a required permission is granted', async () => {
        const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
        mockGetEnvironmentPermissions.mockResolvedValue(['environment-shared_policy_group-r', 'environment-group-r']);

        const { result } = renderHook(() => useHasEnvironmentPermission(['environment-shared_policy_group-r']), {
            wrapper: wrapperFor(queryClient),
        });

        await waitFor(() => expect(result.current).toBe(true));
    });

    it('returns false when none of the required permissions are granted', async () => {
        const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
        mockGetEnvironmentPermissions.mockResolvedValue(['environment-group-r']);

        const { result } = renderHook(() => useHasEnvironmentPermission(['environment-shared_policy_group-r']), {
            wrapper: wrapperFor(queryClient),
        });

        await waitFor(() => expect(mockGetEnvironmentPermissions).toHaveBeenCalled());
        expect(result.current).toBe(false);
    });

    it('does not refetch when a later observer mounts after a 403 patch', async () => {
        const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
        mockGetEnvironmentPermissions.mockResolvedValue(['environment-shared_policy_group-r']);

        const { result: layout } = renderHook(
            () => {
                useEnvironmentPermissions();
                return useEnvironmentPermissionsReady();
            },
            { wrapper: wrapperFor(queryClient) },
        );

        await waitFor(() => expect(layout.current).toBe(true));
        expect(mockGetEnvironmentPermissions).toHaveBeenCalledTimes(1);

        act(() => {
            queryClient.setQueryData(environmentPermissionKeys.detail(ENV_ID), []);
        });

        const { result: pageGuard } = renderHook(() => useHasEnvironmentPermission(['environment-shared_policy_group-r']), {
            wrapper: wrapperFor(queryClient),
        });

        expect(pageGuard.current).toBe(false);
        expect(mockGetEnvironmentPermissions).toHaveBeenCalledTimes(1);
    });
});
