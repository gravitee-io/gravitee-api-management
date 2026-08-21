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

import { useAuthenticationPage } from './useAuthenticationPage';
import { listActivatedIdentityProviders, listIdentityProviders } from '../services/identityProviders';

jest.mock('../services/identityProviders', () => ({
    listIdentityProviders: jest.fn(),
    listActivatedIdentityProviders: jest.fn(),
}));

const mockListIdentityProviders = jest.mocked(listIdentityProviders);
const mockListActivatedIdentityProviders = jest.mocked(listActivatedIdentityProviders);

const PROVIDERS = [
    {
        id: 'google-idp',
        name: 'Google',
        description: '',
        enabled: true,
        sync: false,
        type: 'GOOGLE' as const,
        created_at: 1,
        updated_at: 1,
    },
];

function createTestContext() {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    function Wrapper({ children }: { children: ReactNode }) {
        return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
    }
    return { Wrapper };
}

describe('useAuthenticationPage', () => {
    beforeEach(() => {
        mockListIdentityProviders.mockResolvedValue(PROVIDERS);
        mockListActivatedIdentityProviders.mockResolvedValue([{ identityProvider: 'google-idp' }]);
    });

    afterEach(() => {
        jest.clearAllMocks();
    });

    it('loads providers and activations as independent queries', async () => {
        const { Wrapper } = createTestContext();
        const { result } = renderHook(() => useAuthenticationPage(), { wrapper: Wrapper });

        await waitFor(() => expect(result.current.providersQuery.isSuccess).toBe(true));
        await waitFor(() => expect(result.current.activationsQuery.isSuccess).toBe(true));
        expect(result.current.providersQuery.data).toEqual(PROVIDERS);
        expect(result.current.activationsQuery.data).toEqual([{ identityProvider: 'google-idp' }]);
        expect(mockListIdentityProviders).toHaveBeenCalledTimes(1);
        expect(mockListActivatedIdentityProviders).toHaveBeenCalledTimes(1);
    });

    it('keeps providers available when activations cannot be loaded', async () => {
        mockListActivatedIdentityProviders.mockRejectedValue(new Error('activations down'));
        const { Wrapper } = createTestContext();
        const { result } = renderHook(() => useAuthenticationPage(), { wrapper: Wrapper });

        await waitFor(() => expect(result.current.providersQuery.isSuccess).toBe(true));
        await waitFor(() => expect(result.current.activationsQuery.isError).toBe(true));
        expect(result.current.providersQuery.data).toEqual(PROVIDERS);
    });
});
