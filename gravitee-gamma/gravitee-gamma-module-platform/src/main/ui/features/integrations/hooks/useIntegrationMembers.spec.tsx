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

import { useIntegrationMembers } from './useIntegrationMembers';
import { listIntegrationMembers } from '../services/integrationMembers';

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    useEnvironment: jest.fn(),
}));

jest.mock('../services/integrationMembers');

const mockUseEnvironment = jest.mocked(useEnvironment);
const mockListIntegrationMembers = jest.mocked(listIntegrationMembers);

function wrapper({ children }: { children: ReactNode }) {
    const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    return <QueryClientProvider client={client}>{children}</QueryClientProvider>;
}

describe('useIntegrationMembers', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        mockUseEnvironment.mockReturnValue({ id: 'env-1' });
    });

    it('lists the members of the integration in the current environment', async () => {
        const members = [{ id: 'user-1', displayName: 'Jane Doe' }];
        mockListIntegrationMembers.mockResolvedValue(members);

        const { result } = renderHook(() => useIntegrationMembers('int-1'), { wrapper });

        await waitFor(() => expect(result.current.data).toEqual(members));
        expect(mockListIntegrationMembers).toHaveBeenCalledWith('env-1', 'int-1');
    });

    it('surfaces a failed members listing as the query error', async () => {
        const failure = new Error('Internal error');
        mockListIntegrationMembers.mockRejectedValue(failure);

        const { result } = renderHook(() => useIntegrationMembers('int-1'), { wrapper });

        await waitFor(() => expect(result.current.isError).toBe(true));
        expect(result.current.error).toBe(failure);
    });

    it('stays idle and does not request the members when the integration id is empty', () => {
        const { result } = renderHook(() => useIntegrationMembers(''), { wrapper });

        expect(result.current.fetchStatus).toBe('idle');
        expect(mockListIntegrationMembers).not.toHaveBeenCalled();
    });
});
