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
import { buttonHarness, pageHeaderHarness, renderWithGraphene } from '@gravitee/graphene-core/testing';
import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, useNavigate, useParams } from 'react-router-dom';

import { CredentialDetailPage } from './CredentialDetailPage';
import { useCredentialActions } from '../hooks/useCredentialActions';
import { useCredential, useCredentialConsumers } from '../hooks/useCredentials';
import { stubLayoutApis } from '../testing/stubLayoutApis';
import type { Credential, CredentialConsumer } from '../types/credential';

jest.mock('@gravitee/gamma-modules-sdk', () => ({ useEnvironment: () => ({ id: 'env-1' }) }));
jest.mock('react-router-dom', () => ({ ...jest.requireActual('react-router-dom'), useNavigate: jest.fn(), useParams: jest.fn() }));
jest.mock('../hooks/useCredentialActions');
jest.mock('../hooks/useCredentials');
jest.mock('../components/DeleteCredentialDialog', () => ({
    DeleteCredentialDialog: ({ open, onDeleted }: { open: boolean; onDeleted: () => void }) =>
        open ? (
            <button type="button" onClick={onDeleted}>
                Confirm delete
            </button>
        ) : null,
}));

const navigate = jest.fn();
const MANAGED: Credential = {
    id: 'cred-1',
    name: 'Anthropic research',
    strategy: 'API_KEY',
    serviceId: 'anthropic',
    ownerId: 'user-1',
    ownerName: 'Priya Raman',
    configuration: { headerName: 'x-api-key' },
    secretReferences: { value: { reference: '/gravitee/credential.cred-1.value:value', managed: true } },
    managed: true,
    createdAt: '2026-09-01T10:00:00Z',
    updatedAt: '2026-09-01T10:00:00Z',
};
const REFERENCED: Credential = {
    id: 'cred-2',
    name: 'Vault token',
    strategy: 'BEARER',
    secretReferences: { token: { reference: '/vault/secret/openai:token', managed: false } },
    managed: false,
    rotatedAt: '2026-10-01T10:00:00Z',
};

function given(credential: Credential, consumers: CredentialConsumer[] = []) {
    jest.mocked(useParams).mockReturnValue({ credentialId: credential.id });
    jest.mocked(useCredential).mockReturnValue({ data: credential, isLoading: false, isError: false } as unknown as ReturnType<
        typeof useCredential
    >);
    jest.mocked(useCredentialConsumers).mockReturnValue({ data: consumers, isLoading: false } as unknown as ReturnType<
        typeof useCredentialConsumers
    >);
}

function givenActions({ canEdit = false, canDelete = false }: { canEdit?: boolean; canDelete?: boolean } = {}) {
    jest.mocked(useCredentialActions).mockReturnValue({ canEdit: () => canEdit, canDelete: () => canDelete });
}

function renderPage() {
    renderWithGraphene(
        <MemoryRouter>
            <CredentialDetailPage />
        </MemoryRouter>,
    );
}

describe('CredentialDetailPage', () => {
    beforeAll(stubLayoutApis);

    beforeEach(() => {
        jest.clearAllMocks();
        jest.mocked(useNavigate).mockReturnValue(navigate);
        givenActions();
    });

    it('shows a credential of a type this console does not know', () => {
        given({ ...MANAGED, strategy: 'MTLS' } as unknown as Credential);

        renderPage();

        expect(pageHeaderHarness().getTitle()).toBe('Anthropic research');
        expect(pageHeaderHarness().getDescription()).toBe('MTLS');
    });

    it('shows the details of a credential Gravitee stores', () => {
        given(MANAGED);

        renderPage();

        expect(pageHeaderHarness().getTitle()).toBe('Anthropic research');
        expect(screen.getByText('Service')).toBeInTheDocument();
        expect(screen.getByText('anthropic')).toBeInTheDocument();
        expect(screen.getAllByText('API key').length).toBeGreaterThan(0);
        expect(screen.getByText('Priya Raman')).toBeInTheDocument();
        expect(screen.getByText('Stored by Gravitee')).toBeInTheDocument();
        expect(screen.getByText('x-api-key')).toBeInTheDocument();
        expect(screen.getByText('Never replaced')).toBeInTheDocument();
        expect(screen.queryByText(/credential\.cred-1/)).not.toBeInTheDocument();
    });

    it('shows the references of a credential that references secrets', () => {
        given(REFERENCED);

        renderPage();

        expect(screen.getByText('References existing secrets')).toBeInTheDocument();
        expect(screen.getByText('/vault/secret/openai:token')).toBeInTheDocument();
    });

    it('lists each consumer with a link to it', () => {
        given(MANAGED, [{ module: 'aim', type: 'LLM_PROXY', id: 'proxy-1', name: 'Support proxy' }]);

        renderPage();

        expect(screen.getByRole('link', { name: 'Support proxy' })).toHaveAttribute('href', '/environments/env-1/aim/llm-proxy/proxy-1');
        expect(screen.getByText('LLM proxy')).toBeInTheDocument();
    });

    it('names the actions menu after the credential', () => {
        given(MANAGED);
        givenActions({ canEdit: true });

        renderPage();

        expect(pageHeaderHarness().hasActions()).toBe(true);
        expect(screen.getByRole('button', { name: 'Actions for Anthropic research' })).toBeInTheDocument();
    });

    it('never shows the id of an owner whose name the server does not know', () => {
        given({ ...MANAGED, ownerName: undefined });

        renderPage();

        expect(screen.getByText('Unknown user')).toBeInTheDocument();
        expect(screen.queryByText('user-1')).not.toBeInTheDocument();
    });

    it('shows the error with a retry when the credential cannot load', async () => {
        const refetch = jest.fn();
        jest.mocked(useParams).mockReturnValue({ credentialId: 'cred-1' });
        jest.mocked(useCredential).mockReturnValue({
            data: undefined,
            isLoading: false,
            error: new Error('Credential not found'),
            isFetching: false,
            refetch,
        } as unknown as ReturnType<typeof useCredential>);
        jest.mocked(useCredentialConsumers).mockReturnValue({ data: undefined, isError: true } as unknown as ReturnType<
            typeof useCredentialConsumers
        >);
        renderPage();

        expect(screen.getByRole('alert')).toHaveTextContent('Could not load the credential');
        expect(screen.getByRole('alert')).toHaveTextContent('Credential not found');
        await buttonHarness({ name: 'Retry' }).click();

        expect(refetch).toHaveBeenCalled();
    });

    it('says when nothing uses the credential', () => {
        given(MANAGED);

        renderPage();

        expect(screen.getByText('Nothing uses this credential yet.')).toBeInTheDocument();
    });

    it('states when running APIs pick up new values', () => {
        given(MANAGED);

        renderPage();

        expect(screen.getByText(/within 1 day and 15 minutes/)).toBeInTheDocument();
    });

    it('hides the actions without the update and delete permissions', () => {
        given(MANAGED);

        renderPage();

        expect(screen.queryByRole('button', { name: /^Actions for/ })).not.toBeInTheDocument();
    });

    it('opens the edit page when the user may edit the credential', async () => {
        given(MANAGED);
        givenActions({ canEdit: true });
        renderPage();

        await buttonHarness({ name: /^Actions for/ }).click();

        expect(await screen.findByRole('menuitem', { name: 'Edit' })).toHaveAttribute('href', '/edit');
    });

    it('says so when the consumers fail to load', () => {
        given(MANAGED);
        jest.mocked(useCredentialConsumers).mockReturnValue({ data: undefined, isLoading: false, isError: true } as unknown as ReturnType<
            typeof useCredentialConsumers
        >);

        renderPage();

        expect(screen.getByText(/could not load what uses this credential/i)).toBeInTheDocument();
    });

    it('offers only the actions the user may take on this credential', async () => {
        given(REFERENCED);
        const canDelete = jest.fn(() => true);
        jest.mocked(useCredentialActions).mockReturnValue({ canEdit: () => false, canDelete });
        renderPage();

        await buttonHarness({ name: /^Actions for/ }).click();

        expect(await screen.findByRole('menuitem', { name: 'Delete' })).toBeInTheDocument();
        expect(screen.queryByRole('menuitem', { name: 'Edit' })).not.toBeInTheDocument();
        expect(canDelete).toHaveBeenCalledWith(REFERENCED);
    });

    it('goes back to the list once the credential is deleted', async () => {
        given(MANAGED);
        givenActions({ canDelete: true });
        renderPage();

        await buttonHarness({ name: /^Actions for/ }).click();
        await userEvent.click(await screen.findByRole('menuitem', { name: 'Delete' }));
        await userEvent.click(screen.getByRole('button', { name: 'Confirm delete' }));

        expect(navigate).toHaveBeenCalledWith('..');
    });
});
