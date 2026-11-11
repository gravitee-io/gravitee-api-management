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
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';

import { DeleteCredentialDialog } from './DeleteCredentialDialog';
import { ApimApiError } from '../../../shared/api/apimClient';
import { notify } from '../../../shared/notify';
import { useDeleteCredential } from '../hooks/useCredentialMutations';
import { useCredentialConsumers } from '../hooks/useCredentials';

jest.mock('@gravitee/gamma-modules-sdk', () => ({ useEnvironment: () => ({ id: 'env-1' }) }));
jest.mock('../hooks/useCredentials');
jest.mock('../hooks/useCredentialMutations');
jest.mock('../../../shared/notify', () => ({ notify: { success: jest.fn(), error: jest.fn() } }));

const mockUseCredentialConsumers = jest.mocked(useCredentialConsumers);
const mockUseDeleteCredential = jest.mocked(useDeleteCredential);
const CREDENTIAL = { id: 'cred-1', name: 'OpenAI production' };
const PROXY = { module: 'aim', type: 'LLM_PROXY', id: 'proxy-1', name: 'Support proxy' };

function consumers(data: unknown[] | undefined, refetch = jest.fn()) {
    mockUseCredentialConsumers.mockReturnValue({ data, isLoading: data === undefined, refetch } as unknown as ReturnType<
        typeof useCredentialConsumers
    >);
    return refetch;
}

function deleteMutation(mutateAsync: jest.Mock) {
    mockUseDeleteCredential.mockReturnValue({ mutateAsync, isPending: false } as unknown as ReturnType<typeof useDeleteCredential>);
}

function renderDialog(onDeleted = jest.fn()) {
    render(
        <MemoryRouter>
            <DeleteCredentialDialog credential={CREDENTIAL} open onOpenChange={jest.fn()} onDeleted={onDeleted} />
        </MemoryRouter>,
    );
    return onDeleted;
}

describe('DeleteCredentialDialog', () => {
    beforeEach(() => jest.clearAllMocks());

    it('deletes a credential nothing uses', async () => {
        consumers([]);
        const mutateAsync = jest.fn().mockResolvedValue(undefined);
        deleteMutation(mutateAsync);
        const onDeleted = renderDialog();

        await userEvent.click(screen.getByRole('button', { name: 'Delete' }));

        expect(mutateAsync).toHaveBeenCalledWith('cred-1');
        expect(notify.success).toHaveBeenCalledWith('Credential deleted');
        expect(onDeleted).toHaveBeenCalled();
    });

    it('blocks the delete and lists the consumers of a credential in use', () => {
        consumers([PROXY]);
        deleteMutation(jest.fn());
        renderDialog();

        expect(screen.getByText(/still uses this credential/)).toBeInTheDocument();
        expect(screen.getByRole('link', { name: 'Support proxy' })).toHaveAttribute('href', '/environments/env-1/aim/llm-proxy/proxy-1');
        expect(screen.getByRole('button', { name: 'Delete' })).toBeDisabled();
    });

    it('waits for the consumers before it allows the delete', () => {
        consumers(undefined);
        deleteMutation(jest.fn());
        renderDialog();

        expect(screen.getByRole('button', { name: 'Delete' })).toBeDisabled();
    });

    it('still allows the delete when the consumers fail to load, since the server refuses a delete in use', async () => {
        mockUseCredentialConsumers.mockReturnValue({
            data: undefined,
            isLoading: false,
            isError: true,
            refetch: jest.fn(),
        } as unknown as ReturnType<typeof useCredentialConsumers>);
        const mutateAsync = jest.fn().mockResolvedValue(undefined);
        deleteMutation(mutateAsync);
        renderDialog();

        expect(screen.getByText(/could not check what uses this credential/i)).toBeInTheDocument();
        await userEvent.click(screen.getByRole('button', { name: 'Delete' }));

        expect(mutateAsync).toHaveBeenCalledWith('cred-1');
    });

    it('reloads the consumers when the server refuses a delete in use', async () => {
        const refetch = consumers([]);
        const conflict = new ApimApiError(409, 'Credential in use', undefined);
        deleteMutation(jest.fn().mockRejectedValue(conflict));
        const onDeleted = renderDialog();

        await userEvent.click(screen.getByRole('button', { name: 'Delete' }));

        await waitFor(() => expect(refetch).toHaveBeenCalled());
        expect(notify.error).toHaveBeenCalledWith(conflict, 'The credential could not be deleted.');
        expect(onDeleted).not.toHaveBeenCalled();
    });
});
