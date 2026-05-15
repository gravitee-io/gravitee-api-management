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

import { useVerifyApiProductName } from './useVerifyApiProductName';
import { verifyApiProductName } from '../services/apiProduct';

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    ...jest.requireActual<object>('@gravitee/gamma-modules-sdk'),
    useEnvironment: jest.fn(),
}));
jest.mock('../services/apiProduct', () => ({ verifyApiProductName: jest.fn() }));

const mockUseEnvironment = jest.mocked(useEnvironment);
const mockVerifyApiProductName = jest.mocked(verifyApiProductName);

const MOCK_ENV = { id: 'env-1', hrids: ['env-1'] };

function createWrapper() {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    return function Wrapper({ children }: { children: ReactNode }) {
        return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
    };
}

describe('useVerifyApiProductName', () => {
    beforeEach(() => {
        mockUseEnvironment.mockReturnValue(MOCK_ENV);
        mockVerifyApiProductName.mockResolvedValue({ ok: true });
    });

    afterEach(() => jest.clearAllMocks());

    it('does not call the service when name is empty', () => {
        renderHook(() => useVerifyApiProductName(''), { wrapper: createWrapper() });
        expect(mockVerifyApiProductName).not.toHaveBeenCalled();
    });

    it('does not call the service when env is not ready', () => {
        mockUseEnvironment.mockReturnValue(null);
        renderHook(() => useVerifyApiProductName('My Product'), { wrapper: createWrapper() });
        expect(mockVerifyApiProductName).not.toHaveBeenCalled();
    });

    it('calls the service with envId and name — no productId — when creating', async () => {
        renderHook(() => useVerifyApiProductName('My Product'), { wrapper: createWrapper() });
        await waitFor(() => expect(mockVerifyApiProductName).toHaveBeenCalledTimes(1));
        expect(mockVerifyApiProductName).toHaveBeenCalledWith('env-1', 'My Product', undefined);
    });

    it('passes productId to the service when editing an existing product (excludes self from uniqueness check)', async () => {
        renderHook(() => useVerifyApiProductName('My Product', 'prod-abc'), { wrapper: createWrapper() });
        await waitFor(() => expect(mockVerifyApiProductName).toHaveBeenCalledTimes(1));
        expect(mockVerifyApiProductName).toHaveBeenCalledWith('env-1', 'My Product', 'prod-abc');
    });

    it('returns ok: false with reason when the name is already taken', async () => {
        mockVerifyApiProductName.mockResolvedValue({ ok: false, reason: 'Name already in use.' });
        const { result } = renderHook(() => useVerifyApiProductName('Taken Name'), { wrapper: createWrapper() });
        await waitFor(() => expect(result.current.data).toBeDefined());
        expect(result.current.data).toEqual({ ok: false, reason: 'Name already in use.' });
        expect(result.current.isChecking).toBe(false);
    });
});
