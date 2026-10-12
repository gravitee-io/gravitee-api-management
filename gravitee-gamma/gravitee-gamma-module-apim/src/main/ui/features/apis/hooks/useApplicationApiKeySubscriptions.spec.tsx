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

import { useCreateSubscription } from './useSubscriptionActions';
import { useApplicationApiKeySubscriptions } from './useSubscriptions';
import { createSubscription, listApplicationApiKeySubscriptions } from '../services/subscriptions';
import { apiSubscriptionKeys } from '../utils/queryKeys';

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    useEnvironment: jest.fn(() => ({ id: 'DEFAULT' })),
}));

jest.mock('../services/subscriptions', () => {
    const actual = jest.requireActual('../services/subscriptions');
    return {
        ...actual,
        createSubscription: jest.fn(),
        listApplicationApiKeySubscriptions: jest.fn(),
    };
});

const mockCreate = createSubscription as jest.Mock;
const mockList = listApplicationApiKeySubscriptions as jest.Mock;

function createWrapper(queryClient: QueryClient) {
    return function Wrapper({ children }: { children: ReactNode }) {
        return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
    };
}

describe('useApplicationApiKeySubscriptions', () => {
    beforeEach(() => {
        mockList.mockReset();
    });

    it('does not request subscriptions until the lookup is enabled', () => {
        const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
        renderHook(() => useApplicationApiKeySubscriptions('app-1', false), { wrapper: createWrapper(queryClient) });
        expect(mockList).not.toHaveBeenCalled();
    });

    it('loads the application API key subscriptions when enabled', async () => {
        mockList.mockResolvedValue([{ api: 'other-api' }]);
        const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
        const { result } = renderHook(() => useApplicationApiKeySubscriptions('app-1', true), {
            wrapper: createWrapper(queryClient),
        });

        await waitFor(() => expect(result.current.isSuccess).toBe(true));
        expect(mockList).toHaveBeenCalledWith('DEFAULT', 'app-1');
        expect(result.current.data).toEqual([{ api: 'other-api' }]);
    });
});

describe('useCreateSubscription', () => {
    it('drops the cached application API key lookup after a subscription is created', async () => {
        mockCreate.mockResolvedValue({ id: 'sub-1' });
        const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
        const invalidate = jest.spyOn(queryClient, 'invalidateQueries');
        const { result } = renderHook(() => useCreateSubscription({ type: 'api', entityId: 'api-1' }), {
            wrapper: createWrapper(queryClient),
        });

        await result.current.mutateAsync({ planId: 'plan-1', applicationId: 'app-1' });

        expect(invalidate).toHaveBeenCalledWith({
            queryKey: apiSubscriptionKeys.applicationApiKeySubscriptions('DEFAULT', 'app-1'),
        });
    });
});
