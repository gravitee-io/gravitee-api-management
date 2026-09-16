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

import { useApiScoreEnabled } from './useApiScoreEnabled';
import { getEnvironmentPortalConfiguration } from '../../settings/services/portalSettings';

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    useEnvironment: jest.fn(() => ({ id: 'DEFAULT' })),
}));

jest.mock('../../settings/services/portalSettings', () => ({
    getEnvironmentPortalConfiguration: jest.fn(),
}));

const mockUseEnvironment = useEnvironment as jest.MockedFunction<typeof useEnvironment>;
const mockGetPortal = getEnvironmentPortalConfiguration as jest.MockedFunction<typeof getEnvironmentPortalConfiguration>;

function createWrapper() {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    return function Wrapper({ children }: { children: ReactNode }) {
        return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
    };
}

describe('useApiScoreEnabled', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        mockUseEnvironment.mockReturnValue({ id: 'DEFAULT' } as ReturnType<typeof useEnvironment>);
    });

    it('is enabled when GET /portal reports apiScore.enabled', async () => {
        mockGetPortal.mockResolvedValue({ apiScore: { enabled: true } });
        const { result } = renderHook(() => useApiScoreEnabled(), { wrapper: createWrapper() });

        await waitFor(() => expect(result.current.isFetched).toBe(true));
        expect(mockGetPortal).toHaveBeenCalledWith('DEFAULT');
        expect(result.current.enabled).toBe(true);
    });

    it('is disabled when the portal flag is off', async () => {
        mockGetPortal.mockResolvedValue({ apiScore: { enabled: false } });
        const { result } = renderHook(() => useApiScoreEnabled(), { wrapper: createWrapper() });

        await waitFor(() => expect(result.current.isFetched).toBe(true));
        expect(result.current.enabled).toBe(false);
    });
});
