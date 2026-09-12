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
import { useHasFeature } from '@gravitee/gamma-modules-sdk';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { renderHook, waitFor } from '@testing-library/react';
import type { ReactNode } from 'react';

import { useFederationEnabled } from './useFederationEnabled';
import { fetchOrgConsoleSettings, type OrgConsoleSettings } from '../settings/services/orgConsoleSettings';

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    ...jest.requireActual<object>('@gravitee/gamma-modules-sdk'),
    useHasFeature: jest.fn(),
}));
jest.mock('../settings/services/orgConsoleSettings', () => ({ fetchOrgConsoleSettings: jest.fn() }));

const mockUseHasFeature = jest.mocked(useHasFeature);
const mockFetchOrgConsoleSettings = jest.mocked(fetchOrgConsoleSettings);

function createWrapper(retry: boolean | number = false) {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry } } });
    return function Wrapper({ children }: { children: ReactNode }) {
        return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
    };
}

describe('useFederationEnabled', () => {
    afterEach(() => jest.clearAllMocks());

    it('enables federation when the org setting is on and the license carries the feature', async () => {
        mockUseHasFeature.mockReturnValue(true);
        mockFetchOrgConsoleSettings.mockResolvedValue({ federation: { enabled: true } });

        const { result } = renderHook(() => useFederationEnabled(), { wrapper: createWrapper() });

        await waitFor(() => expect(result.current.isResolved).toBe(true));
        expect(result.current.enabled).toBe(true);
        // The classic console registers this feature without the `apim-` prefix the other APIM ids carry;
        // an `apim-federation` key would read as unlicensed on every real deployment.
        expect(mockUseHasFeature).toHaveBeenCalledWith('federation');
    });

    it('keeps federation off when the org setting is on but the license lacks the feature', async () => {
        mockUseHasFeature.mockReturnValue(false);
        mockFetchOrgConsoleSettings.mockResolvedValue({ federation: { enabled: true } });

        const { result } = renderHook(() => useFederationEnabled(), { wrapper: createWrapper() });

        await waitFor(() => expect(result.current.isResolved).toBe(true));
        expect(result.current.enabled).toBe(false);
    });

    it('keeps federation off when the license carries the feature but the org setting is off', async () => {
        mockUseHasFeature.mockReturnValue(true);
        mockFetchOrgConsoleSettings.mockResolvedValue({ federation: { enabled: false } });

        const { result } = renderHook(() => useFederationEnabled(), { wrapper: createWrapper() });

        await waitFor(() => expect(result.current.isResolved).toBe(true));
        expect(result.current.enabled).toBe(false);
    });

    // `enabled` is a nullable Boolean on the backend, so both shapes are real wire states.
    it.each<[string, OrgConsoleSettings]>([
        ['the console configuration carries no federation object', {}],
        ['the federation object carries no enabled flag', { federation: {} }],
    ])('keeps federation off, as if explicitly disabled, when %s', async (_shape, settings) => {
        mockUseHasFeature.mockReturnValue(true);
        mockFetchOrgConsoleSettings.mockResolvedValue(settings);

        const { result } = renderHook(() => useFederationEnabled(), { wrapper: createWrapper() });

        await waitFor(() => expect(result.current.isResolved).toBe(true));
        expect(result.current.enabled).toBe(false);
    });

    it('fails closed on a single attempt when the org settings read rejects', async () => {
        mockUseHasFeature.mockReturnValue(true);
        mockFetchOrgConsoleSettings.mockRejectedValue(new Error('console settings unreachable'));

        // The app's QueryClient retries three times with backoff; inheriting that would hold
        // isResolved false for seconds and stall the list this gate is meant to release.
        const { result } = renderHook(() => useFederationEnabled(), { wrapper: createWrapper(3) });

        await waitFor(() => expect(result.current.isResolved).toBe(true));
        expect(result.current.enabled).toBe(false);
        expect(mockFetchOrgConsoleSettings).toHaveBeenCalledTimes(1);
    });

    it('does not switch federation off silently when the org settings read rejects', async () => {
        const warn = jest.spyOn(console, 'warn').mockImplementation(() => {});
        const readFailure = new Error('console settings unreachable');
        mockUseHasFeature.mockReturnValue(true);
        mockFetchOrgConsoleSettings.mockRejectedValue(readFailure);

        const { result } = renderHook(() => useFederationEnabled(), { wrapper: createWrapper() });

        await waitFor(() => expect(result.current.isResolved).toBe(true));
        expect(warn).toHaveBeenCalledWith(expect.any(String), readFailure);
        warn.mockRestore();
    });
});
