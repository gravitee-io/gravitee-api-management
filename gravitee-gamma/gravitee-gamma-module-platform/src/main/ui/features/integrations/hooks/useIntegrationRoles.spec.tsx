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

import { useIntegrationDefaultRole, useIntegrationRoles } from './useIntegrationRoles';
import { listIntegrationRoles } from '../services/integrationMembers';
import type { IntegrationRole } from '../types/integrationMembers';

jest.mock('../services/integrationMembers', () => ({ listIntegrationRoles: jest.fn() }));

const mockListIntegrationRoles = jest.mocked(listIntegrationRoles);

function wrapper({ children }: { children: ReactNode }) {
    const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    return <QueryClientProvider client={client}>{children}</QueryClientProvider>;
}

describe('useIntegrationRoles', () => {
    beforeEach(() => {
        jest.clearAllMocks();
    });

    it.each<[scenario: string, listedRoles: string[], expectedRoles: string[]]>([
        ['the default Integration roles', ['PRIMARY_OWNER', 'OWNER', 'USER'], ['OWNER', 'USER']],
        ['a custom Integration role next to the defaults', ['PRIMARY_OWNER', 'OWNER', 'USER', 'REVIEWER'], ['OWNER', 'USER', 'REVIEWER']],
    ])('offers every listed Integration role except Primary Owner for %s', async (_scenario, listedRoles, expectedRoles) => {
        mockListIntegrationRoles.mockResolvedValue(listedRoles.map(name => ({ name })));

        const { result } = renderHook(() => useIntegrationRoles(), { wrapper });

        await waitFor(() => expect(result.current.data).toEqual(expectedRoles));
    });

    it('surfaces a failed roles listing as the query error', async () => {
        const failure = new Error('Internal error');
        mockListIntegrationRoles.mockRejectedValue(failure);

        const { result } = renderHook(() => useIntegrationRoles(), { wrapper });

        await waitFor(() => expect(result.current.isError).toBe(true));
        expect(result.current.error).toBe(failure);
        expect(result.current.data).toBeUndefined();
    });
});

describe('useIntegrationDefaultRole', () => {
    beforeEach(() => {
        jest.clearAllMocks();
    });

    it('preselects the first role flagged default, skipping a Primary Owner flagged default', async () => {
        mockListIntegrationRoles.mockResolvedValue([
            { name: 'PRIMARY_OWNER', default: true },
            { name: 'OWNER' },
            { name: 'USER', default: true },
        ]);

        const { result } = renderHook(() => useIntegrationDefaultRole(), { wrapper });

        await waitFor(() => expect(result.current.data).toBe('USER'));
    });

    it.each<[scenario: string, listedRoles: IntegrationRole[]]>([
        ['only Primary Owner is flagged default', [{ name: 'PRIMARY_OWNER', default: true }, { name: 'OWNER' }, { name: 'USER' }]],
        ['no role is flagged default', [{ name: 'PRIMARY_OWNER' }, { name: 'OWNER' }, { name: 'USER' }]],
    ])('preselects no role when %s', async (_scenario, listedRoles) => {
        mockListIntegrationRoles.mockResolvedValue(listedRoles);

        const { result } = renderHook(() => useIntegrationDefaultRole(), { wrapper });

        await waitFor(() => expect(result.current.isSuccess).toBe(true));
        expect(result.current.data).toBeUndefined();
    });
});
