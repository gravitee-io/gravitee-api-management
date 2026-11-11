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

import { CredentialsPage } from './CredentialsPage';
import { useHasEnvironmentPermission } from '../../../shared/hooks/useEnvironmentPermissions';
import { useCredentialOwnerNames, useCredentialsPaged } from '../hooks/useCredentials';
import type { Credential } from '../types/credential';
import { ENVIRONMENT_CREDENTIAL_CREATE_PERMISSION } from '../utils/credentialPermissions';

jest.mock('@gravitee/gamma-modules-sdk', () => ({ useEnvironment: () => ({ id: 'env-1' }) }));
jest.mock('react-router-dom', () => ({ ...jest.requireActual('react-router-dom'), useNavigate: jest.fn() }));
jest.mock('../../../shared/hooks/useEnvironmentPermissions');
jest.mock('../../../shared/hooks/useForbiddenResourceRedirect');
jest.mock('../hooks/useCredentials');
jest.mock('../components/CredentialsTable', () => ({
    CredentialsTable: ({
        credentials,
        ownerNames,
        onEdit,
        onDelete,
    }: {
        credentials: Credential[];
        ownerNames: Record<string, string>;
        onEdit: (credential: Credential) => void;
        onDelete: (credential: Credential) => void;
    }) => (
        <div>
            {credentials.map(credential => (
                <div key={credential.id}>
                    <span>{credential.name}</span>
                    <span>{ownerNames[credential.ownerId ?? '']}</span>
                    <button type="button" onClick={() => onEdit(credential)}>
                        Edit {credential.name}
                    </button>
                    <button type="button" onClick={() => onDelete(credential)}>
                        Delete {credential.name}
                    </button>
                </div>
            ))}
        </div>
    ),
}));
jest.mock('../components/DeleteCredentialDialog', () => ({
    DeleteCredentialDialog: ({ credential, open }: { credential: Credential; open: boolean }) =>
        open ? <div role="dialog">Delete dialog for {credential.name}</div> : null,
}));

const navigate = jest.fn();
const OPENAI: Credential = { id: 'cred-1', name: 'OpenAI production', strategy: 'BEARER', ownerId: 'user-1' };

function givenCredentials(credentials: Credential[]) {
    jest.mocked(useCredentialsPaged).mockReturnValue({
        data: {
            data: credentials,
            pagination: { page: 1, perPage: 25, pageCount: 1, pageItemsCount: credentials.length, totalCount: credentials.length },
        },
        isLoading: false,
        isError: false,
        error: null,
    } as unknown as ReturnType<typeof useCredentialsPaged>);
}

function givenPermissions(granted: readonly string[]) {
    jest.mocked(useHasEnvironmentPermission).mockImplementation(anyOf => anyOf.some(permission => granted.includes(permission)));
}

describe('CredentialsPage', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        jest.mocked(useNavigate).mockReturnValue(navigate);
        jest.mocked(useCredentialOwnerNames).mockReturnValue({ 'user-1': 'Alex Doyle' });
        givenPermissions([]);
    });

    it('lists the credentials with their owner names', () => {
        givenCredentials([OPENAI]);

        render(<CredentialsPage />);

        expect(screen.getByRole('heading', { name: 'Credentials' })).toBeInTheDocument();
        expect(screen.getByText('OpenAI production')).toBeInTheDocument();
        expect(screen.getByText('Alex Doyle')).toBeInTheDocument();
    });

    it('offers a new credential only with the create permission', async () => {
        givenCredentials([OPENAI]);
        const { unmount } = render(<CredentialsPage />);
        expect(screen.queryByRole('button', { name: 'New credential' })).not.toBeInTheDocument();
        unmount();

        givenPermissions([ENVIRONMENT_CREDENTIAL_CREATE_PERMISSION]);
        render(<CredentialsPage />);
        await userEvent.click(screen.getByRole('button', { name: 'New credential' }));

        expect(navigate).toHaveBeenCalledWith('new');
    });

    it('opens the edit page of a credential', async () => {
        givenCredentials([OPENAI]);
        render(<CredentialsPage />);

        await userEvent.click(screen.getByRole('button', { name: 'Edit OpenAI production' }));

        expect(navigate).toHaveBeenCalledWith('cred-1/edit');
    });

    it('asks before it deletes a credential', async () => {
        givenCredentials([OPENAI]);
        render(<CredentialsPage />);

        await userEvent.click(screen.getByRole('button', { name: 'Delete OpenAI production' }));

        expect(screen.getByRole('dialog')).toHaveTextContent('Delete dialog for OpenAI production');
    });

    it('explains credentials when there are none yet', () => {
        givenCredentials([]);

        render(<CredentialsPage />);

        expect(screen.getByText('No credentials yet')).toBeInTheDocument();
    });
});
