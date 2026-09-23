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
import { licenseService } from '@gravitee/gamma-modules-sdk';
import type { License } from '@gravitee/gamma-modules-sdk/types';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { act, renderHook, waitFor } from '@testing-library/react';
import type { ReactNode } from 'react';

import { useFederationEnabled } from './useFederationEnabled';
import { ApimApiError } from '../../shared/api/apimClient';
import { ENTERPRISE_LICENSE, OSS_LICENSE } from '../../testing/factories';
import { fetchOrgConsoleSettings, type OrgConsoleSettings } from '../settings/services/orgConsoleSettings';

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    ...jest.requireActual<object>('@gravitee/gamma-modules-sdk'),
    licenseService: { subscribe: jest.fn(() => () => {}), getSnapshot: jest.fn() },
}));
jest.mock('../settings/services/orgConsoleSettings', () => ({ fetchOrgConsoleSettings: jest.fn() }));

const mockLicenseSnapshot = jest.mocked(licenseService.getSnapshot);
const mockLicenseSubscribe = jest.mocked(licenseService.subscribe);
const mockFetchOrgConsoleSettings = jest.mocked(fetchOrgConsoleSettings);

const EXPIRED_ENTERPRISE_LICENSE: License = { ...ENTERPRISE_LICENSE, isExpired: true };
const UNIVERSE_LICENSE: License = { ...ENTERPRISE_LICENSE, tier: 'universe' };

function createWrapper(retry: boolean | number = false) {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry } } });
    return function Wrapper({ children }: { children: ReactNode }) {
        return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
    };
}

describe('useFederationEnabled', () => {
    beforeEach(() => mockLicenseSnapshot.mockReturnValue(ENTERPRISE_LICENSE));

    afterEach(() => jest.clearAllMocks());

    // Every Enterprise tier is entitled to federation (APIM-4273); no license ever carries a federation feature id.
    it.each<[string, License]>([
        ['enterprise', ENTERPRISE_LICENSE],
        ['universe', UNIVERSE_LICENSE],
    ])('enables federation when the org setting is on and the license tier is %s', async (_tier, license) => {
        mockLicenseSnapshot.mockReturnValue(license);
        mockFetchOrgConsoleSettings.mockResolvedValue({ federation: { enabled: true } });

        const { result } = renderHook(() => useFederationEnabled(), { wrapper: createWrapper() });

        await waitFor(() => expect(result.current.isResolved).toBe(true));
        expect(result.current.enabled).toBe(true);
    });

    it.each<[string, License | null]>([
        ['no license has been reported', null],
        ['the license tier is oss', OSS_LICENSE],
        ['the license has expired', EXPIRED_ENTERPRISE_LICENSE],
    ])('keeps federation off when the org setting is on but %s', async (_reason, license) => {
        mockLicenseSnapshot.mockReturnValue(license);
        mockFetchOrgConsoleSettings.mockResolvedValue({ federation: { enabled: true } });

        const { result } = renderHook(() => useFederationEnabled(), { wrapper: createWrapper() });

        await waitFor(() => expect(result.current.isResolved).toBe(true));
        expect(result.current.enabled).toBe(false);
    });

    it('enables federation when the host pushes an entitled license after first render', async () => {
        mockLicenseSnapshot.mockReturnValue(OSS_LICENSE);
        mockFetchOrgConsoleSettings.mockResolvedValue({ federation: { enabled: true } });

        const { result } = renderHook(() => useFederationEnabled(), { wrapper: createWrapper() });
        await waitFor(() => expect(result.current.isResolved).toBe(true));
        expect(result.current.enabled).toBe(false);

        mockLicenseSnapshot.mockReturnValue(ENTERPRISE_LICENSE);
        const notifyLicenseChanged = mockLicenseSubscribe.mock.calls.at(-1)![0];
        act(() => notifyLicenseChanged());

        await waitFor(() => expect(result.current.enabled).toBe(true));
    });

    it('keeps federation off when the license entitles it but the org setting is off', async () => {
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
        mockFetchOrgConsoleSettings.mockResolvedValue(settings);

        const { result } = renderHook(() => useFederationEnabled(), { wrapper: createWrapper() });

        await waitFor(() => expect(result.current.isResolved).toBe(true));
        expect(result.current.enabled).toBe(false);
    });

    it('fails closed on a single attempt when the org settings read rejects', async () => {
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
        mockFetchOrgConsoleSettings.mockRejectedValue(readFailure);

        const { result } = renderHook(() => useFederationEnabled(), { wrapper: createWrapper() });

        await waitFor(() => expect(result.current.isResolved).toBe(true));
        expect(warn).toHaveBeenCalledWith(expect.any(String), readFailure);
        warn.mockRestore();
    });

    it('names the HTTP status in the warning when the org settings endpoint answers with an error', async () => {
        const warn = jest.spyOn(console, 'warn').mockImplementation(() => {});
        const readFailure = new ApimApiError(503, 'Unavailable');
        mockFetchOrgConsoleSettings.mockRejectedValue(readFailure);

        const { result } = renderHook(() => useFederationEnabled(), { wrapper: createWrapper() });

        await waitFor(() => expect(result.current.isResolved).toBe(true));
        expect(warn).toHaveBeenCalledWith(expect.stringContaining('HTTP 503'), readFailure);
        warn.mockRestore();
    });
});
