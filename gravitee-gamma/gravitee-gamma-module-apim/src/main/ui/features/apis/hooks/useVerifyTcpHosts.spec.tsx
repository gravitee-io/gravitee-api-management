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
import type { ReactNode } from 'react';

import { useVerifyTcpHosts } from './useVerifyTcpHosts';
import { verifyApiHosts } from '../services/apiProxy';
import { ApiCreationProvider, useApiCreation } from '../store/apiCreationStore';

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    ...jest.requireActual<object>('@gravitee/gamma-modules-sdk'),
    useEnvironment: jest.fn(),
}));
jest.mock('../services/apiProxy', () => ({ verifyApiHosts: jest.fn() }));

const mockUseEnvironment = jest.mocked(useEnvironment);
const mockVerifyApiHosts = jest.mocked(verifyApiHosts);

function wrapper({ children }: { children: ReactNode }) {
    return <ApiCreationProvider>{children}</ApiCreationProvider>;
}

// Composite hook so we can drive the store and read its state in one renderHook call.
function useHook() {
    useVerifyTcpHosts();
    return useApiCreation();
}

describe('useVerifyTcpHosts', () => {
    beforeEach(() => {
        jest.useFakeTimers();
        mockUseEnvironment.mockReturnValue({ id: 'env-1', hrids: ['env-1'] });
        mockVerifyApiHosts.mockResolvedValue({ ok: true });
    });

    afterEach(() => {
        jest.useRealTimers();
        jest.clearAllMocks();
    });

    it('does not call the API when protocol is HTTP', async () => {
        const { result } = renderHook(() => useHook(), { wrapper });

        act(() => {
            result.current.dispatch({ type: 'UPDATE_TCP_HOST', index: 0, patch: { host: 'tcp.example.com' } });
        });

        await act(async () => {
            jest.runAllTimers();
        });

        expect(mockVerifyApiHosts).not.toHaveBeenCalled();
    });

    it('does not call the API when a host fails local validation', async () => {
        const { result } = renderHook(() => useHook(), { wrapper });

        act(() => {
            result.current.dispatch({ type: 'UPDATE_FORM', patch: { protocol: 'TCP' } });
            result.current.dispatch({ type: 'UPDATE_TCP_HOST', index: 0, patch: { host: 'not a host!' } });
        });

        await act(async () => {
            jest.runAllTimers();
        });

        expect(mockVerifyApiHosts).not.toHaveBeenCalled();
        expect(result.current.state.isPathVerifying).toBe(false);
    });

    it('calls the API with every host after the debounce delay once protocol is TCP', async () => {
        const { result } = renderHook(() => useHook(), { wrapper });

        act(() => {
            result.current.dispatch({ type: 'UPDATE_FORM', patch: { protocol: 'TCP' } });
            result.current.dispatch({ type: 'UPDATE_TCP_HOST', index: 0, patch: { host: 'tcp.example.com' } });
            result.current.dispatch({ type: 'ADD_TCP_HOST' });
            result.current.dispatch({ type: 'UPDATE_TCP_HOST', index: 1, patch: { host: 'tcp2.example.com' } });
        });

        expect(mockVerifyApiHosts).not.toHaveBeenCalled();

        await act(async () => {
            jest.runAllTimers();
        });

        expect(mockVerifyApiHosts).toHaveBeenCalledTimes(1);
        expect(mockVerifyApiHosts).toHaveBeenCalledWith('env-1', 'TCP', ['tcp.example.com', 'tcp2.example.com']);
        expect(result.current.state.isPathVerifying).toBe(false);
    });

    it('still verifies once a first host is filled in, ignoring a blank row added via "Add host"', async () => {
        const { result } = renderHook(() => useHook(), { wrapper });

        act(() => {
            result.current.dispatch({ type: 'UPDATE_FORM', patch: { protocol: 'TCP' } });
            result.current.dispatch({ type: 'UPDATE_TCP_HOST', index: 0, patch: { host: 'tcp.example.com' } });
            // Row added but left blank — should not block validation or be sent to the API.
            result.current.dispatch({ type: 'ADD_TCP_HOST' });
        });

        await act(async () => {
            jest.runAllTimers();
        });

        expect(mockVerifyApiHosts).toHaveBeenCalledTimes(1);
        expect(mockVerifyApiHosts).toHaveBeenCalledWith('env-1', 'TCP', ['tcp.example.com']);
    });

    it('sets a tcpHosts error when the API reports a host is already taken', async () => {
        mockVerifyApiHosts.mockResolvedValue({ ok: false, reason: 'Host already in use.' });
        const { result } = renderHook(() => useHook(), { wrapper });

        act(() => {
            result.current.dispatch({ type: 'UPDATE_FORM', patch: { protocol: 'TCP' } });
            result.current.dispatch({ type: 'UPDATE_TCP_HOST', index: 0, patch: { host: 'taken.example.com' } });
        });

        await act(async () => {
            jest.runAllTimers();
        });

        expect(result.current.state.validationErrors['tcpHosts']).toBe('Host already in use.');
        expect(result.current.state.isPathVerifying).toBe(false);
    });

    it('clears a stale tcpHosts error when verification later succeeds', async () => {
        mockVerifyApiHosts.mockResolvedValueOnce({ ok: false, reason: 'Host already in use.' }).mockResolvedValueOnce({ ok: true });
        const { result } = renderHook(() => useHook(), { wrapper });

        act(() => {
            result.current.dispatch({ type: 'UPDATE_FORM', patch: { protocol: 'TCP' } });
            result.current.dispatch({ type: 'UPDATE_TCP_HOST', index: 0, patch: { host: 'taken.example.com' } });
        });

        await act(async () => {
            jest.runAllTimers();
        });
        expect(result.current.state.validationErrors['tcpHosts']).toBe('Host already in use.');

        act(() => {
            result.current.dispatch({ type: 'UPDATE_TCP_HOST', index: 0, patch: { host: 'free.example.com' } });
        });

        await act(async () => {
            jest.runAllTimers();
        });

        expect(result.current.state.validationErrors).not.toHaveProperty('tcpHosts');
    });

    it('does not set an error and resets isPathVerifying on network failure', async () => {
        mockVerifyApiHosts.mockRejectedValue(new Error('network error'));
        const { result } = renderHook(() => useHook(), { wrapper });

        act(() => {
            result.current.dispatch({ type: 'UPDATE_FORM', patch: { protocol: 'TCP' } });
            result.current.dispatch({ type: 'UPDATE_TCP_HOST', index: 0, patch: { host: 'tcp.example.com' } });
        });

        await act(async () => {
            jest.runAllTimers();
        });

        expect(result.current.state.isPathVerifying).toBe(false);
        expect(result.current.state.validationErrors).not.toHaveProperty('tcpHosts');
    });
});
