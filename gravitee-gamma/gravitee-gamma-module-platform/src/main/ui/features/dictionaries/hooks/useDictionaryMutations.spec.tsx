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

import { useUpdateDictionaryProperties } from './useDictionaryMutations';
import { getEnvironmentDictionary, updateEnvironmentDictionary } from '../services/dictionaries';
import type { Dictionary } from '../types/dictionary';

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    useEnvironment: jest.fn(),
}));

jest.mock('../services/dictionaries', () => ({
    getEnvironmentDictionary: jest.fn(),
    updateEnvironmentDictionary: jest.fn(),
}));

const mockGet = jest.mocked(getEnvironmentDictionary);
const mockUpdate = jest.mocked(updateEnvironmentDictionary);

const FRESH: Dictionary = { id: 'd1', name: 'Remote', type: 'DYNAMIC', properties: { a: '1', addedByProvider: '2' } };

function renderUpdateProperties() {
    jest.mocked(useEnvironment).mockReturnValue({ id: 'env-1' } as ReturnType<typeof useEnvironment>);
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    function Wrapper({ children }: { children: ReactNode }) {
        return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
    }
    return renderHook(() => useUpdateDictionaryProperties(), { wrapper: Wrapper });
}

describe('useUpdateDictionaryProperties', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        mockUpdate.mockResolvedValue(FRESH);
    });

    it('applies the change to the dictionary it just re-read, then writes it', async () => {
        mockGet.mockResolvedValue(FRESH);
        const change = jest.fn((current: Dictionary) => ({ name: current.name, type: current.type, properties: current.properties }));
        const { result } = renderUpdateProperties();

        await act(() => result.current.mutateAsync({ dictionaryId: 'd1', change }));

        expect(mockGet).toHaveBeenCalledWith('env-1', 'd1');
        expect(change).toHaveBeenCalledWith(FRESH);
        expect(mockUpdate).toHaveBeenCalledWith('env-1', 'd1', { name: 'Remote', type: 'DYNAMIC', properties: FRESH.properties });
        expect(mockGet.mock.invocationCallOrder[0]).toBeLessThan(mockUpdate.mock.invocationCallOrder[0]);
    });

    it('writes nothing when the re-read fails', async () => {
        mockGet.mockRejectedValue(new Error('boom'));
        const change = jest.fn();
        const { result } = renderUpdateProperties();

        await act(async () => {
            await expect(result.current.mutateAsync({ dictionaryId: 'd1', change })).rejects.toThrow('boom');
        });

        expect(change).not.toHaveBeenCalled();
        expect(mockUpdate).not.toHaveBeenCalled();
    });
});
