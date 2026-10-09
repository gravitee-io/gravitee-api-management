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

import { useLiveDeploymentEvent } from './useApiEvents';
import { getApiEvents } from '../services/apis';
import type { ApiEvent } from '../types';

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    useEnvironment: jest.fn(() => ({ id: 'DEFAULT' })),
}));

jest.mock('../services/apis', () => ({
    getApiEvents: jest.fn(),
}));

const mockGetApiEvents = getApiEvents as jest.Mock;

const event = (id: string): ApiEvent => ({
    id,
    createdAt: '2026-10-01T10:00:00Z',
    payload: '{}',
    initiator: { id: 'u1', displayName: 'admin' },
    properties: { DEPLOYMENT_NUMBER: id },
});

function renderLiveEvent(page: number, events: ApiEvent[]) {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    return renderHook(() => useLiveDeploymentEvent('api-1', page, events), {
        wrapper: ({ children }: { children: ReactNode }) => <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>,
    });
}

describe('useLiveDeploymentEvent', () => {
    beforeEach(() => {
        mockGetApiEvents.mockReset();
    });

    it('takes the live version from the first row on page 1, without another request', () => {
        const { result } = renderLiveEvent(1, [event('newest'), event('older')]);

        expect(result.current?.id).toBe('newest');
        expect(mockGetApiEvents).not.toHaveBeenCalled();
    });

    it('fetches the newest event once the list is on another page', async () => {
        mockGetApiEvents.mockResolvedValue({ data: [event('newest')], pagination: { totalCount: 30 } });

        const { result } = renderLiveEvent(2, [event('older')]);

        await waitFor(() => expect(result.current?.id).toBe('newest'));
        expect(mockGetApiEvents).toHaveBeenCalledWith('DEFAULT', 'api-1', { page: 1, perPage: 1 });
    });
});
