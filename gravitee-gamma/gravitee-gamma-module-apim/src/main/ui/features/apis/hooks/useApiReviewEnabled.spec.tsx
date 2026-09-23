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
import { act, renderHook, waitFor } from '@testing-library/react';
import type { ReactNode } from 'react';

import { useApiReviewEnabled } from './useApiReviewEnabled';
import { useApiScoreEnabled } from './useApiScoreEnabled';
import { PORTAL_SETTINGS_CHANGED_EVENT } from './useEnvironmentPortalConfiguration';
import { getEnvironmentPortalConfiguration } from '../../settings/services/portalSettings';

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    useEnvironment: jest.fn(() => ({ id: 'DEFAULT' })),
}));

jest.mock('../../settings/services/portalSettings', () => ({
    getEnvironmentPortalConfiguration: jest.fn(),
}));

const mockUseEnvironment = useEnvironment as jest.MockedFunction<typeof useEnvironment>;
const mockGetPortal = getEnvironmentPortalConfiguration as jest.MockedFunction<typeof getEnvironmentPortalConfiguration>;

function createWrapper() {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    return function Wrapper({ children }: { children: ReactNode }) {
        return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
    };
}

describe('useApiReviewEnabled', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        mockUseEnvironment.mockReturnValue({ id: 'DEFAULT' } as ReturnType<typeof useEnvironment>);
    });

    it('is enabled when GET /portal reports apiReview.enabled', async () => {
        mockGetPortal.mockResolvedValue({ apiReview: { enabled: true } });
        const { result } = renderHook(() => useApiReviewEnabled(), { wrapper: createWrapper() });

        await waitFor(() => expect(result.current.isFetched).toBe(true));
        expect(mockGetPortal).toHaveBeenCalledWith('DEFAULT');
        expect(result.current.enabled).toBe(true);
    });

    it('is disabled when the flag is off or missing', async () => {
        mockGetPortal.mockResolvedValue({ apiScore: { enabled: true } });
        const { result } = renderHook(() => useApiReviewEnabled(), { wrapper: createWrapper() });

        await waitFor(() => expect(result.current.isFetched).toBe(true));
        expect(result.current.enabled).toBe(false);
    });

    it('shares one GET /portal request with the API Score flag', async () => {
        mockGetPortal.mockResolvedValue({ apiScore: { enabled: true }, apiReview: { enabled: true } });
        const wrapper = createWrapper();
        const { result } = renderHook(() => ({ review: useApiReviewEnabled(), score: useApiScoreEnabled() }), { wrapper });

        await waitFor(() => expect(result.current.review.isFetched).toBe(true));
        expect(result.current.score.enabled).toBe(true);
        expect(result.current.review.enabled).toBe(true);
        expect(mockGetPortal).toHaveBeenCalledTimes(1);
    });

    it('picks up a flag toggled in the platform module on the next mount, without waiting for staleTime', async () => {
        // The platform settings page runs as its own federated module with its own QueryClient, so its
        // invalidate after a save cannot reach this cache. The flag is re-read whenever an API screen mounts.
        mockGetPortal.mockResolvedValue({ apiReview: { enabled: true } });
        const wrapper = createWrapper();

        const first = renderHook(() => useApiReviewEnabled(), { wrapper });
        await waitFor(() => expect(first.result.current.isFetched).toBe(true));
        expect(first.result.current.enabled).toBe(true);
        first.unmount();

        mockGetPortal.mockResolvedValue({ apiReview: { enabled: false } });
        const second = renderHook(() => useApiReviewEnabled(), { wrapper });

        await waitFor(() => expect(second.result.current.enabled).toBe(false));
        expect(mockGetPortal).toHaveBeenCalledTimes(2);
    });

    it('drops the cached flags as soon as the platform module broadcasts a save', async () => {
        // The settings page cannot invalidate this module's cache directly, so it broadcasts instead.
        mockGetPortal.mockResolvedValue({ apiReview: { enabled: true } });
        const { result } = renderHook(() => useApiReviewEnabled(), { wrapper: createWrapper() });
        await waitFor(() => expect(result.current.isFetched).toBe(true));
        expect(result.current.enabled).toBe(true);

        mockGetPortal.mockResolvedValue({ apiReview: { enabled: false } });
        act(() => {
            window.dispatchEvent(new CustomEvent(PORTAL_SETTINGS_CHANGED_EVENT));
        });

        await waitFor(() => expect(result.current.enabled).toBe(false));
        expect(mockGetPortal).toHaveBeenCalledTimes(2);
    });

    it('stays disabled without an environment', () => {
        mockUseEnvironment.mockReturnValue(undefined as unknown as ReturnType<typeof useEnvironment>);
        const { result } = renderHook(() => useApiReviewEnabled(), { wrapper: createWrapper() });
        expect(result.current.enabled).toBe(false);
        expect(mockGetPortal).not.toHaveBeenCalled();
    });
});
