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
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, useNavigate, useParams } from 'react-router-dom';

import { CredentialDetailPage } from './CredentialDetailPage';
import { useHasEnvironmentPermission } from '../../../shared/hooks/useEnvironmentPermissions';
import { useCredential, useCredentialConsumers, useCredentialOwnerNames } from '../hooks/useCredentials';
import type { Credential, CredentialConsumer } from '../types/credential';
import { ENVIRONMENT_CREDENTIAL_DELETE_PERMISSION, ENVIRONMENT_CREDENTIAL_UPDATE_PERMISSION } from '../utils/credentialPermissions';

jest.mock('@gravitee/gamma-modules-sdk', () => ({ useEnvironment: () => ({ id: 'env-1' }) }));
jest.mock('react-router-dom', () => ({ ...jest.requireActual('react-router-dom'), useNavigate: jest.fn(), useParams: jest.fn() }));
jest.mock('../../../shared/hooks/useEnvironmentPermissions');
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

function givenPermissions(granted: readonly string[]) {
    jest.mocked(useHasEnvironmentPermission).mockImplementation(anyOf => anyOf.some(permission => granted.includes(permission)));
}

function renderPage() {
    render(
        <MemoryRouter>
            <CredentialDetailPage />
        </MemoryRouter>,
    );
}

describe('CredentialDetailPage', () => {
    beforeAll(() => {
        global.ResizeObserver = class ResizeObserver {
            observe() {}
            unobserve() {}
            disconnect() {}
        } as typeof ResizeObserver;
    });

    beforeEach(() => {
        jest.clearAllMocks();
        jest.mocked(useNavigate).mockReturnValue(navigate);
        jest.mocked(useCredentialOwnerNames).mockReturnValue({ 'user-1': 'Priya Raman' });
        givenPermissions([]);
    });

    it('shows the details of a credential Gravitee stores', () => {
        given(MANAGED);

        renderPage();

        expect(screen.getByRole('heading', { name: 'Anthropic research' })).toBeInTheDocument();
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

        expect(screen.queryByRole('button', { name: 'Credential actions' })).not.toBeInTheDocument();
    });

    it('opens the edit page with the update permission', async () => {
        given(MANAGED);
        givenPermissions([ENVIRONMENT_CREDENTIAL_UPDATE_PERMISSION]);
        renderPage();

        await userEvent.click(screen.getByRole('button', { name: 'Credential actions' }));
        await userEvent.click(await screen.findByRole('menuitem', { name: 'Edit' }));

        expect(navigate).toHaveBeenCalledWith('edit');
    });

    it('goes back to the list once the credential is deleted', async () => {
        given(MANAGED);
        givenPermissions([ENVIRONMENT_CREDENTIAL_DELETE_PERMISSION]);
        renderPage();

        await userEvent.click(screen.getByRole('button', { name: 'Credential actions' }));
        await userEvent.click(await screen.findByRole('menuitem', { name: 'Delete' }));
        await userEvent.click(screen.getByRole('button', { name: 'Confirm delete' }));

        expect(navigate).toHaveBeenCalledWith('..');
    });
});
