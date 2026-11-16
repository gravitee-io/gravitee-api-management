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
import { renderHook } from '@testing-library/react';
import type { ReactNode } from 'react';
import { MemoryRouter } from 'react-router-dom';

import { useEnvironment } from '@gravitee/gamma-modules-sdk';

import { useCredentialList } from './useCredentialList';
import { useCredentialsPaged } from './useCredentials';
import type { CredentialsPagedResponse } from '../types/credential';

jest.mock('@gravitee/gamma-modules-sdk', () => ({ useEnvironment: jest.fn() }));
jest.mock('./useCredentials');

const ENV_A_PAGE: CredentialsPagedResponse = {
    data: [{ id: 'cred-a', name: 'Env A credential', strategy: 'BEARER', ownerId: 'user-1' }],
    pagination: { page: 1, perPage: 10, pageCount: 1, pageItemsCount: 1, totalCount: 1 },
};

function givenEnvironment(id: string) {
    jest.mocked(useEnvironment).mockReturnValue({ id } as ReturnType<typeof useEnvironment>);
}

function givenListAnswer(state: { data?: CredentialsPagedResponse; error?: Error }) {
    jest.mocked(useCredentialsPaged).mockReturnValue({
        data: state.data,
        error: state.error ?? null,
        isSuccess: Boolean(state.data),
        isPlaceholderData: false,
        isFetching: false,
        refetch: jest.fn(),
    } as unknown as ReturnType<typeof useCredentialsPaged>);
}

const wrapper = ({ children }: { children: ReactNode }) => <MemoryRouter>{children}</MemoryRouter>;

describe('useCredentialList', () => {
    it('keeps the last page on screen when a later load of the same environment fails', () => {
        givenEnvironment('env-a');
        givenListAnswer({ data: ENV_A_PAGE });
        const { result, rerender } = renderHook(() => useCredentialList(), { wrapper });

        givenListAnswer({ error: new Error('Gateway timeout') });
        rerender();

        expect(result.current.result).toBe(ENV_A_PAGE);
    });

    it('never shows the credentials of the previous environment when the new one fails to load', () => {
        givenEnvironment('env-a');
        givenListAnswer({ data: ENV_A_PAGE });
        const { result, rerender } = renderHook(() => useCredentialList(), { wrapper });

        givenEnvironment('env-b');
        givenListAnswer({ error: new Error('Gateway timeout') });
        rerender();

        expect(result.current.result).toBeUndefined();
        expect(result.current.error?.message).toBe('Gateway timeout');
    });
});
