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
jest.mock('../services/userFields', () => ({
    createUserField: jest.fn(),
    updateUserField: jest.fn(),
    deleteUserField: jest.fn(),
}));

import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { renderHook, waitFor } from '@testing-library/react';
import type { ReactNode } from 'react';

import { useCreateUserField, useDeleteUserField, useUpdateUserField } from './useUserFieldMutations';
import { createUserField, deleteUserField, updateUserField } from '../services/userFields';
import { userFieldKeys } from '../utils/queryKeys';

const mockCreateUserField = jest.mocked(createUserField);
const mockUpdateUserField = jest.mocked(updateUserField);
const mockDeleteUserField = jest.mocked(deleteUserField);

const PAYLOAD = { key: 'department', label: 'Department', required: false, values: [] };

function makeWrapper() {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } });
    const invalidateSpy = jest.spyOn(queryClient, 'invalidateQueries');
    const wrapper = ({ children }: { children: ReactNode }) => <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
    return { wrapper, invalidateSpy };
}

describe('user field mutations', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        mockCreateUserField.mockResolvedValue({ ...PAYLOAD });
        mockUpdateUserField.mockResolvedValue({ ...PAYLOAD });
        mockDeleteUserField.mockResolvedValue(undefined);
    });

    it('creates through the service and refreshes the list', async () => {
        const { wrapper, invalidateSpy } = makeWrapper();
        const { result } = renderHook(() => useCreateUserField(), { wrapper });

        await result.current.mutateAsync(PAYLOAD);

        expect(mockCreateUserField).toHaveBeenCalledWith(PAYLOAD);
        await waitFor(() => expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: userFieldKeys.list() }));
    });

    it('updates through the service and refreshes the list', async () => {
        const { wrapper, invalidateSpy } = makeWrapper();
        const { result } = renderHook(() => useUpdateUserField(), { wrapper });

        await result.current.mutateAsync(PAYLOAD);

        expect(mockUpdateUserField).toHaveBeenCalledWith(PAYLOAD);
        await waitFor(() => expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: userFieldKeys.list() }));
    });

    it('deletes by key through the service and refreshes the list', async () => {
        const { wrapper, invalidateSpy } = makeWrapper();
        const { result } = renderHook(() => useDeleteUserField(), { wrapper });

        await result.current.mutateAsync('department');

        expect(mockDeleteUserField).toHaveBeenCalledWith('department');
        await waitFor(() => expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: userFieldKeys.list() }));
    });

    it('does not refresh the list when the service rejects', async () => {
        const { wrapper, invalidateSpy } = makeWrapper();
        mockCreateUserField.mockRejectedValue(new Error('CustomUserField [department] already exists.'));
        const { result } = renderHook(() => useCreateUserField(), { wrapper });

        await expect(result.current.mutateAsync(PAYLOAD)).rejects.toThrow('already exists');

        expect(invalidateSpy).not.toHaveBeenCalled();
    });
});
