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
import { useEnvironment } from '@gravitee/gamma-modules-sdk';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { renderHook, waitFor } from '@testing-library/react';
import type { ReactNode } from 'react';

import { useEnvironmentConsoleSettings } from './useEnvironmentConsoleSettings';
import type { ConsoleSettings } from '../../organization-settings/types/consoleSettings';
import { getEnvironmentConsoleSettings } from '../services/environmentConsoleSettings';

jest.mock('@gravitee/gamma-modules-sdk', () => ({ useEnvironment: jest.fn() }));
jest.mock('../services/environmentConsoleSettings', () => ({ getEnvironmentConsoleSettings: jest.fn() }));

const mockUseEnvironment = jest.mocked(useEnvironment);
const mockGetEnvironmentConsoleSettings = jest.mocked(getEnvironmentConsoleSettings);

function wrapper({ children }: { children: ReactNode }) {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
}

describe('useEnvironmentConsoleSettings', () => {
    afterEach(() => jest.clearAllMocks());

    it('fetches settings scoped to the current environment', async () => {
        mockUseEnvironment.mockReturnValue({ id: 'DEFAULT' } as ReturnType<typeof useEnvironment>);
        const settings: ConsoleSettings = { email: { enabled: true } };
        mockGetEnvironmentConsoleSettings.mockResolvedValue(settings);

        const { result } = renderHook(() => useEnvironmentConsoleSettings(), { wrapper });

        await waitFor(() => expect(result.current.data).toEqual(settings));
        expect(mockGetEnvironmentConsoleSettings).toHaveBeenCalledWith('DEFAULT');
    });

    it('does not fetch when there is no current environment', () => {
        mockUseEnvironment.mockReturnValue(undefined as unknown as ReturnType<typeof useEnvironment>);

        renderHook(() => useEnvironmentConsoleSettings(), { wrapper });

        expect(mockGetEnvironmentConsoleSettings).not.toHaveBeenCalled();
    });
});
