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

import { useEnvironmentHealthReport } from './useEnvironmentHealthReport';
import { fetchEnvironmentHealthReport } from '../services/environmentHealthApis';

jest.mock('@gravitee/gamma-modules-sdk', () => ({ useEnvironment: jest.fn() }));
jest.mock('../services/environmentHealthApis', () => ({ fetchEnvironmentHealthReport: jest.fn() }));

const mockUseEnvironment = jest.mocked(useEnvironment);
const mockFetchReport = jest.mocked(fetchEnvironmentHealthReport);

function createTestContext() {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    function Wrapper({ children }: { children: ReactNode }) {
        return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
    }
    return { Wrapper };
}

describe('useEnvironmentHealthReport', () => {
    beforeEach(() => {
        mockUseEnvironment.mockReturnValue({ id: 'env-1' });
        mockFetchReport.mockResolvedValue({ operational: 1, inWarning: 0, inError: 0 });
    });

    afterEach(() => jest.clearAllMocks());

    it('loads the report for the timeframe window and does not take a table search argument', async () => {
        const { Wrapper } = createTestContext();
        renderHook(() => useEnvironmentHealthReport({ from: 10, to: 20, reloadToken: 0 }), { wrapper: Wrapper });

        await waitFor(() => expect(mockFetchReport).toHaveBeenCalledWith('env-1', 10, 20, expect.anything()));
        expect(mockFetchReport.mock.calls[0]?.length).toBe(4);
    });
});
