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

import { useSaveLocalLogin } from './useIdentityProviderMutations';
import { saveOrgConsoleSettings } from '../../organization-settings/services/consoleSettings';
import { orgConsoleSettingsKeys } from '../../organization-settings/utils/queryKeys';

jest.mock('../../organization-settings/services/consoleSettings', () => ({
    saveOrgConsoleSettings: jest.fn(),
}));

const mockSaveOrgConsoleSettings = jest.mocked(saveOrgConsoleSettings);

describe('useSaveLocalLogin', () => {
    afterEach(() => {
        jest.clearAllMocks();
    });

    it('saves through the shared org settings client and invalidates that cache', async () => {
        const payload = { authentication: { localLogin: { enabled: false } }, cors: { maxAge: 60 } };
        mockSaveOrgConsoleSettings.mockResolvedValue(payload);
        const queryClient = new QueryClient({ defaultOptions: { mutations: { retry: false }, queries: { retry: false } } });
        const invalidateQueries = jest.spyOn(queryClient, 'invalidateQueries');

        function Wrapper({ children }: { children: ReactNode }) {
            return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
        }

        const { result } = renderHook(() => useSaveLocalLogin(), { wrapper: Wrapper });
        await result.current.mutateAsync(payload);

        await waitFor(() => {
            expect(mockSaveOrgConsoleSettings).toHaveBeenCalledWith(payload);
            expect(invalidateQueries).toHaveBeenCalledWith({ queryKey: orgConsoleSettingsKeys.all });
        });
    });
});
