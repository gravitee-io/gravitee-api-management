/*
 * Copyright (C) 2026 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import { act, renderHook, waitFor } from '@testing-library/react';

import { notify } from '../../shared/notify/notify';
import { createDefaultThemeDocument } from '../storage/migrate-legacy-theme';
import { getTheme, saveTheme } from '../storage/theme.storage';
import { usePortalTheme } from './usePortalTheme';

jest.mock('../storage/theme.storage', () => ({
    getTheme: jest.fn(),
    saveTheme: jest.fn(),
}));

const PORTAL_ID = 'test-portal';

describe('usePortalTheme', () => {
    const defaultTheme = createDefaultThemeDocument(PORTAL_ID);

    beforeEach(() => {
        jest.useFakeTimers();
        jest.clearAllMocks();
        jest.clearAllTimers();
        jest.mocked(getTheme).mockResolvedValue(defaultTheme);
        jest.mocked(saveTheme).mockResolvedValue(undefined);
        jest.spyOn(notify, 'error').mockImplementation(() => undefined);
    });

    afterEach(() => {
        jest.useRealTimers();
        jest.restoreAllMocks();
    });

    it('does not call saveTheme on initial load when autoSave is enabled', async () => {
        renderHook(() => usePortalTheme(PORTAL_ID, { autoSave: true }));

        await waitFor(() => {
            expect(getTheme).toHaveBeenCalledWith(PORTAL_ID);
        });

        act(() => {
            jest.advanceTimersByTime(1000);
        });

        expect(saveTheme).not.toHaveBeenCalled();
    });

    it('calls saveTheme after debounce when a token is updated', async () => {
        const { result } = renderHook(() => usePortalTheme(PORTAL_ID, { autoSave: true }));

        await waitFor(() => {
            expect(result.current.loading).toBe(false);
        });

        act(() => {
            result.current.updateFoundationToken('light', 'primary', '#ff0000');
        });

        expect(saveTheme).not.toHaveBeenCalled();

        act(() => {
            jest.advanceTimersByTime(500);
        });

        expect(saveTheme).toHaveBeenCalledTimes(1);
        expect(saveTheme).toHaveBeenCalledWith(
            expect.objectContaining({
                foundation: expect.objectContaining({
                    light: expect.objectContaining({ primary: '#ff0000' }),
                }),
            }),
        );
    });

    it('save() flushes immediately without waiting for debounce', async () => {
        const { result } = renderHook(() => usePortalTheme(PORTAL_ID, { autoSave: true }));

        await waitFor(() => {
            expect(result.current.loading).toBe(false);
        });

        act(() => {
            result.current.updateFoundationToken('light', 'primary', '#00ff00');
        });

        await act(async () => {
            await result.current.save();
        });

        expect(saveTheme).toHaveBeenCalledTimes(1);
        expect(saveTheme).toHaveBeenCalledWith(
            expect.objectContaining({
                foundation: expect.objectContaining({
                    light: expect.objectContaining({ primary: '#00ff00' }),
                }),
            }),
        );

        act(() => {
            jest.advanceTimersByTime(500);
        });

        expect(saveTheme).toHaveBeenCalledTimes(1);
    });

    it('does not persist on token change when autoSave is disabled', async () => {
        const { result } = renderHook(() => usePortalTheme(PORTAL_ID));

        await waitFor(() => {
            expect(result.current.loading).toBe(false);
        });

        act(() => {
            result.current.updateFoundationToken('light', 'primary', '#ff0000');
        });

        act(() => {
            jest.advanceTimersByTime(1000);
        });

        expect(saveTheme).not.toHaveBeenCalled();
    });
});
