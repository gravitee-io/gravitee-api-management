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

import { useIntegrationEnvironmentGroups } from './useIntegrationEnvironmentGroups';
import { listEnvironmentGroups } from '../../shared/services/groupMembers';
import type { EnvironmentGroup } from '../../shared/types/groupMembers';

jest.mock('@gravitee/gamma-modules-sdk', () => ({ useEnvironment: jest.fn() }));
jest.mock('../../shared/services/groupMembers', () => ({ listEnvironmentGroups: jest.fn() }));

const mockUseEnvironment = jest.mocked(useEnvironment);
const mockListEnvironmentGroups = jest.mocked(listEnvironmentGroups);

const ENVIRONMENT_GROUPS: EnvironmentGroup[] = [
    { id: 'group-1', name: 'Developers' },
    { id: 'group-2', name: 'Operators' },
];

function wrapper({ children }: { children: ReactNode }) {
    const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    return <QueryClientProvider client={client}>{children}</QueryClientProvider>;
}

describe('useIntegrationEnvironmentGroups', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        mockUseEnvironment.mockReturnValue({ id: 'DEFAULT' } as ReturnType<typeof useEnvironment>);
    });

    it('lists the groups of the current environment', async () => {
        mockListEnvironmentGroups.mockResolvedValue(ENVIRONMENT_GROUPS);

        const { result } = renderHook(() => useIntegrationEnvironmentGroups(), { wrapper });

        await waitFor(() => expect(result.current.data).toEqual(ENVIRONMENT_GROUPS));
        expect(mockListEnvironmentGroups).toHaveBeenCalledWith('DEFAULT');
    });

    it('stays idle without listing groups when disabled', () => {
        const { result } = renderHook(() => useIntegrationEnvironmentGroups({ enabled: false }), { wrapper });

        expect(mockListEnvironmentGroups).not.toHaveBeenCalled();
        expect(result.current.fetchStatus).toBe('idle');
        expect(result.current.data).toBeUndefined();
    });

    it('stays idle without listing groups when no environment is selected', () => {
        mockUseEnvironment.mockReturnValue(undefined as unknown as ReturnType<typeof useEnvironment>);

        const { result } = renderHook(() => useIntegrationEnvironmentGroups(), { wrapper });

        expect(mockListEnvironmentGroups).not.toHaveBeenCalled();
        expect(result.current.fetchStatus).toBe('idle');
    });

    it('surfaces a failed groups listing as the query error', async () => {
        const failure = new Error('Internal error');
        mockListEnvironmentGroups.mockRejectedValue(failure);

        const { result } = renderHook(() => useIntegrationEnvironmentGroups(), { wrapper });

        await waitFor(() => expect(result.current.isError).toBe(true));
        expect(result.current.error).toBe(failure);
        expect(result.current.data).toBeUndefined();
    });
});
