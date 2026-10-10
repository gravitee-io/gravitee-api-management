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

import { useIntegrationIngestedApis } from './useIntegrationIngestedApis';
import { ApimApiError } from '../../../shared/api/apimClient';
import { listIngestedApis } from '../services/integrationDetail';
import { INGESTION_POLL_INTERVAL_MS } from '../utils/ingestion';

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    useEnvironment: jest.fn(),
}));

jest.mock('../services/integrationDetail');

const mockUseEnvironment = jest.mocked(useEnvironment);
const mockListIngestedApis = jest.mocked(listIngestedApis);

const NO_INGESTED_APIS = { data: [], pagination: { page: 1, perPage: 10, pageCount: 0, pageItemsCount: 0, totalCount: 0 } };

function wrapper({ children }: { children: ReactNode }) {
    const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    return <QueryClientProvider client={client}>{children}</QueryClientProvider>;
}

function renderIngestedApisHook(isIngesting: boolean) {
    return renderHook(
        ({ ingesting }: { ingesting: boolean }) => useIntegrationIngestedApis('int-1', { page: 2, perPage: 25, isIngesting: ingesting }),
        { wrapper, initialProps: { ingesting: isIngesting } },
    );
}

describe('useIntegrationIngestedApis', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        mockUseEnvironment.mockReturnValue({ id: 'env-1' });
        mockListIngestedApis.mockResolvedValue(NO_INGESTED_APIS);
    });

    afterEach(() => {
        jest.useRealTimers();
    });

    it('requests the given page of ingested APIs of the integration in the current environment', async () => {
        const { result } = renderIngestedApisHook(false);

        await waitFor(() => expect(result.current.data).toEqual(NO_INGESTED_APIS));
        expect(mockListIngestedApis).toHaveBeenCalledWith('env-1', 'int-1', { page: 2, perPage: 25 });
    });

    it('sends no request when there is no current environment', async () => {
        mockUseEnvironment.mockReturnValue(undefined);

        const { result } = renderIngestedApisHook(false);

        await act(() => new Promise(resolve => setTimeout(resolve, 0)));
        expect(result.current.fetchStatus).toBe('idle');
        expect(result.current.status).toBe('pending');
        expect(mockListIngestedApis).not.toHaveBeenCalled();
    });

    it('surfaces a failed request as the error state', async () => {
        const failure = new ApimApiError(500, 'Internal Server Error');
        mockListIngestedApis.mockRejectedValue(failure);

        const { result } = renderIngestedApisHook(false);

        await waitFor(() => expect(result.current.isError).toBe(true));
        expect(result.current.error).toBe(failure);
    });

    it.each([
        ['refreshes the list on every poll interval while ingestion is in progress', true, 3],
        ['does not refresh the list when no ingestion is in progress', false, 1],
    ])('%s', async (_, isIngesting, expectedRequests) => {
        jest.useFakeTimers();

        renderIngestedApisHook(isIngesting);

        await waitFor(() => expect(mockListIngestedApis).toHaveBeenCalledTimes(1));
        await act(() => jest.advanceTimersByTimeAsync(2 * INGESTION_POLL_INTERVAL_MS));
        expect(mockListIngestedApis).toHaveBeenCalledTimes(expectedRequests);
    });

    it('refreshes the list exactly once when ingestion ends, then stops refreshing', async () => {
        jest.useFakeTimers();
        const { rerender } = renderIngestedApisHook(true);
        await waitFor(() => expect(mockListIngestedApis).toHaveBeenCalledTimes(1));

        rerender({ ingesting: false });

        await waitFor(() => expect(mockListIngestedApis).toHaveBeenCalledTimes(2));
        await act(() => jest.advanceTimersByTimeAsync(2 * INGESTION_POLL_INTERVAL_MS));
        expect(mockListIngestedApis).toHaveBeenCalledTimes(2);
    });
});
