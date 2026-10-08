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
import { ThemeProvider, useTheme } from '@gravitee/graphene-core';
import { act, renderHook, screen } from '@testing-library/react';
import type { ReactNode } from 'react';

import { showStartupError } from './startup-error';

const STARTUP_FAILURE = new Error('Failed to fetch bootstrap config: 401');

describe('showStartupError', () => {
    let error: jest.SpyInstance;

    beforeEach(() => {
        error = jest.spyOn(console, 'error').mockImplementation(() => undefined);
        document.body.innerHTML = '<div id="root"></div>';
    });

    afterEach(() => {
        document.documentElement.classList.remove('dark');
        jest.restoreAllMocks();
    });

    it('should replace the console with a message and a button to reload the page, without the technical error', () => {
        showStartupError(STARTUP_FAILURE);

        expect(screen.getByRole('alert').textContent).toContain("The console couldn't start");
        expect(screen.getByRole('button', { name: 'Reload page' })).toBeTruthy();
        expect(screen.queryByText(/bootstrap config/)).toBeNull();
    });

    it('should title the page with a heading', () => {
        showStartupError(STARTUP_FAILURE);

        expect(screen.getByRole('heading', { level: 1, name: "The console couldn't start" })).toBeTruthy();
    });

    it('should stay centered and show its icon without the console stylesheet or any other file', () => {
        showStartupError(STARTUP_FAILURE);

        const page = screen.getByRole('main');
        expect(page.style.display).toBe('flex');
        expect(page.style.alignItems).toBe('center');
        expect(page.style.justifyContent).toBe('center');
        expect(page.querySelector('svg')?.getAttribute('aria-hidden')).toBe('true');
    });

    it('should log the technical error in the browser console', () => {
        showStartupError(STARTUP_FAILURE);

        expect(error).toHaveBeenCalledWith(expect.stringContaining('[Startup]'), STARTUP_FAILURE);
    });

    it('should follow the theme picked in the console, where Graphene saves it', () => {
        const { result, unmount } = renderHook(() => useTheme(), {
            wrapper: ({ children }: { children: ReactNode }) => <ThemeProvider defaultMode="light">{children}</ThemeProvider>,
        });
        act(() => result.current.setMode('dark'));
        unmount();
        document.documentElement.classList.remove('dark');

        showStartupError(STARTUP_FAILURE);

        expect(document.documentElement.classList.contains('dark')).toBe(true);
    });

    it('should follow the system theme when none was picked', () => {
        jest.spyOn(window, 'matchMedia').mockReturnValue({ matches: true } as MediaQueryList);

        showStartupError(STARTUP_FAILURE);

        expect(document.documentElement.classList.contains('dark')).toBe(true);
    });
});
