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

import { useCustomApiKeyCheck } from './useCustomApiKeyCheck';
import { verifyCustomApiKey } from '../services/subscriptions';
import type { SubscriptionContext } from '../types/subscription';

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    useEnvironment: jest.fn(() => ({ id: 'DEFAULT' })),
}));

jest.mock('../services/subscriptions', () => ({
    verifyCustomApiKey: jest.fn(),
}));

const mockVerify = verifyCustomApiKey as jest.Mock;
const CTX: SubscriptionContext = { type: 'api', entityId: 'api-1' };

function createWrapper() {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    return function Wrapper({ children }: { children: ReactNode }) {
        return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
    };
}

describe('useCustomApiKeyCheck', () => {
    beforeEach(() => {
        mockVerify.mockReset();
    });

    it('allows an empty key without calling verify', () => {
        const { result } = renderHook(() => useCustomApiKeyCheck(CTX, 'app-1', ''), { wrapper: createWrapper() });
        expect(result.current).toEqual({ error: null, checking: false, blocked: false });
        expect(mockVerify).not.toHaveBeenCalled();
    });

    it('blocks a key that fails the format rules without calling verify', () => {
        const { result } = renderHook(() => useCustomApiKeyCheck(CTX, 'app-1', 'short'), { wrapper: createWrapper() });
        expect(result.current.error).toBe('Must be longer than 8 characters');
        expect(result.current.blocked).toBe(true);
        expect(mockVerify).not.toHaveBeenCalled();
    });

    it('blocks while uniqueness is still being checked', () => {
        mockVerify.mockImplementation(() => new Promise(() => undefined));
        const { result } = renderHook(() => useCustomApiKeyCheck(CTX, 'app-1', 'validkey1'), { wrapper: createWrapper() });
        expect(result.current.checking).toBe(true);
        expect(result.current.blocked).toBe(true);
    });

    it('blocks a key the server reports as already used', async () => {
        mockVerify.mockResolvedValue({ ok: false });
        const { result } = renderHook(() => useCustomApiKeyCheck(CTX, 'app-1', 'validkey1'), { wrapper: createWrapper() });

        await waitFor(() => expect(result.current.error).toBe('Key already exists with given value'));
        expect(result.current.blocked).toBe(true);
        expect(mockVerify).toHaveBeenCalledWith('DEFAULT', CTX, { applicationId: 'app-1', apiKey: 'validkey1' });
    });

    it('allows a key verify accepts', async () => {
        mockVerify.mockResolvedValue({ ok: true });
        const { result } = renderHook(() => useCustomApiKeyCheck(CTX, 'app-1', 'validkey1'), { wrapper: createWrapper() });

        await waitFor(() => expect(result.current.blocked).toBe(false));
        expect(result.current.error).toBeNull();
    });

    it('blocks when verify cannot be completed', async () => {
        mockVerify.mockRejectedValue(new Error('down'));
        const { result } = renderHook(() => useCustomApiKeyCheck(CTX, 'app-1', 'validkey1'), { wrapper: createWrapper() });

        await waitFor(() => expect(result.current.error).toBe('Could not verify this API key.'));
        expect(result.current.blocked).toBe(true);
    });
});
