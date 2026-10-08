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
import { act, renderHook } from '@testing-library/react';
import { useState } from 'react';

import { useTcpHostUniqueness } from './useTcpHostUniqueness';
import { verifyApiHosts } from '../services/apiProxy';
import type { TcpHostEntry } from '../types/apiCreation';

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    ...jest.requireActual<object>('@gravitee/gamma-modules-sdk'),
    useEnvironment: jest.fn(),
}));
jest.mock('../services/apiProxy', () => ({ verifyApiHosts: jest.fn() }));

const mockUseEnvironment = jest.mocked(useEnvironment);
const mockVerifyApiHosts = jest.mocked(verifyApiHosts);

function useHarness(initial: TcpHostEntry[], apiId: string | undefined = 'api-1') {
    const [hosts, setHosts] = useState(initial);
    const uniqueness = useTcpHostUniqueness(apiId, hosts);
    return { ...uniqueness, setHosts };
}

describe('useTcpHostUniqueness', () => {
    beforeEach(() => {
        jest.useFakeTimers();
        mockUseEnvironment.mockReturnValue({ id: 'env-1', hrids: ['env-1'] });
        mockVerifyApiHosts.mockResolvedValue({ ok: true });
    });

    afterEach(() => {
        jest.useRealTimers();
        jest.clearAllMocks();
    });

    it('does not verify hosts that were loaded and not edited', async () => {
        renderHook(() => useHarness([{ id: 'row-1', host: 'tcp.example.com' }]));

        await act(async () => {
            jest.runAllTimers();
        });

        expect(mockVerifyApiHosts).not.toHaveBeenCalled();
    });

    it('verifies an edited host with the current API id after 250ms', async () => {
        const { result } = renderHook(() => useHarness([{ id: 'row-1', host: 'tcp.example.com' }]));

        act(() => {
            result.current.markDirty('row-1');
            result.current.setHosts([{ id: 'row-1', host: 'taken.example.com' }]);
        });

        expect(mockVerifyApiHosts).not.toHaveBeenCalled();

        await act(async () => {
            jest.advanceTimersByTime(249);
        });
        expect(mockVerifyApiHosts).not.toHaveBeenCalled();

        await act(async () => {
            jest.advanceTimersByTime(1);
        });

        expect(mockVerifyApiHosts).toHaveBeenCalledTimes(1);
        expect(mockVerifyApiHosts).toHaveBeenCalledWith('env-1', 'TCP', ['taken.example.com'], 'api-1');
    });

    it('reports the verify reason and clears it when a later check succeeds', async () => {
        mockVerifyApiHosts.mockResolvedValueOnce({ ok: false, reason: 'Host already exists' }).mockResolvedValueOnce({ ok: true });
        const { result } = renderHook(() => useHarness([{ id: 'row-1', host: '' }]));

        act(() => {
            result.current.markDirty('row-1');
            result.current.setHosts([{ id: 'row-1', host: 'taken.example.com' }]);
        });
        await act(async () => {
            jest.runAllTimers();
        });

        expect(result.current.errorsById['row-1']).toBe('Host already exists');
        expect(result.current.pending).toBe(false);

        act(() => {
            result.current.markDirty('row-1');
            result.current.setHosts([{ id: 'row-1', host: 'free.example.com' }]);
        });
        await act(async () => {
            jest.runAllTimers();
        });

        expect(result.current.errorsById).not.toHaveProperty('row-1');
    });

    it('does not call verify while the host fails local validation', async () => {
        const { result } = renderHook(() => useHarness([{ id: 'row-1', host: '' }]));

        act(() => {
            result.current.markDirty('row-1');
            result.current.setHosts([{ id: 'row-1', host: 'not a host!' }]);
        });
        await act(async () => {
            jest.runAllTimers();
        });

        expect(mockVerifyApiHosts).not.toHaveBeenCalled();
        expect(result.current.pending).toBe(false);
    });

    it('keeps the host blocked when verify fails on the network', async () => {
        mockVerifyApiHosts.mockRejectedValue(new Error('network error'));
        const { result } = renderHook(() => useHarness([{ id: 'row-1', host: '' }]));

        act(() => {
            result.current.markDirty('row-1');
            result.current.setHosts([{ id: 'row-1', host: 'tcp.example.com' }]);
        });
        await act(async () => {
            jest.runAllTimers();
        });

        expect(result.current.pending).toBe(false);
        expect(result.current.errorsById['row-1']).toBe('Unable to verify this host. Save stays disabled until the check succeeds.');
    });

    it('verifies a host marked dirty without a separate host update', async () => {
        const { result } = renderHook(() => useHarness([{ id: 'row-1', host: 'taken.example.com' }]));

        act(() => {
            result.current.markDirty('row-1');
        });
        await act(async () => {
            jest.runAllTimers();
        });

        expect(mockVerifyApiHosts).toHaveBeenCalledWith('env-1', 'TCP', ['taken.example.com'], 'api-1');
    });

    it('drops an in-flight result after reset', async () => {
        let resolveVerify: (value: { ok: boolean; reason?: string }) => void = () => {};
        mockVerifyApiHosts.mockImplementation(
            () =>
                new Promise(resolve => {
                    resolveVerify = resolve;
                }),
        );
        const { result } = renderHook(() => useHarness([{ id: 'row-1', host: 'tcp.example.com' }]));

        act(() => {
            result.current.markDirty('row-1');
            result.current.setHosts([{ id: 'row-1', host: 'taken.example.com' }]);
        });
        await act(async () => {
            jest.advanceTimersByTime(250);
        });

        act(() => {
            result.current.reset();
        });
        await act(async () => {
            resolveVerify({ ok: false, reason: 'Host already exists' });
        });

        expect(result.current.errorsById).toEqual({});
        expect(result.current.pending).toBe(false);
    });
});
