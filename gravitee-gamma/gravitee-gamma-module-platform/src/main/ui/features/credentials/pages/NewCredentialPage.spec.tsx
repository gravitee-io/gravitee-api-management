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
import { useNavigate } from 'react-router-dom';

import { NewCredentialPage } from './NewCredentialPage';
import { ApimApiError } from '../../../shared/api/apimClient';
import { useHasEnvironmentPermission } from '../../../shared/hooks/useEnvironmentPermissions';
import { notify } from '../../../shared/notify';
import { useCreateCredential } from '../hooks/useCredentialMutations';
import { ENVIRONMENT_CREDENTIAL_CREATE_PERMISSION, ENVIRONMENT_SECRET_CREATE_PERMISSION } from '../utils/credentialPermissions';

jest.mock('@gravitee/gamma-modules-sdk', () => ({ useEnvironment: () => ({ id: 'env-1' }) }));
jest.mock('react-router-dom', () => ({ ...jest.requireActual('react-router-dom'), useNavigate: jest.fn() }));
jest.mock('../../../shared/hooks/useEnvironmentPermissions');
jest.mock('../hooks/useCredentialMutations');
jest.mock('../../../shared/notify', () => ({ notify: { success: jest.fn(), error: jest.fn() } }));

const navigate = jest.fn();
const mutateAsync = jest.fn();

function givenPermissions(granted: readonly string[]) {
    jest.mocked(useHasEnvironmentPermission).mockImplementation(anyOf => anyOf.some(permission => granted.includes(permission)));
}

async function fillDetails(name: string, type?: string) {
    await userEvent.type(screen.getByLabelText(/Name/), name);
    if (type) {
        await userEvent.click(screen.getByRole('radio', { name: type }));
    }
    await userEvent.click(screen.getByRole('button', { name: 'Next' }));
}

describe('NewCredentialPage', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        jest.mocked(useNavigate).mockReturnValue(navigate);
        jest.mocked(useCreateCredential).mockReturnValue({ mutateAsync, isPending: false } as unknown as ReturnType<
            typeof useCreateCredential
        >);
        mutateAsync.mockResolvedValue({ id: 'cred-9' });
        givenPermissions([ENVIRONMENT_CREDENTIAL_CREATE_PERMISSION, ENVIRONMENT_SECRET_CREATE_PERMISSION]);
    });

    it('creates a bearer credential with a value Gravitee stores', async () => {
        render(<NewCredentialPage />);

        await fillDetails('OpenAI production');
        await userEvent.type(screen.getByLabelText(/Token/), 'sk-123');
        await userEvent.click(screen.getByRole('button', { name: 'Create credential' }));

        expect(mutateAsync).toHaveBeenCalledWith({
            name: 'OpenAI production',
            strategy: 'BEARER',
            configuration: {},
            values: { token: 'sk-123' },
        });
        expect(notify.success).toHaveBeenCalledWith('Credential created');
        expect(navigate).toHaveBeenCalledWith('../cred-9');
    });

    it('masks the typed values', async () => {
        render(<NewCredentialPage />);

        await fillDetails('OpenAI production');

        expect(screen.getByLabelText(/Token/)).toHaveAttribute('type', 'password');
    });

    it('creates an API key credential that references an existing secret', async () => {
        render(<NewCredentialPage />);

        await fillDetails('Anthropic research', 'API key');
        await userEvent.click(screen.getByRole('radio', { name: 'Reference a secret' }));
        await userEvent.type(screen.getByLabelText(/Header name/), 'x-api-key');
        await userEvent.type(screen.getByLabelText(/Value/), '/vault/secret/anthropic:key');
        await userEvent.click(screen.getByRole('button', { name: 'Create credential' }));

        expect(mutateAsync).toHaveBeenCalledWith({
            name: 'Anthropic research',
            strategy: 'API_KEY',
            configuration: { headerName: 'x-api-key' },
            references: { value: '/vault/secret/anthropic:key' },
        });
    });

    it('stays on the details step until the credential has a name', async () => {
        render(<NewCredentialPage />);

        await userEvent.click(screen.getByRole('button', { name: 'Next' }));

        expect(screen.getByText('Name is required.')).toBeInTheDocument();
        expect(screen.queryByRole('button', { name: 'Create credential' })).not.toBeInTheDocument();
    });

    it('clears the name error once a name is typed', async () => {
        render(<NewCredentialPage />);

        await userEvent.click(screen.getByRole('button', { name: 'Next' }));
        await userEvent.type(screen.getByLabelText(/Name/), 'O');

        expect(screen.queryByText('Name is required.')).not.toBeInTheDocument();
    });

    it('clears a secret field error once the field is edited, and keeps the others', async () => {
        render(<NewCredentialPage />);

        await fillDetails('Partner API', 'Basic auth');
        await userEvent.click(screen.getByRole('button', { name: 'Create credential' }));
        await userEvent.type(screen.getByLabelText(/Username/), 'bob');

        expect(screen.queryByText('Username is required.')).not.toBeInTheDocument();
        expect(screen.getByText('Password is required.')).toBeInTheDocument();
    });

    it('does not create a credential with a missing secret field', async () => {
        render(<NewCredentialPage />);

        await fillDetails('OpenAI production');
        await userEvent.click(screen.getByRole('button', { name: 'Create credential' }));

        expect(screen.getByText('Token is required.')).toBeInTheDocument();
        expect(mutateAsync).not.toHaveBeenCalled();
    });

    it('only offers references without the secret create permission', async () => {
        givenPermissions([ENVIRONMENT_CREDENTIAL_CREATE_PERMISSION]);
        render(<NewCredentialPage />);

        await fillDetails('OpenAI production');

        expect(screen.getByRole('radio', { name: 'Store a value' })).toBeDisabled();
        expect(screen.getByRole('radio', { name: 'Reference a secret' })).toHaveAttribute('aria-checked', 'true');
        expect(screen.getByText(/needs the permission to create secrets/)).toBeInTheDocument();
    });

    it('reports a refused creation', async () => {
        const refusal = new ApimApiError(403, 'Forbidden', undefined);
        mutateAsync.mockRejectedValue(refusal);
        render(<NewCredentialPage />);

        await fillDetails('OpenAI production');
        await userEvent.type(screen.getByLabelText(/Token/), 'sk-123');
        await userEvent.click(screen.getByRole('button', { name: 'Create credential' }));

        expect(notify.error).toHaveBeenCalledWith(refusal, 'The credential could not be created.');
        expect(navigate).not.toHaveBeenCalled();
    });
});
