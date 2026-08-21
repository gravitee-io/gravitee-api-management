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

import { buttonHarness, renderWithGraphene } from '@gravitee/graphene-core/testing';
import { screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';

import { EditIdentityProviderPage } from './EditIdentityProviderPage';
import { useIdentityProvider, useIdentityProviderMappingCatalog } from '../features/authentication/hooks/useIdentityProvider';
import type { IdentityProvider } from '../features/authentication/types/identityProvider';
import { ApimApiError } from '../shared/api/apimClient';
import { useForbiddenResourceRedirect } from '../shared/hooks/useForbiddenResourceRedirect';

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    useHasPermission: () => true,
}));

jest.mock('../features/authentication/hooks/useIdentityProvider');
jest.mock('../shared/hooks/useForbiddenResourceRedirect');

let mockFormDirty = false;

jest.mock('../features/authentication/components/IdentityProviderEditForm', () => {
    const { useEffect } = jest.requireActual('react');
    return {
        IdentityProviderEditForm: ({
            mappingsDisabled,
            onDirtyChange,
        }: {
            mappingsDisabled?: boolean;
            onDirtyChange?: (dirty: boolean) => void;
        }) => {
            useEffect(() => {
                onDirtyChange?.(mockFormDirty);
            }, [onDirtyChange]);
            return <div data-testid="identity-provider-edit-form" data-mappings-disabled={String(Boolean(mappingsDisabled))} />;
        },
    };
});

const mockUseIdentityProvider = jest.mocked(useIdentityProvider);
const mockUseIdentityProviderMappingCatalog = jest.mocked(useIdentityProviderMappingCatalog);

const PROVIDER: IdentityProvider = {
    id: 'google-idp',
    name: 'Google',
    type: 'GOOGLE',
    enabled: true,
    configuration: { clientId: 'id', clientSecret: 'secret' },
    groupMappings: [],
    roleMappings: [],
};

function emptyCatalog() {
    return {
        groupsQuery: { data: [], isLoading: false, isError: false },
        environmentsQuery: { data: [], isLoading: false, isError: false },
        organizationRolesQuery: { data: [], isLoading: false, isError: false },
        environmentRolesQuery: { data: [], isLoading: false, isError: false },
        refetchCatalogs: jest.fn(),
    } as unknown as ReturnType<typeof useIdentityProviderMappingCatalog>;
}

function renderPage() {
    return renderWithGraphene(
        <MemoryRouter initialEntries={['/authentication/google-idp']}>
            <Routes>
                <Route path="/authentication/:identityProviderId" element={<EditIdentityProviderPage />} />
                <Route path="/authentication" element={<div data-testid="authentication-list" />} />
            </Routes>
        </MemoryRouter>,
    );
}

describe('EditIdentityProviderPage', () => {
    beforeAll(() => {
        Object.defineProperty(window, 'matchMedia', {
            writable: true,
            value: jest.fn().mockImplementation((query: string) => ({
                matches: false,
                media: query,
                onchange: null,
                addListener: jest.fn(),
                removeListener: jest.fn(),
                addEventListener: jest.fn(),
                removeEventListener: jest.fn(),
                dispatchEvent: jest.fn(),
            })),
        });
    });

    beforeEach(() => {
        mockFormDirty = false;
        mockUseIdentityProviderMappingCatalog.mockReturnValue(emptyCatalog());
        jest.mocked(useForbiddenResourceRedirect).mockImplementation(() => undefined);
    });

    it('renders the edit form for a loaded provider', () => {
        mockUseIdentityProvider.mockReturnValue({
            data: PROVIDER,
            isLoading: false,
            isError: false,
            error: null,
        } as ReturnType<typeof useIdentityProvider>);
        renderPage();
        expect(screen.getByRole('heading', { name: 'Update Google identity provider' })).not.toBeNull();
        expect(screen.getByTestId('identity-provider-edit-form')).not.toBeNull();
        expect(screen.getByTestId('identity-provider-edit-form').getAttribute('data-mappings-disabled')).toBe('false');
    });

    it('waits for environments before mounting the form', () => {
        mockUseIdentityProvider.mockReturnValue({
            data: PROVIDER,
            isLoading: false,
            isError: false,
            error: null,
        } as ReturnType<typeof useIdentityProvider>);
        mockUseIdentityProviderMappingCatalog.mockReturnValue({
            ...emptyCatalog(),
            environmentsQuery: { data: undefined, isLoading: true, isError: false },
        } as ReturnType<typeof useIdentityProviderMappingCatalog>);
        renderPage();
        expect(screen.queryByTestId('identity-provider-edit-form')).toBeNull();
        expect(screen.queryByRole('heading', { name: 'Update Google identity provider' })).toBeNull();
    });

    it('mounts the form while group and role catalogs are still loading', () => {
        mockUseIdentityProvider.mockReturnValue({
            data: PROVIDER,
            isLoading: false,
            isError: false,
            error: null,
        } as ReturnType<typeof useIdentityProvider>);
        mockUseIdentityProviderMappingCatalog.mockReturnValue({
            ...emptyCatalog(),
            groupsQuery: { data: undefined, isLoading: true, isError: false },
            organizationRolesQuery: { data: undefined, isLoading: true, isError: false },
            environmentRolesQuery: { data: undefined, isLoading: true, isError: false },
        } as ReturnType<typeof useIdentityProviderMappingCatalog>);
        renderPage();
        expect(screen.getByTestId('identity-provider-edit-form')).not.toBeNull();
        expect(screen.getByTestId('identity-provider-edit-form').getAttribute('data-mappings-disabled')).toBe('true');
        expect(screen.queryByText('Groups and roles could not be loaded')).toBeNull();
    });

    it('shows a not-found message when the provider cannot be loaded', () => {
        mockUseIdentityProvider.mockReturnValue({
            data: undefined,
            isLoading: false,
            isError: true,
            error: new ApimApiError(404, 'Not found'),
        } as unknown as ReturnType<typeof useIdentityProvider>);
        renderPage();
        expect(screen.getByText('Identity provider not found or failed to load.')).not.toBeNull();
        expect(screen.queryByTestId('identity-provider-edit-form')).toBeNull();
    });

    it('redirects when the provider request is forbidden', () => {
        mockUseIdentityProvider.mockReturnValue({
            data: undefined,
            isLoading: false,
            isError: true,
            error: new ApimApiError(403, 'Forbidden'),
        } as unknown as ReturnType<typeof useIdentityProvider>);
        renderPage();
        expect(useForbiddenResourceRedirect).toHaveBeenCalledWith(expect.objectContaining({ isForbidden: true, redirectTo: '..' }));
        expect(screen.queryByTestId('identity-provider-edit-form')).toBeNull();
    });

    it('keeps the edit form available when mapping catalogs fail', async () => {
        const refetchCatalogs = jest.fn();
        mockUseIdentityProvider.mockReturnValue({
            data: PROVIDER,
            isLoading: false,
            isError: false,
            error: null,
        } as ReturnType<typeof useIdentityProvider>);
        mockUseIdentityProviderMappingCatalog.mockReturnValue({
            ...emptyCatalog(),
            groupsQuery: { data: undefined, isLoading: false, isError: true },
            refetchCatalogs,
        } as unknown as ReturnType<typeof useIdentityProviderMappingCatalog>);
        renderPage();
        expect(screen.getByRole('heading', { name: 'Update Google identity provider' })).not.toBeNull();
        expect(screen.getByTestId('identity-provider-edit-form')).not.toBeNull();
        expect(screen.getByTestId('identity-provider-edit-form').getAttribute('data-mappings-disabled')).toBe('true');
        expect(screen.getByText('Groups and roles could not be loaded')).not.toBeNull();
        await buttonHarness({ name: 'Retry' }).click();
        expect(refetchCatalogs).toHaveBeenCalledTimes(1);
    });

    it('asks to confirm before leaving a dirty form from Back', async () => {
        mockFormDirty = true;
        mockUseIdentityProvider.mockReturnValue({
            data: PROVIDER,
            isLoading: false,
            isError: false,
            error: null,
        } as ReturnType<typeof useIdentityProvider>);
        renderPage();
        await buttonHarness({ name: 'Back to Authentication' }).click();
        expect(screen.getByText('Unsaved changes')).not.toBeNull();
        expect(screen.queryByTestId('authentication-list')).toBeNull();
        await buttonHarness({ name: 'Leave' }).click();
        expect(screen.getByTestId('authentication-list')).not.toBeNull();
    });
});
