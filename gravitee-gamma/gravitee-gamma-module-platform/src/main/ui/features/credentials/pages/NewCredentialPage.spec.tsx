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
import { buttonHarness, inputHarness, pageHeaderHarness, passwordInputHarness, renderWithGraphene } from '@gravitee/graphene-core/testing';
import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { useNavigate } from 'react-router-dom';

import { NewCredentialPage } from './NewCredentialPage';
import { ApimApiError } from '../../../shared/api/apimClient';
import { useHasEnvironmentPermission } from '../../../shared/hooks/useEnvironmentPermissions';
import { notify } from '../../../shared/notify';
import { useCreateCredential } from '../hooks/useCredentialMutations';
import { stubLayoutApis } from '../testing/stubLayoutApis';
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
    await inputHarness({ name: /Name/ }).type(name);
    if (type) {
        await userEvent.click(screen.getByRole('radio', { name: type }));
    }
    await buttonHarness({ name: 'Next' }).click();
}

describe('NewCredentialPage', () => {
    beforeAll(stubLayoutApis);

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
        renderWithGraphene(<NewCredentialPage />);

        await fillDetails('OpenAI production');
        await passwordInputHarness({ name: /Token/ }).type('sk-123');
        await buttonHarness({ name: 'Create credential' }).click();

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
        renderWithGraphene(<NewCredentialPage />);

        await fillDetails('OpenAI production');

        expect(passwordInputHarness({ name: /Token/ }).isMasked()).toBe(true);
    });

    it('creates an API key credential that references an existing secret', async () => {
        renderWithGraphene(<NewCredentialPage />);

        await fillDetails('Anthropic research', 'API key');
        await userEvent.click(screen.getByRole('radio', { name: 'Reference a secret' }));
        await inputHarness({ name: /Header name/ }).type('x-api-key');
        await inputHarness({ name: /Value/ }).type('/vault/secret/anthropic:key');
        await buttonHarness({ name: 'Create credential' }).click();

        expect(mutateAsync).toHaveBeenCalledWith({
            name: 'Anthropic research',
            strategy: 'API_KEY',
            configuration: { headerName: 'x-api-key' },
            references: { value: '/vault/secret/anthropic:key' },
        });
    });

    it('explains the flags that let a referenced secret pick up a rotated value', async () => {
        renderWithGraphene(<NewCredentialPage />);

        await fillDetails('Anthropic research', 'API key');
        await userEvent.click(screen.getByRole('radio', { name: 'Reference a secret' }));

        expect(screen.getByText(/\?renewable=true&reloadOnChange=true/)).toBeInTheDocument();
    });

    it('stays on the details step until the credential has a name', async () => {
        renderWithGraphene(<NewCredentialPage />);

        await buttonHarness({ name: 'Next' }).click();

        expect(screen.getByText('Name is required.')).toBeInTheDocument();
        expect(screen.queryByRole('button', { name: 'Create credential' })).not.toBeInTheDocument();
    });

    it('heads the page as adding a credential', () => {
        renderWithGraphene(<NewCredentialPage />);

        expect(pageHeaderHarness().getTitle()).toBe('Add credential');
    });

    it('ties the name error to the name field', async () => {
        renderWithGraphene(<NewCredentialPage />);

        await buttonHarness({ name: 'Next' }).click();

        expect(inputHarness({ name: /Name/ }).isInvalid()).toBe(true);
        expect(inputHarness({ name: /Name/ }).getElement()).toHaveAccessibleDescription('Name is required.');
    });

    it('ties a secret field error to its field', async () => {
        renderWithGraphene(<NewCredentialPage />);

        await fillDetails('OpenAI production');
        await buttonHarness({ name: 'Create credential' }).click();

        expect(passwordInputHarness({ name: /Token/ }).getElement()).toHaveAccessibleDescription('Token is required.');
    });

    it('clears the name error once a name is typed', async () => {
        renderWithGraphene(<NewCredentialPage />);

        await buttonHarness({ name: 'Next' }).click();
        await inputHarness({ name: /Name/ }).type('O');

        expect(screen.queryByText('Name is required.')).not.toBeInTheDocument();
    });

    it('clears a secret field error once the field is edited, and keeps the others', async () => {
        renderWithGraphene(<NewCredentialPage />);

        await fillDetails('Partner API', 'Basic auth');
        await buttonHarness({ name: 'Create credential' }).click();
        await inputHarness({ name: /Username/ }).type('bob');

        expect(screen.queryByText('Username is required.')).not.toBeInTheDocument();
        expect(screen.getByText('Password is required.')).toBeInTheDocument();
    });

    it('does not create a credential with a missing secret field', async () => {
        renderWithGraphene(<NewCredentialPage />);

        await fillDetails('OpenAI production');
        await buttonHarness({ name: 'Create credential' }).click();

        expect(screen.getByText('Token is required.')).toBeInTheDocument();
        expect(mutateAsync).not.toHaveBeenCalled();
    });

    it('only offers references without the secret create permission', async () => {
        givenPermissions([ENVIRONMENT_CREDENTIAL_CREATE_PERMISSION]);
        renderWithGraphene(<NewCredentialPage />);

        await fillDetails('OpenAI production');

        expect(screen.getByRole('radio', { name: 'Store a value' })).toBeDisabled();
        expect(screen.getByRole('radio', { name: 'Reference a secret' })).toHaveAttribute('aria-checked', 'true');
        expect(screen.getByText(/needs the permission to create secrets/)).toBeInTheDocument();
    });

    it('reports a refused creation', async () => {
        const refusal = new ApimApiError(403, 'Forbidden', { message: 'Forbidden' });
        mutateAsync.mockRejectedValue(refusal);
        renderWithGraphene(<NewCredentialPage />);

        await fillDetails('OpenAI production');
        await passwordInputHarness({ name: /Token/ }).type('sk-123');
        await buttonHarness({ name: 'Create credential' }).click();

        expect(notify.error).toHaveBeenCalledWith('Forbidden');
        expect(navigate).not.toHaveBeenCalled();
    });

    it('reports a server fault without its operator detail', async () => {
        mutateAsync.mockRejectedValue(
            new ApimApiError(500, 'Secret values cannot be stored or read: api.secrets.encryption.key is not set.'),
        );
        renderWithGraphene(<NewCredentialPage />);

        await fillDetails('OpenAI production');
        await passwordInputHarness({ name: /Token/ }).type('sk-123');
        await buttonHarness({ name: 'Create credential' }).click();

        expect(notify.error).toHaveBeenCalledWith('The credential could not be created.');
    });
});
