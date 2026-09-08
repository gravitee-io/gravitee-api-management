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

import { useEnvironmentHealthAvailability } from './useEnvironmentHealthAvailability';
import { getApiAvailability } from '../services/environmentHealthApis';

jest.mock('@gravitee/gamma-modules-sdk', () => ({ useEnvironment: jest.fn() }));
jest.mock('../services/environmentHealthApis', () => ({ getApiAvailability: jest.fn() }));

const mockUseEnvironment = jest.mocked(useEnvironment);
const mockGetApiAvailability = jest.mocked(getApiAvailability);

function createTestContext() {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    function Wrapper({ children }: { children: ReactNode }) {
        return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
    }
    return { Wrapper };
}

describe('useEnvironmentHealthAvailability', () => {
    beforeEach(() => {
        mockUseEnvironment.mockReturnValue({ id: 'env-1' });
        mockGetApiAvailability.mockResolvedValue({ global: 0.88, group: { default: 0.88 } });
    });

    afterEach(() => jest.clearAllMocks());

    it('skips the availability GET when health check is not configured', () => {
        const { Wrapper } = createTestContext();
        renderHook(() => useEnvironmentHealthAvailability({ apiId: 'api-1', from: 1, to: 2, enabled: false }), {
            wrapper: Wrapper,
        });

        expect(mockGetApiAvailability).not.toHaveBeenCalled();
    });

    it('loads configured availability when health check is enabled', async () => {
        const { Wrapper } = createTestContext();
        const { result } = renderHook(() => useEnvironmentHealthAvailability({ apiId: 'api-1', from: 1, to: 2, enabled: true }), {
            wrapper: Wrapper,
        });

        await waitFor(() => expect(mockGetApiAvailability).toHaveBeenCalledWith('env-1', 'api-1', 1, 2, expect.anything()));
        await waitFor(() => expect(result.current.availability).toEqual({ type: 'configured', availabilityPct: 88 }));
    });
});
