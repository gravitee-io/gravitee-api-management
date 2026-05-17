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
import { renderHook, waitFor } from '@testing-library/react';
import type { ReactNode } from 'react';

import { useApplicationPermissions } from './useApplicationPermissions';
import { getApplicationPermissions } from '../services/applicationPermissions';

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    useEnvironment: jest.fn(),
    permissionService: {
        load: jest.fn(),
        clear: jest.fn(),
    },
}));

jest.mock('../services/applicationPermissions');

const mockUseEnvironment = jest.mocked(useEnvironment);
const mockGetApplicationPermissions = jest.mocked(getApplicationPermissions);
const mockPermissionService = permissionService as jest.Mocked<typeof permissionService>;

function wrapper({ children }: { children: ReactNode }) {
    const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    return <QueryClientProvider client={client}>{children}</QueryClientProvider>;
}

describe('useApplicationPermissions', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        mockUseEnvironment.mockReturnValue({ id: 'DEFAULT' });
    });

    it('sets permissionsReady on success and loads application scope', async () => {
        mockGetApplicationPermissions.mockResolvedValue(['application-definition-r']);

        const { result } = renderHook(() => useApplicationPermissions('app-1'), { wrapper });

        await waitFor(() => expect(result.current.permissionsReady).toBe(true));

        expect(mockPermissionService.load).toHaveBeenCalledWith('application', ['application-definition-r']);
        expect(result.current.isError).toBe(false);
    });

    it('exposes isError when the permissions request fails', async () => {
        mockGetApplicationPermissions.mockRejectedValue(new Error('network'));

        const { result } = renderHook(() => useApplicationPermissions('app-1'), { wrapper });

        await waitFor(() => expect(result.current.isError).toBe(true));

        expect(result.current.permissionsReady).toBe(false);
        expect(mockPermissionService.load).not.toHaveBeenCalled();
    });
});
