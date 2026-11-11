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
import { act, renderHook } from '@testing-library/react';
import type { ReactNode } from 'react';

import { useEnvironment } from '@gravitee/gamma-modules-sdk';

import { useCreateCredential, useDeleteCredential, useRotateCredential, useUpdateCredential } from './useCredentialMutations';
import { retryTransientRequest } from '../../../shared/api/queryRetry';
import { createCredential, deleteCredential, rotateCredential, updateCredential } from '../services/credentials';
import { credentialKeys } from '../utils/queryKeys';

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    useEnvironment: jest.fn(),
}));
jest.mock('../services/credentials', () => ({
    createCredential: jest.fn(),
    updateCredential: jest.fn(),
    rotateCredential: jest.fn(),
    deleteCredential: jest.fn(),
}));

describe('credential mutations', () => {
    let queryClient: QueryClient;

    beforeEach(() => {
        queryClient = new QueryClient({ defaultOptions: { mutations: { retry: retryTransientRequest, retryDelay: 0 } } });
        jest.mocked(useEnvironment).mockReturnValue({ id: 'env-1' } as ReturnType<typeof useEnvironment>);
    });

    afterEach(() => {
        queryClient.clear();
        jest.clearAllMocks();
    });

    function wrapper({ children }: { children: ReactNode }) {
        return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
    }

    it.each([
        ['a creation', useCreateCredential, createCredential, { name: 'OpenAI', strategy: 'BEARER' }],
        ['an update', useUpdateCredential, updateCredential, { id: 'cred-1', payload: { name: 'OpenAI' } }],
        ['a rotation', useRotateCredential, rotateCredential, { id: 'cred-1', payload: { values: { token: 'sk-2' } } }],
        ['a deletion', useDeleteCredential, deleteCredential, 'cred-1'],
    ] as const)('does not repeat %s that failed with a server error', async (_, useHook, service, variables) => {
        const failure = Object.assign(new Error('Server error'), { status: 500 });
        jest.mocked(service).mockRejectedValue(failure);
        const { result } = renderHook(() => (useHook as () => { mutateAsync: (v: unknown) => Promise<unknown> })(), { wrapper });

        await act(async () => {
            await expect(result.current.mutateAsync(variables)).rejects.toBe(failure);
        });

        expect(service).toHaveBeenCalledTimes(1);
    });

    it('drops the queries of a deleted credential instead of reloading them, and reloads the lists', async () => {
        jest.mocked(deleteCredential).mockResolvedValue(undefined);
        const list = credentialKeys.list('env-1', '', 1, 25);
        const detail = credentialKeys.detail('env-1', 'cred-1');
        const consumers = credentialKeys.consumers('env-1', 'cred-1');
        const owner = credentialKeys.owner('user-1');
        queryClient.setQueryData(list, { data: [] });
        queryClient.setQueryData(detail, { id: 'cred-1' });
        queryClient.setQueryData(consumers, []);
        queryClient.setQueryData(owner, { id: 'user-1' });
        const { result } = renderHook(() => useDeleteCredential(), { wrapper });

        await act(async () => {
            await result.current.mutateAsync('cred-1');
        });

        expect(queryClient.getQueryState(detail)).toBeUndefined();
        expect(queryClient.getQueryState(consumers)).toBeUndefined();
        expect(queryClient.getQueryState(list)?.isInvalidated).toBe(true);
        expect(queryClient.getQueryState(owner)?.isInvalidated).toBe(false);
    });
});
