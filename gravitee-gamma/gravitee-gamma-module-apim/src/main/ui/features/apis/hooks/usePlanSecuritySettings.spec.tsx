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

import { toPlanSecuritySettings, useCustomApiKeyEnabled, usePlanSecuritySettings, useSharedApiKeyEnabled } from './usePlanSecuritySettings';
import { getEnvironmentPortalConfiguration, getEnvironmentPortalSettings } from '../../settings/services/portalSettings';

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    useEnvironment: jest.fn(() => ({ id: 'DEFAULT' })),
}));

jest.mock('../../settings/services/portalSettings', () => ({
    getEnvironmentPortalConfiguration: jest.fn(),
    getEnvironmentPortalSettings: jest.fn(),
}));

const mockUseEnvironment = useEnvironment as jest.MockedFunction<typeof useEnvironment>;
const mockGetPortal = getEnvironmentPortalConfiguration as jest.MockedFunction<typeof getEnvironmentPortalConfiguration>;
const mockGetSettings = getEnvironmentPortalSettings as jest.MockedFunction<typeof getEnvironmentPortalSettings>;

function createWrapper() {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    return function Wrapper({ children }: { children: ReactNode }) {
        return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
    };
}

describe('toPlanSecuritySettings', () => {
    it('maps GET /settings plan.security JSON keys to PlanSecurityType flags', () => {
        expect(
            toPlanSecuritySettings({
                plan: {
                    security: {
                        apikey: { enabled: false },
                        jwt: { enabled: true },
                        oauth2: { enabled: false },
                        mtls: { enabled: true },
                        keyless: { enabled: false },
                    },
                },
            }),
        ).toEqual({
            API_KEY: false,
            JWT: true,
            OAUTH2: false,
            MTLS: true,
            KEY_LESS: false,
        });
    });

    it('defaults absent keys and an empty payload to enabled', () => {
        expect(toPlanSecuritySettings(undefined)).toEqual({
            API_KEY: true,
            JWT: true,
            OAUTH2: true,
            MTLS: true,
            KEY_LESS: true,
        });
        expect(toPlanSecuritySettings({ plan: { security: { apikey: { enabled: false } } } })).toEqual({
            API_KEY: false,
            JWT: true,
            OAUTH2: true,
            MTLS: true,
            KEY_LESS: true,
        });
    });

    it('treats enabled: false as disabled even when other fields are present', () => {
        expect(toPlanSecuritySettings({ plan: { security: { jwt: { enabled: false } } } }).JWT).toBe(false);
    });
});

describe('usePlanSecuritySettings', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        mockUseEnvironment.mockReturnValue({ id: 'DEFAULT' } as ReturnType<typeof useEnvironment>);
    });

    it('is loading until GET /settings resolves, then exposes mapped flags', async () => {
        let resolveSettings!: (value: Awaited<ReturnType<typeof getEnvironmentPortalSettings>>) => void;
        mockGetSettings.mockReturnValue(
            new Promise(resolve => {
                resolveSettings = resolve;
            }),
        );

        const { result } = renderHook(() => usePlanSecuritySettings(), { wrapper: createWrapper() });

        expect(result.current.isLoading).toBe(true);

        resolveSettings({
            plan: {
                security: {
                    apikey: { enabled: false },
                    jwt: { enabled: true },
                    oauth2: { enabled: true },
                    mtls: { enabled: true },
                    keyless: { enabled: true },
                },
            },
        });

        await waitFor(() => expect(result.current.isLoading).toBe(false));
        expect(mockGetSettings).toHaveBeenCalledWith('DEFAULT');
        expect(result.current.planSecuritySettings.API_KEY).toBe(false);
        expect(result.current.planSecuritySettings.JWT).toBe(true);
    });

    it('fails open to all enabled when GET /settings rejects', async () => {
        mockGetSettings.mockRejectedValue(new Error('network'));
        const { result } = renderHook(() => usePlanSecuritySettings(), { wrapper: createWrapper() });

        await waitFor(() => expect(result.current.isLoading).toBe(false));
        expect(result.current.planSecuritySettings).toEqual({
            API_KEY: true,
            JWT: true,
            OAUTH2: true,
            MTLS: true,
            KEY_LESS: true,
        });
    });
});

describe('custom and shared API key flags', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        mockUseEnvironment.mockReturnValue({ id: 'DEFAULT' } as ReturnType<typeof useEnvironment>);
        mockGetSettings.mockRejectedValue(new Error('403'));
    });

    it('reads custom and shared API keys from GET /portal and stays off while that response is missing', async () => {
        mockGetPortal.mockResolvedValue({
            plan: { security: { customApiKey: { enabled: true }, sharedApiKey: { enabled: true } } },
        });
        const { result } = renderHook(() => ({ custom: useCustomApiKeyEnabled(), shared: useSharedApiKeyEnabled() }), {
            wrapper: createWrapper(),
        });

        expect(result.current).toEqual({ custom: false, shared: false });
        await waitFor(() => expect(result.current).toEqual({ custom: true, shared: true }));
        expect(mockGetPortal).toHaveBeenCalledWith('DEFAULT');
        expect(mockGetSettings).not.toHaveBeenCalled();
    });

    it('stays off when GET /portal does not enable the flags', async () => {
        mockGetPortal.mockResolvedValue({ plan: { security: { customApiKey: { enabled: false } } } });
        const { result } = renderHook(() => useCustomApiKeyEnabled(), { wrapper: createWrapper() });

        await waitFor(() => expect(mockGetPortal).toHaveBeenCalled());
        expect(result.current).toBe(false);
        expect(mockGetSettings).not.toHaveBeenCalled();
    });
});
