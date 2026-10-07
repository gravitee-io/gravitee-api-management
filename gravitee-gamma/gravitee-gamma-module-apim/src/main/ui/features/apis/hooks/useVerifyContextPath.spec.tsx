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

import { useVerifyContextPath } from './useVerifyContextPath';
import { verifyContextPath } from '../services/apiProxy';
import { ApiCreationProvider, useApiCreation } from '../store/apiCreationStore';

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    ...jest.requireActual<object>('@gravitee/gamma-modules-sdk'),
    useEnvironment: jest.fn(),
}));
jest.mock('../services/apiProxy', () => ({ verifyContextPath: jest.fn() }));

const mockUseEnvironment = jest.mocked(useEnvironment);
const mockVerifyContextPath = jest.mocked(verifyContextPath);

function wrapper({ children }: { children: ReactNode }) {
    return <ApiCreationProvider>{children}</ApiCreationProvider>;
}

// Composite hook so we can drive the store and read its state in one renderHook call.
function useHook() {
    useVerifyContextPath();
    return useApiCreation();
}

describe('useVerifyContextPath', () => {
    beforeEach(() => {
        jest.useFakeTimers();
        mockUseEnvironment.mockReturnValue({ id: 'env-1', hrids: ['env-1'] });
        mockVerifyContextPath.mockResolvedValue({ ok: true });
    });

    afterEach(() => {
        jest.useRealTimers();
        jest.clearAllMocks();
    });

    it('does not call the API when contextPath is empty', async () => {
        renderHook(() => useHook(), { wrapper });

        await act(async () => {
            jest.runAllTimers();
        });

        expect(mockVerifyContextPath).not.toHaveBeenCalled();
    });

    it('does not call the API when contextPath fails local validation (too short)', async () => {
        const { result } = renderHook(() => useHook(), { wrapper });

        act(() => {
            result.current.dispatch({ type: 'UPDATE_FORM', patch: { contextPath: '/ab' } });
        });

        await act(async () => {
            jest.runAllTimers();
        });

        expect(mockVerifyContextPath).not.toHaveBeenCalled();
        expect(result.current.state.isPathVerifying).toBe(false);
    });

    it('does not call the API when contextPath contains invalid characters', async () => {
        const { result } = renderHook(() => useHook(), { wrapper });

        act(() => {
            result.current.dispatch({ type: 'UPDATE_FORM', patch: { contextPath: '/my api' } });
        });

        await act(async () => {
            jest.runAllTimers();
        });

        expect(mockVerifyContextPath).not.toHaveBeenCalled();
    });

    it('does not call the API for a virtual host until host and path are valid', async () => {
        const { result } = renderHook(() => useHook(), { wrapper });

        act(() => {
            result.current.dispatch({ type: 'UPDATE_FORM', patch: { contextPath: '/valid-path', virtualHostsEnabled: true } });
        });

        await act(async () => {
            jest.runAllTimers();
        });

        expect(mockVerifyContextPath).not.toHaveBeenCalled();
    });

    it('calls the API with the runtime config and path after the debounce delay', async () => {
        const { result } = renderHook(() => useHook(), { wrapper });

        act(() => {
            result.current.dispatch({ type: 'UPDATE_FORM', patch: { contextPath: '/valid-path' } });
        });

        expect(mockVerifyContextPath).not.toHaveBeenCalled();

        await act(async () => {
            jest.runAllTimers();
        });

        expect(mockVerifyContextPath).toHaveBeenCalledTimes(1);
        expect(mockVerifyContextPath).toHaveBeenCalledWith('env-1', [{ path: '/valid-path' }]);
        expect(result.current.state.isPathVerifying).toBe(false);
    });

    it('sets a contextPath error when the API reports the path is already taken', async () => {
        mockVerifyContextPath.mockResolvedValue({ ok: false, reason: 'Path already in use.' });
        const { result } = renderHook(() => useHook(), { wrapper });

        act(() => {
            result.current.dispatch({ type: 'UPDATE_FORM', patch: { contextPath: '/taken-path' } });
        });

        await act(async () => {
            jest.runAllTimers();
        });

        expect(result.current.state.validationErrors['contextPath']).toBe('Path already in use.');
        expect(result.current.state.isPathVerifying).toBe(false);
    });

    it('uses a fallback message when the API returns ok: false without a reason', async () => {
        mockVerifyContextPath.mockResolvedValue({ ok: false });
        const { result } = renderHook(() => useHook(), { wrapper });

        act(() => {
            result.current.dispatch({ type: 'UPDATE_FORM', patch: { contextPath: '/taken-path' } });
        });

        await act(async () => {
            jest.runAllTimers();
        });

        expect(result.current.state.validationErrors['contextPath']).toBe('This context path is already in use by another API.');
    });

    it('clears a previous uniqueness error when the path becomes available', async () => {
        mockVerifyContextPath.mockResolvedValueOnce({ ok: false, reason: 'Already taken.' });
        const { result } = renderHook(() => useHook(), { wrapper });

        // First check: path is taken.
        act(() => {
            result.current.dispatch({ type: 'UPDATE_FORM', patch: { contextPath: '/taken-path' } });
        });
        await act(async () => {
            jest.runAllTimers();
        });
        expect(result.current.state.validationErrors['contextPath']).toBe('Already taken.');

        // Second check: path is now free.
        mockVerifyContextPath.mockResolvedValueOnce({ ok: true });
        act(() => {
            result.current.dispatch({ type: 'UPDATE_FORM', patch: { contextPath: '/free-path' } });
        });
        await act(async () => {
            jest.runAllTimers();
        });

        expect(result.current.state.validationErrors).not.toHaveProperty('contextPath');
    });

    it('does not set an error and resets isPathVerifying on network failure', async () => {
        mockVerifyContextPath.mockRejectedValue(new Error('network error'));
        const { result } = renderHook(() => useHook(), { wrapper });

        act(() => {
            result.current.dispatch({ type: 'UPDATE_FORM', patch: { contextPath: '/valid-path' } });
        });

        await act(async () => {
            jest.runAllTimers();
        });

        expect(result.current.state.isPathVerifying).toBe(false);
        expect(result.current.state.validationErrors).not.toHaveProperty('contextPath');
    });

    it('debounces rapid changes — fires the API only once for the final value', async () => {
        const { result } = renderHook(() => useHook(), { wrapper });

        act(() => {
            result.current.dispatch({ type: 'UPDATE_FORM', patch: { contextPath: '/path-one' } });
        });
        act(() => {
            result.current.dispatch({ type: 'UPDATE_FORM', patch: { contextPath: '/path-two' } });
        });
        act(() => {
            result.current.dispatch({ type: 'UPDATE_FORM', patch: { contextPath: '/path-three' } });
        });

        await act(async () => {
            jest.runAllTimers();
        });

        expect(mockVerifyContextPath).toHaveBeenCalledTimes(1);
        expect(mockVerifyContextPath).toHaveBeenCalledWith('env-1', [{ path: '/path-three' }]);
    });

    it('verifies a virtual host path together with its host', async () => {
        const { result } = renderHook(() => useHook(), { wrapper });

        act(() => {
            result.current.dispatch({
                type: 'UPDATE_FORM',
                patch: {
                    virtualHostsEnabled: true,
                    virtualHosts: [{ id: '1', host: 'vh.example.com', path: '/vh-test', overrideAccess: false }],
                },
            });
        });

        await act(async () => {
            jest.runAllTimers();
        });

        expect(mockVerifyContextPath).toHaveBeenCalledWith('env-1', [{ path: '/vh-test', host: 'vh.example.com' }]);
    });

    it('keeps a host typed after the path check has started', async () => {
        let resolveVerify: (result: { ok: boolean }) => void = () => undefined;
        mockVerifyContextPath.mockImplementation(
            () =>
                new Promise(resolve => {
                    resolveVerify = resolve;
                }),
        );
        const { result } = renderHook(() => useHook(), { wrapper });

        act(() => {
            result.current.dispatch({
                type: 'UPDATE_FORM',
                patch: {
                    virtualHostsEnabled: true,
                    virtualHosts: [{ id: '1', host: 'old.example.com', path: '/vh-test', overrideAccess: false }],
                },
            });
        });

        await act(async () => {
            jest.advanceTimersByTime(500);
        });

        act(() => {
            result.current.dispatch({ type: 'UPDATE_VIRTUAL_HOST', index: 0, patch: { host: 'newer.example.com' } });
        });

        await act(async () => {
            resolveVerify({ ok: true });
        });

        expect(result.current.state.form.virtualHosts[0]?.host).toBe('newer.example.com');
    });
});
