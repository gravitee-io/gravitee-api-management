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
import {
    buttonHarness,
    dataTableHarness,
    pageHeaderHarness,
    renderWithGraphene,
    searchFieldHarness,
} from '@gravitee/graphene-core/testing';
import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes, useLocation } from 'react-router-dom';

import { CredentialsPage } from './CredentialsPage';
import { useHasEnvironmentPermission } from '../../../shared/hooks/useEnvironmentPermissions';
import { useCredentialActions } from '../hooks/useCredentialActions';
import { useDeleteCredential } from '../hooks/useCredentialMutations';
import { useCredentialConsumers, useCredentialsPaged } from '../hooks/useCredentials';
import { stubLayoutApis } from '../testing/stubLayoutApis';
import type { Credential } from '../types/credential';
import { ENVIRONMENT_CREDENTIAL_CREATE_PERMISSION } from '../utils/credentialPermissions';

jest.mock('@gravitee/gamma-modules-sdk', () => ({ useEnvironment: () => ({ id: 'env-1' }) }));
jest.mock('../../../shared/hooks/useEnvironmentPermissions');
jest.mock('../../../shared/hooks/useForbiddenResourceRedirect');
jest.mock('../../../shared/notify', () => ({ notify: { success: jest.fn(), error: jest.fn() } }));
jest.mock('../hooks/useCredentials');
jest.mock('../hooks/useCredentialActions');
jest.mock('../hooks/useCredentialMutations');

const OPENAI: Credential = { id: 'cred-1', name: 'OpenAI production', strategy: 'BEARER', ownerId: 'user-1', ownerName: 'Alex Doyle' };
const refetch = jest.fn();

function givenCredentials(credentials: Credential[], state: { totalCount?: number; isFetching?: boolean; error?: Error } = {}) {
    const loaded = !state.error;
    jest.mocked(useCredentialsPaged).mockReturnValue({
        data: loaded
            ? {
                  data: credentials,
                  pagination: {
                      page: 1,
                      perPage: 10,
                      pageCount: 1,
                      pageItemsCount: credentials.length,
                      totalCount: state.totalCount ?? credentials.length,
                  },
              }
            : undefined,
        error: state.error ?? null,
        isSuccess: loaded,
        isPlaceholderData: false,
        isFetching: state.isFetching ?? false,
        refetch,
    } as unknown as ReturnType<typeof useCredentialsPaged>);
}

function givenPermissions(granted: readonly string[]) {
    jest.mocked(useHasEnvironmentPermission).mockImplementation(anyOf => anyOf.some(permission => granted.includes(permission)));
}

function LocationProbe() {
    const location = useLocation();
    return <output aria-label="Location">{`${location.pathname}${location.search}`}</output>;
}

function renderPage(search = '') {
    return renderWithGraphene(
        <MemoryRouter initialEntries={[`/credentials${search}`]}>
            <Routes>
                <Route
                    path="/credentials"
                    element={
                        <>
                            <CredentialsPage />
                            <LocationProbe />
                        </>
                    }
                />
            </Routes>
        </MemoryRouter>,
    );
}

const location = () => screen.getByRole('status', { name: 'Location' }).textContent;
const lastListRequest = () => jest.mocked(useCredentialsPaged).mock.lastCall?.[0];

describe('CredentialsPage', () => {
    beforeAll(stubLayoutApis);

    beforeEach(() => {
        jest.clearAllMocks();
        givenPermissions([]);
        jest.mocked(useCredentialActions).mockReturnValue({ canEdit: () => true, canDelete: () => true });
        jest.mocked(useCredentialConsumers).mockReturnValue({ data: [], isError: false, refetch: jest.fn() } as unknown as ReturnType<
            typeof useCredentialConsumers
        >);
        jest.mocked(useDeleteCredential).mockReturnValue({
            mutateAsync: jest.fn().mockResolvedValue(undefined),
            isPending: false,
        } as unknown as ReturnType<typeof useDeleteCredential>);
    });

    it('lists the credentials with their owner names', () => {
        givenCredentials([OPENAI]);

        renderPage();

        expect(pageHeaderHarness().getTitle()).toBe('Credentials');
        expect(dataTableHarness().getRow('OpenAI production').getCellText('Owner')).toBe('Alex Doyle');
    });

    it('offers to add a credential only with the create permission', () => {
        givenCredentials([OPENAI]);
        const { unmount } = renderPage();
        expect(pageHeaderHarness().hasActions()).toBe(false);
        unmount();

        givenPermissions([ENVIRONMENT_CREDENTIAL_CREATE_PERMISSION]);
        renderPage();

        expect(screen.getByRole('link', { name: 'Add credential' })).toHaveAttribute('href', '/credentials/new');
    });

    it('asks for the first page of ten credentials by default', () => {
        givenCredentials([OPENAI]);

        renderPage();

        expect(lastListRequest()).toEqual({ query: '', page: 1, perPage: 10 });
    });

    it('restores the search and the page from the URL', () => {
        givenCredentials([OPENAI], { totalCount: 60 });

        renderPage('?q=open&page=2&size=25');

        expect(searchFieldHarness({ name: 'Search credentials' }).getValue()).toBe('open');
        expect(lastListRequest()).toEqual({ query: 'open', page: 2, perPage: 25 });
    });

    it('puts a new search in the URL and goes back to the first page', async () => {
        givenCredentials([OPENAI], { totalCount: 60 });
        renderPage('?page=3');

        await searchFieldHarness({ name: 'Search credentials' }).type('vault');

        await waitFor(() => expect(location()).toBe('/credentials?q=vault'));
        expect(lastListRequest()).toEqual({ query: 'vault', page: 1, perPage: 10 });
    });

    it('resets the search and gives the focus back to the search field', async () => {
        givenCredentials([OPENAI]);
        renderPage('?q=open');

        await buttonHarness({ name: 'Reset' }).click();

        expect(location()).toBe('/credentials');
        expect(searchFieldHarness({ name: 'Search credentials' }).getElement()).toHaveFocus();
        expect(screen.queryByRole('button', { name: 'Reset' })).not.toBeInTheDocument();
    });

    it('goes to the last page when the page in the URL no longer exists', async () => {
        givenCredentials([OPENAI], { totalCount: 15 });

        renderPage('?page=3');

        await waitFor(() => expect(location()).toBe('/credentials?page=2'));
    });

    it('shows the error with a retry when the credentials cannot load', async () => {
        givenCredentials([], { error: new Error('Gateway timeout') });
        renderPage();

        expect(screen.getByRole('alert')).toHaveTextContent('Could not load the credentials');
        expect(screen.getByRole('alert')).toHaveTextContent('Gateway timeout');
        await buttonHarness({ name: 'Retry' }).click();

        expect(refetch).toHaveBeenCalled();
    });

    it('explains credentials when there are none yet', () => {
        givenCredentials([]);

        renderPage();

        expect(screen.getByText('No credentials yet')).toBeInTheDocument();
    });

    it('gives the focus back to the row actions when a delete is cancelled', async () => {
        givenCredentials([OPENAI]);
        renderPage();
        const actions = buttonHarness({ name: 'Actions for OpenAI production' });

        await actions.click();
        await userEvent.click(await screen.findByRole('menuitem', { name: 'Delete' }));
        await buttonHarness({ name: 'Cancel' }).click();

        await waitFor(() => expect(actions.getElement()).toHaveFocus());
    });

    it('moves the focus to the search field once the credential is deleted', async () => {
        givenCredentials([OPENAI]);
        renderPage();

        await buttonHarness({ name: 'Actions for OpenAI production' }).click();
        await userEvent.click(await screen.findByRole('menuitem', { name: 'Delete' }));
        await buttonHarness({ name: 'Delete', within: screen.getByRole('dialog') }).click();

        await waitFor(() => expect(searchFieldHarness({ name: 'Search credentials' }).getElement()).toHaveFocus());
    });
});
