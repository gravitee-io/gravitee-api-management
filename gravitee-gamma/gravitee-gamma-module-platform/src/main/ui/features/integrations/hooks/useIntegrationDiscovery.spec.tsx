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
import { act, renderHook, waitFor } from '@testing-library/react';
import type { ReactNode } from 'react';

import { useEnvironment } from '@gravitee/gamma-modules-sdk';

import { useIntegrationDiscovery } from './useIntegrationDiscovery';
import { ApimApiError } from '../../../shared/api/apimClient';
import { previewIntegration } from '../services/integrationDetail';
import type { IntegrationPreview } from '../types/integration';

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    useEnvironment: jest.fn(),
}));

jest.mock('../services/integrationDetail');

const mockUseEnvironment = jest.mocked(useEnvironment);
const mockPreviewIntegration = jest.mocked(previewIntegration);

const PREVIEW: IntegrationPreview = {
    isPartiallyDiscovered: false,
    totalCount: 1,
    newCount: 1,
    updateCount: 0,
    apis: [{ id: 'api-1', name: 'Orders', version: '1.0', state: 'NEW' }],
};

// The client retries once with no delay so a hook that stops opting out of retries is detected.
function wrapper({ children }: { children: ReactNode }) {
    const client = new QueryClient({ defaultOptions: { queries: { retry: 1, retryDelay: 0 } } });
    return <QueryClientProvider client={client}>{children}</QueryClientProvider>;
}

function renderDiscoveryHook(integrationId: string, enabled: boolean) {
    return renderHook(() => useIntegrationDiscovery(integrationId, enabled), { wrapper });
}

async function flushPendingWork() {
    await act(() => new Promise(resolve => setTimeout(resolve, 0)));
}

describe('useIntegrationDiscovery', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        mockUseEnvironment.mockReturnValue({ id: 'env-1' });
        mockPreviewIntegration.mockResolvedValue(PREVIEW);
    });

    it('discovers the APIs of the integration in the current environment', async () => {
        const { result } = renderDiscoveryHook('int-1', true);

        await waitFor(() => expect(result.current.data).toEqual(PREVIEW));
        expect(mockPreviewIntegration).toHaveBeenCalledTimes(1);
        expect(mockPreviewIntegration).toHaveBeenCalledWith('env-1', 'int-1');
    });

    it('sends no request while discovery is disabled', async () => {
        const { result } = renderDiscoveryHook('int-1', false);

        await flushPendingWork();
        expect(result.current.fetchStatus).toBe('idle');
        expect(mockPreviewIntegration).not.toHaveBeenCalled();
    });

    it('sends no request when there is no current environment', async () => {
        mockUseEnvironment.mockReturnValue(undefined);

        const { result } = renderDiscoveryHook('int-1', true);

        await flushPendingWork();
        expect(result.current.fetchStatus).toBe('idle');
        expect(mockPreviewIntegration).not.toHaveBeenCalled();
    });

    it('sends no request when the integration id is empty', async () => {
        const { result } = renderDiscoveryHook('', true);

        await flushPendingWork();
        expect(result.current.fetchStatus).toBe('idle');
        expect(mockPreviewIntegration).not.toHaveBeenCalled();
    });

    it('surfaces a failed discovery as the error state without retrying it', async () => {
        const failure = new ApimApiError(503, 'Service Unavailable');
        mockPreviewIntegration.mockRejectedValue(failure);

        const { result } = renderDiscoveryHook('int-1', true);

        await waitFor(() => expect(result.current.isError).toBe(true));
        expect(result.current.error).toBe(failure);
        await flushPendingWork();
        expect(mockPreviewIntegration).toHaveBeenCalledTimes(1);
    });
});
