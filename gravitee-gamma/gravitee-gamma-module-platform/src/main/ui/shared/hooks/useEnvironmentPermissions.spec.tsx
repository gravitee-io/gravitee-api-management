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

import { useHasEnvironmentPermission } from './useEnvironmentPermissions';
import { getEnvironmentPermissions } from '../services/environmentPermissions';

jest.mock('../services/environmentPermissions');
jest.mock('@gravitee/gamma-modules-sdk', () => ({
    useEnvironment: jest.fn(),
}));

const mockGetEnvironmentPermissions = jest.mocked(getEnvironmentPermissions);
const mockUseEnvironment = jest.mocked(useEnvironment);

describe('useHasEnvironmentPermission', () => {
    function wrapper({ children }: { children: ReactNode }) {
        return (
            <QueryClientProvider client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}>
                {children}
            </QueryClientProvider>
        );
    }

    beforeEach(() => {
        mockUseEnvironment.mockReturnValue({ id: 'dev' });
    });

    afterEach(() => {
        jest.clearAllMocks();
    });

    it('returns false until the environment permissions have loaded', () => {
        mockGetEnvironmentPermissions.mockReturnValue(new Promise(() => {}));

        const { result } = renderHook(() => useHasEnvironmentPermission(['environment-shared_policy_group-r']), { wrapper });

        expect(result.current).toBe(false);
    });

    it('returns true once a required permission is granted', async () => {
        mockGetEnvironmentPermissions.mockResolvedValue(['environment-shared_policy_group-r', 'environment-group-r']);

        const { result } = renderHook(() => useHasEnvironmentPermission(['environment-shared_policy_group-r']), { wrapper });

        await waitFor(() => expect(result.current).toBe(true));
    });

    it('returns false when none of the required permissions are granted', async () => {
        mockGetEnvironmentPermissions.mockResolvedValue(['environment-group-r']);

        const { result } = renderHook(() => useHasEnvironmentPermission(['environment-shared_policy_group-r']), { wrapper });

        await waitFor(() => expect(mockGetEnvironmentPermissions).toHaveBeenCalled());
        expect(result.current).toBe(false);
    });
});
