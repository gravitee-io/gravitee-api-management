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
import { MemoryRouter, Route, Routes, useNavigate, useParams } from 'react-router-dom';

import { EditCredentialPage } from './EditCredentialPage';
import { ApimApiError } from '../../../shared/api/apimClient';
import { useHasEnvironmentPermission } from '../../../shared/hooks/useEnvironmentPermissions';
import { notify } from '../../../shared/notify';
import { useRotateCredential, useUpdateCredential } from '../hooks/useCredentialMutations';
import { useCredential, useCredentialOwnerNames } from '../hooks/useCredentials';
import type { Credential } from '../types/credential';
import { ENVIRONMENT_CREDENTIAL_UPDATE_PERMISSION, ENVIRONMENT_SECRET_UPDATE_PERMISSION } from '../utils/credentialPermissions';

jest.mock('@gravitee/gamma-modules-sdk', () => ({ useEnvironment: () => ({ id: 'env-1' }) }));
jest.mock('react-router-dom', () => ({ ...jest.requireActual('react-router-dom'), useNavigate: jest.fn(), useParams: jest.fn() }));
jest.mock('../../../shared/hooks/useEnvironmentPermissions');
jest.mock('../hooks/useCredentials');
jest.mock('../hooks/useCredentialMutations');
jest.mock('../../../shared/notify', () => ({ notify: { success: jest.fn(), error: jest.fn() } }));

const navigate = jest.fn();
const update = jest.fn();
const rotate = jest.fn();
const MANAGED: Credential = {
    id: 'cred-1',
    name: 'Anthropic research',
    strategy: 'API_KEY',
    serviceId: 'anthropic',
    ownerId: 'user-1',
    configuration: { headerName: 'x-api-key' },
    secretReferences: { value: { reference: '/gravitee/credential.cred-1.value:value', managed: true } },
    managed: true,
    manageable: true,
};
const REFERENCED: Credential = {
    id: 'cred-2',
    name: 'Vault token',
    strategy: 'BEARER',
    secretReferences: { token: { reference: '/vault/secret/openai:token', managed: false } },
    managed: false,
    manageable: true,
};

function given(credential: Credential) {
    jest.mocked(useParams).mockReturnValue({ credentialId: credential.id });
    jest.mocked(useCredential).mockReturnValue({ data: credential, isLoading: false, isError: false } as unknown as ReturnType<
        typeof useCredential
    >);
}

function givenPermissions(granted: readonly string[]) {
    jest.mocked(useHasEnvironmentPermission).mockImplementation(anyOf => anyOf.some(permission => granted.includes(permission)));
}

async function save() {
    await userEvent.click(screen.getByRole('button', { name: 'Save changes' }));
}

describe('EditCredentialPage', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        jest.mocked(useNavigate).mockReturnValue(navigate);
        jest.mocked(useCredentialOwnerNames).mockReturnValue({ 'user-1': 'Priya Raman' });
        jest.mocked(useUpdateCredential).mockReturnValue({ mutateAsync: update, isPending: false } as unknown as ReturnType<
            typeof useUpdateCredential
        >);
        jest.mocked(useRotateCredential).mockReturnValue({ mutateAsync: rotate, isPending: false } as unknown as ReturnType<
            typeof useRotateCredential
        >);
        update.mockResolvedValue(MANAGED);
        rotate.mockResolvedValue(MANAGED);
        givenPermissions([ENVIRONMENT_CREDENTIAL_UPDATE_PERMISSION, ENVIRONMENT_SECRET_UPDATE_PERMISSION]);
    });

    it('loads the credential with its owner and type fixed', () => {
        given(MANAGED);

        render(<EditCredentialPage />);

        expect(screen.getByLabelText(/^Name/)).toHaveValue('Anthropic research');
        expect(screen.getByLabelText(/Service/)).toHaveValue('anthropic');
        expect(screen.getByLabelText(/Header name/)).toHaveValue('x-api-key');
        expect(screen.getByLabelText(/Value/)).toHaveValue('');
        expect(screen.getByText('Priya Raman')).toBeInTheDocument();
        expect(screen.getByText('API key')).toBeInTheDocument();
    });

    it('saves the details and keeps the stored values when none are typed', async () => {
        given(MANAGED);
        render(<EditCredentialPage />);

        await userEvent.clear(screen.getByLabelText(/^Name/));
        await userEvent.type(screen.getByLabelText(/^Name/), 'Anthropic prod');
        await save();

        expect(update).toHaveBeenCalledWith({
            id: 'cred-1',
            payload: { name: 'Anthropic prod', serviceId: 'anthropic', configuration: { headerName: 'x-api-key' } },
        });
        expect(rotate).not.toHaveBeenCalled();
        expect(notify.success).toHaveBeenCalledWith('Credential updated');
    });

    it('rotates the values the user types', async () => {
        given(MANAGED);
        render(<EditCredentialPage />);

        await userEvent.type(screen.getByLabelText(/Value/), 'new-key');
        await save();

        expect(update).toHaveBeenCalled();
        expect(rotate).toHaveBeenCalledWith({ id: 'cred-1', payload: { values: { value: 'new-key' } } });
        expect(notify.success).toHaveBeenCalledWith('Credential updated and its secret replaced');
    });

    it('states when running APIs pick up a replaced value', () => {
        given(MANAGED);

        render(<EditCredentialPage />);

        expect(screen.getByText(/within 1 day and 15 minutes/)).toBeInTheDocument();
    });

    it('cannot replace the values without the secret update permission', () => {
        given(MANAGED);
        givenPermissions([ENVIRONMENT_CREDENTIAL_UPDATE_PERMISSION]);

        render(<EditCredentialPage />);

        expect(screen.getByLabelText(/Value/)).toBeDisabled();
        expect(screen.getByText(/needs the permission to update secrets/)).toBeInTheDocument();
    });

    it('edits the references of a credential that references secrets', async () => {
        given(REFERENCED);
        render(<EditCredentialPage />);

        expect(screen.getByLabelText(/Token/)).toHaveValue('/vault/secret/openai:token');
        await userEvent.clear(screen.getByLabelText(/Token/));
        await userEvent.type(screen.getByLabelText(/Token/), '/vault/secret/new:token');
        await save();

        expect(update).toHaveBeenCalledWith({
            id: 'cred-2',
            payload: { name: 'Vault token', configuration: {}, references: { token: '/vault/secret/new:token' } },
        });
        expect(rotate).not.toHaveBeenCalled();
    });

    it('sends nothing while a field is invalid', async () => {
        given(MANAGED);
        render(<EditCredentialPage />);

        await userEvent.clear(screen.getByLabelText(/Header name/));
        await save();

        expect(screen.getByText('Header name is required.')).toBeInTheDocument();
        expect(update).not.toHaveBeenCalled();
    });

    it('does not rotate when the update is refused', async () => {
        const refusal = new ApimApiError(403, 'Only the owner can change it', undefined);
        update.mockRejectedValue(refusal);
        given(MANAGED);
        render(<EditCredentialPage />);

        await userEvent.type(screen.getByLabelText(/Value/), 'new-key');
        await save();

        expect(rotate).not.toHaveBeenCalled();
        expect(notify.error).toHaveBeenCalledWith('Only the owner can change it');
        expect(navigate).not.toHaveBeenCalled();
    });

    it('sends a caller who cannot manage the credential back to it', async () => {
        const router = jest.requireActual<{ useNavigate: typeof useNavigate }>('react-router-dom');
        jest.mocked(useNavigate).mockImplementation(router.useNavigate);
        given({ ...MANAGED, manageable: false });
        render(
            <MemoryRouter initialEntries={['/credentials/cred-1/edit']}>
                <Routes>
                    <Route path="credentials">
                        <Route path=":credentialId" element={<p>Credential page</p>} />
                        <Route path=":credentialId/edit" element={<EditCredentialPage />} />
                    </Route>
                </Routes>
            </MemoryRouter>,
        );

        expect(await screen.findByText('Credential page')).toBeInTheDocument();
        expect(screen.queryByRole('button', { name: 'Save changes' })).not.toBeInTheDocument();
    });

    it.each(['Save changes', 'Back to credential', 'Cancel'])('returns to the credential page after %s', async button => {
        const router = jest.requireActual<{ useNavigate: typeof useNavigate }>('react-router-dom');
        jest.mocked(useNavigate).mockImplementation(router.useNavigate);
        given(MANAGED);
        render(
            <MemoryRouter initialEntries={['/credentials/cred-1/edit']}>
                <Routes>
                    <Route path="credentials">
                        <Route index element={<p>Credential list</p>} />
                        <Route path=":credentialId" element={<p>Credential page</p>} />
                        <Route path=":credentialId/edit" element={<EditCredentialPage />} />
                    </Route>
                </Routes>
            </MemoryRouter>,
        );

        await userEvent.click(screen.getByRole('button', { name: button }));

        expect(await screen.findByText('Credential page')).toBeInTheDocument();
    });
});
