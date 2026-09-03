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
import { act, renderHook } from '@testing-library/react';
import type { ReactNode } from 'react';

import { useSaveEnvironmentPortalSettings } from './useSaveEnvironmentPortalSettings';
import { getPortalSettings, savePortalSettings } from '../../security-plan-types/services/portalSettings';
import { portalSettingsKeys } from '../../security-plan-types/utils/queryKeys';

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    useEnvironment: jest.fn(),
}));

jest.mock('../../security-plan-types/services/portalSettings', () => ({
    savePortalSettings: jest.fn(),
    getPortalSettings: jest.fn(),
}));

const mockNotifySuccess = jest.fn();
const mockNotifyError = jest.fn();
jest.mock('../../../shared/notify', () => ({
    notify: {
        success: (msg: string) => mockNotifySuccess(msg),
        error: (err: unknown, fallback?: string) => mockNotifyError(err, fallback),
    },
}));

const mockUseEnvironment = jest.mocked(useEnvironment);
const mockSavePortalSettings = jest.mocked(savePortalSettings);
const mockGetPortalSettings = jest.mocked(getPortalSettings);

function wrapper(queryClient: QueryClient) {
    return function Wrapper({ children }: { children: ReactNode }) {
        return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
    };
}

describe('useSaveEnvironmentPortalSettings', () => {
    beforeEach(() => {
        mockUseEnvironment.mockReturnValue({ id: 'env-1' } as ReturnType<typeof useEnvironment>);
        mockSavePortalSettings.mockReset();
        mockGetPortalSettings.mockReset();
        mockSavePortalSettings.mockResolvedValue({ cors: { maxAge: 60 } });
        mockGetPortalSettings.mockResolvedValue({ cors: { maxAge: 60 }, logging: { maxDurationMillis: 15000 } });
        mockNotifySuccess.mockReset();
        mockNotifyError.mockReset();
    });

    it('posts portal settings, writes the re-fetched document into the env query, and toasts success', async () => {
        const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } });
        const setQueryDataSpy = jest.spyOn(queryClient, 'setQueryData');
        const { result } = renderHook(() => useSaveEnvironmentPortalSettings(), { wrapper: wrapper(queryClient) });

        await act(async () => {
            await result.current.mutateAsync({ cors: { maxAge: 60 } });
        });

        expect(mockSavePortalSettings).toHaveBeenCalledWith('env-1', { cors: { maxAge: 60 } });
        expect(mockGetPortalSettings).toHaveBeenCalledWith('env-1');
        expect(setQueryDataSpy).toHaveBeenCalledWith(portalSettingsKeys.env('env-1'), {
            cors: { maxAge: 60 },
            logging: { maxDurationMillis: 15000 },
        });
        expect(mockNotifySuccess).toHaveBeenCalledWith('Configuration successfully saved!');
    });

    it('toasts a configuration error when save fails', async () => {
        mockSavePortalSettings.mockRejectedValue(new Error('boom'));
        const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } });
        const { result } = renderHook(() => useSaveEnvironmentPortalSettings(), { wrapper: wrapper(queryClient) });

        await act(async () => {
            await result.current.mutateAsync({ cors: { maxAge: 60 } }).catch(() => undefined);
        });

        expect(mockNotifyError).toHaveBeenCalledWith(expect.any(Error), 'An error occurred while saving the configuration.');
        expect(mockGetPortalSettings).not.toHaveBeenCalled();
    });
});
