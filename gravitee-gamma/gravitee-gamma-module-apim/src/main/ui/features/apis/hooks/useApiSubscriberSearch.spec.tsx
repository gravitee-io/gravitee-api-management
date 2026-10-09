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

import { useApiSubscriberSearch } from './useSubscriptions';
import { listApiSubscribers } from '../services/subscriptions';
import type { SubscriptionContext } from '../types/subscription';

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    useEnvironment: jest.fn(() => ({ id: 'DEFAULT' })),
}));

jest.mock('../services/subscriptions', () => ({
    listApiSubscribers: jest.fn(),
}));

const mockListApiSubscribers = listApiSubscribers as jest.Mock;

const CTX: SubscriptionContext = { type: 'api', entityId: 'api-1' };

function renderSearch(initialTerm: string) {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    return renderHook(({ term }) => useApiSubscriberSearch(CTX, term), {
        initialProps: { term: initialTerm },
        wrapper: ({ children }: { children: ReactNode }) => <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>,
    });
}

describe('useApiSubscriberSearch', () => {
    beforeEach(() => {
        mockListApiSubscribers.mockReset();
    });

    it('cancels the request of the previous term when the term changes', async () => {
        mockListApiSubscribers.mockImplementation(() => new Promise(() => {}));
        const { rerender } = renderSearch('che');
        await waitFor(() => expect(mockListApiSubscribers).toHaveBeenCalledTimes(1));
        const previousSignal: AbortSignal = mockListApiSubscribers.mock.calls[0][2].signal;

        rerender({ term: 'chec' });

        await waitFor(() => expect(previousSignal.aborted).toBe(true));
    });

    it('does not search while no term is typed', () => {
        renderSearch('  ');

        expect(mockListApiSubscribers).not.toHaveBeenCalled();
    });
});
