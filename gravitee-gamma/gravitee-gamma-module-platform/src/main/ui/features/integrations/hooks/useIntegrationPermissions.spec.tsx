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
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { renderHook, waitFor } from '@testing-library/react';
import type { ReactNode } from 'react';

import { useEnvironment } from '@gravitee/gamma-modules-sdk';

import { useIntegrationPermissions } from './useIntegrationPermissions';
import { getIntegrationPermissions } from '../services/integrationPermissions';

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    useEnvironment: jest.fn(),
}));

jest.mock('../services/integrationPermissions');

const mockUseEnvironment = jest.mocked(useEnvironment);
const mockGetIntegrationPermissions = jest.mocked(getIntegrationPermissions);

function createWrapper(client: QueryClient) {
    return function Wrapper({ children }: { children: ReactNode }) {
        return <QueryClientProvider client={client}>{children}</QueryClientProvider>;
    };
}

describe('useIntegrationPermissions', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        mockUseEnvironment.mockReturnValue({ id: 'DEFAULT' });
        mockGetIntegrationPermissions.mockResolvedValue(['integration-definition-r']);
    });

    it('reuses cached permissions when remounted with the same environment and integration', async () => {
        const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });

        const first = renderHook(() => useIntegrationPermissions('integration-1'), { wrapper: createWrapper(client) });
        await waitFor(() => expect(first.result.current.isSuccess).toBe(true));
        first.unmount();

        const second = renderHook(() => useIntegrationPermissions('integration-1'), { wrapper: createWrapper(client) });
        await waitFor(() => expect(second.result.current.isSuccess).toBe(true));

        expect(mockGetIntegrationPermissions).toHaveBeenCalledTimes(1);
        expect(mockGetIntegrationPermissions).toHaveBeenCalledWith('DEFAULT', 'integration-1');
        expect(second.result.current.data).toEqual(['integration-definition-r']);
    });
});
