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
import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';

import { DictionariesPage } from './DictionariesPage';
import { useCreateDictionary, useDeleteDictionary, useUpdateDictionary } from '../features/dictionaries/hooks/useDictionaryMutations';
import { useDictionaryPermissions } from '../features/dictionaries/hooks/useDictionaryPermissions';
import { useEnvironmentDictionaries } from '../features/dictionaries/hooks/useEnvironmentDictionaries';
import { useEnvironmentDictionary } from '../features/dictionaries/hooks/useEnvironmentDictionary';
import { ApimApiError } from '../shared/api/apimClient';

const mockNavigate = jest.fn();

jest.mock('react-router-dom', () => ({
    ...jest.requireActual('react-router-dom'),
    useNavigate: () => mockNavigate,
}));

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    useEnvironment: jest.fn(),
}));

jest.mock('../features/dictionaries/hooks/useDictionaryPermissions');
jest.mock('../features/dictionaries/hooks/useEnvironmentDictionaries');
jest.mock('../features/dictionaries/hooks/useEnvironmentDictionary');
jest.mock('../features/dictionaries/hooks/useDictionaryMutations');

jest.mock('../features/dictionaries/components/DictionariesTable', () => ({
    DictionariesTable: () => <div data-testid="dictionaries-table" />,
}));
jest.mock('../features/dictionaries/components/DictionariesEmptyState', () => ({
    DictionariesEmptyState: () => <div data-testid="dictionaries-empty-state" />,
}));
jest.mock('../features/dictionaries/components/CreateDictionarySheet', () => ({
    CreateDictionarySheet: () => null,
}));
jest.mock('../features/dictionaries/components/EditDictionarySheet', () => ({
    EditDictionarySheet: () => null,
}));
jest.mock('../features/dictionaries/components/DictionaryDeleteSheet', () => ({
    DictionaryDeleteSheet: () => null,
}));

const mockUseEnvironment = jest.mocked(useEnvironment);
const mockUseDictionaryPermissions = jest.mocked(useDictionaryPermissions);
const mockUseEnvironmentDictionaries = jest.mocked(useEnvironmentDictionaries);
const mockUseEnvironmentDictionary = jest.mocked(useEnvironmentDictionary);
const mockUseCreateDictionary = jest.mocked(useCreateDictionary);
const mockUseUpdateDictionary = jest.mocked(useUpdateDictionary);
const mockUseDeleteDictionary = jest.mocked(useDeleteDictionary);

// eslint-disable-next-line @typescript-eslint/no-explicit-any
function makeMutation(): any {
    return { mutateAsync: jest.fn(), isPending: false };
}

function renderPage(seedPermissions: string[] = []) {
    const queryClient = new QueryClient();
    queryClient.setQueryData(['environment-permissions', 'env-1'], seedPermissions);
    const invalidateSpy = jest.spyOn(queryClient, 'invalidateQueries');
    render(
        <QueryClientProvider client={queryClient}>
            <MemoryRouter initialEntries={['/dictionaries']}>
                <DictionariesPage />
            </MemoryRouter>
        </QueryClientProvider>,
    );
    return { invalidateSpy, queryClient };
}

describe('DictionariesPage', () => {
    beforeEach(() => {
        mockNavigate.mockClear();
        mockUseEnvironment.mockReturnValue({ id: 'env-1' } as ReturnType<typeof useEnvironment>);
        mockUseDictionaryPermissions.mockReturnValue({ canRead: true, canCreate: true, canUpdate: true, canDelete: true });
        mockUseEnvironmentDictionary.mockReturnValue({ data: undefined, isLoading: false } as ReturnType<typeof useEnvironmentDictionary>);
        mockUseCreateDictionary.mockReturnValue(makeMutation());
        mockUseUpdateDictionary.mockReturnValue(makeMutation());
        mockUseDeleteDictionary.mockReturnValue(makeMutation());
    });

    it('renders the table when dictionaries load successfully', () => {
        mockUseEnvironmentDictionaries.mockReturnValue({
            data: [{ id: '1', name: 'Countries', type: 'MANUAL' }],
            isLoading: false,
            isError: false,
            error: null,
        } as ReturnType<typeof useEnvironmentDictionaries>);

        renderPage();

        expect(screen.getByTestId('dictionaries-table')).not.toBeNull();
        expect(mockNavigate).not.toHaveBeenCalled();
    });

    it('shows the empty state, not the table, when the dictionaries list is empty', () => {
        mockUseEnvironmentDictionaries.mockReturnValue({
            data: [],
            isLoading: false,
            isError: false,
            error: null,
        } as ReturnType<typeof useEnvironmentDictionaries>);

        renderPage();

        expect(screen.getByTestId('dictionaries-empty-state')).not.toBeNull();
        expect(screen.queryByTestId('dictionaries-table')).toBeNull();
    });

    it('shows a generic error message on a non-403 failure, without navigating away', () => {
        mockUseEnvironmentDictionaries.mockReturnValue({
            data: undefined,
            isLoading: false,
            isError: true,
            error: new ApimApiError(500, 'Internal Server Error'),
        } as ReturnType<typeof useEnvironmentDictionaries>);

        renderPage();

        expect(screen.getByText('Failed to load dictionaries. Please refresh and try again.')).not.toBeNull();
        expect(mockNavigate).not.toHaveBeenCalled();
    });

    it('redirects away and strips dictionary permissions from the cache on a 403, even if the backend still grants them', async () => {
        mockUseEnvironmentDictionaries.mockReturnValue({
            data: undefined,
            isLoading: false,
            isError: true,
            error: new ApimApiError(403, 'Forbidden'),
        } as ReturnType<typeof useEnvironmentDictionaries>);

        // Simulate the exact bug reported: the permissions map still says the user can read/write
        // dictionaries (e.g. a stale backend-side cache), even though the live resource call 403s.
        const { invalidateSpy, queryClient } = renderPage([
            'environment-metadata-r',
            'environment-dictionary-r',
            'environment-dictionary-c',
            'environment-dictionary-u',
            'environment-dictionary-d',
        ]);

        await waitFor(() => expect(mockNavigate).toHaveBeenCalledWith('../applications', { replace: true }));

        // The cache is patched directly — hiding the nav item doesn't depend on the backend's next answer.
        expect(queryClient.getQueryData(['environment-permissions', 'env-1'])).toEqual(['environment-metadata-r']);
        // Deliberately not invalidated: refetching could just restore the same stale grant that caused the 403.
        expect(invalidateSpy).not.toHaveBeenCalled();
        expect(screen.queryByText('Failed to load dictionaries. Please refresh and try again.')).toBeNull();
        expect(screen.queryByTestId('dictionaries-table')).toBeNull();
    });
});
