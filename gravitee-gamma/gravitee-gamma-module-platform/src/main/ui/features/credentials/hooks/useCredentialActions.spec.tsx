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
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { renderHook, waitFor } from '@testing-library/react';
import type { ReactNode } from 'react';

import { useCredentialActions } from './useCredentialActions';
import { useHasEnvironmentPermission } from '../../../shared/hooks/useEnvironmentPermissions';
import { fetchCurrentUser } from '../../../shared/services/currentUser';
import type { Credential } from '../types/credential';
import {
    ENVIRONMENT_CREDENTIAL_DELETE_PERMISSION,
    ENVIRONMENT_CREDENTIAL_UPDATE_PERMISSION,
    ENVIRONMENT_SECRET_DELETE_PERMISSION,
} from '../utils/credentialPermissions';

jest.mock('../../../shared/hooks/useEnvironmentPermissions');
jest.mock('../../../shared/services/currentUser', () => ({
    ...jest.requireActual('../../../shared/services/currentUser'),
    fetchCurrentUser: jest.fn(),
}));

const ALL_PERMISSIONS = [
    ENVIRONMENT_CREDENTIAL_UPDATE_PERMISSION,
    ENVIRONMENT_CREDENTIAL_DELETE_PERMISSION,
    ENVIRONMENT_SECRET_DELETE_PERMISSION,
];
const MINE: Credential = { id: 'cred-1', name: 'Mine', strategy: 'BEARER', ownerId: 'me', managed: true };
const THEIRS: Credential = { id: 'cred-2', name: 'Theirs', strategy: 'BEARER', ownerId: 'someone-else', managed: true };
const THEIR_REFERENCE: Credential = { ...THEIRS, id: 'cred-3', managed: false };

function givenPermissions(granted: readonly string[]) {
    jest.mocked(useHasEnvironmentPermission).mockImplementation(anyOf => anyOf.some(permission => granted.includes(permission)));
}

function givenUser(roles: { scope: string; name: string }[] = []) {
    jest.mocked(fetchCurrentUser).mockResolvedValue({ id: 'me', roles } as Awaited<ReturnType<typeof fetchCurrentUser>>);
}

function wrapper({ children }: { children: ReactNode }) {
    return (
        <QueryClientProvider client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}>{children}</QueryClientProvider>
    );
}

async function actionsOnceLoaded() {
    const { result } = renderHook(() => useCredentialActions(), { wrapper });
    await waitFor(() => expect(fetchCurrentUser).toHaveBeenCalled());
    return result;
}

describe('useCredentialActions', () => {
    afterEach(() => jest.clearAllMocks());

    it('lets the owner edit and delete their own credential only', async () => {
        givenPermissions(ALL_PERMISSIONS);
        givenUser();
        const result = await actionsOnceLoaded();

        await waitFor(() => expect(result.current.canEdit(MINE)).toBe(true));
        expect(result.current.canDelete(MINE)).toBe(true);
        expect(result.current.canEdit(THEIRS)).toBe(false);
        expect(result.current.canDelete(THEIRS)).toBe(false);
    });

    it.each([
        ['an organization admin', 'ORGANIZATION'],
        ['an environment admin', 'ENVIRONMENT'],
    ])('lets %s manage any credential', async (_case, scope) => {
        givenPermissions(ALL_PERMISSIONS);
        givenUser([{ scope, name: 'ADMIN' }]);
        const result = await actionsOnceLoaded();

        await waitFor(() => expect(result.current.canEdit(THEIRS)).toBe(true));
        expect(result.current.canDelete(THEIRS)).toBe(true);
    });

    it('needs the credential permissions even for the owner', async () => {
        givenPermissions([]);
        givenUser();
        const result = await actionsOnceLoaded();

        expect(result.current.canEdit(MINE)).toBe(false);
        expect(result.current.canDelete(MINE)).toBe(false);
    });

    it('needs the secret delete permission to delete a credential Gravitee stores', async () => {
        givenPermissions([ENVIRONMENT_CREDENTIAL_DELETE_PERMISSION]);
        givenUser([{ scope: 'ENVIRONMENT', name: 'ADMIN' }]);
        const result = await actionsOnceLoaded();

        await waitFor(() => expect(result.current.canDelete(THEIR_REFERENCE)).toBe(true));
        expect(result.current.canDelete(THEIRS)).toBe(false);
    });
});
