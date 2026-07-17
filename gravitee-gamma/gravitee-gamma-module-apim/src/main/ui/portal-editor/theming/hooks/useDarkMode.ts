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
import { useCallback, useEffect, useMemo, useState } from 'react';

import type { ThemeColorMode } from '../types';

const STORAGE_KEY = 'gravitee-portal-gamma-color-mode';

function readStoredMode(): ThemeColorMode {
    try {
        const stored = localStorage.getItem(STORAGE_KEY);
        if (stored === 'light' || stored === 'dark' || stored === 'system') {
            return stored;
        }
    } catch {
        // localStorage unavailable
    }
    return 'system';
}

export function getSystemPrefersDark(): boolean {
    if (typeof window === 'undefined') return false;
    return window.matchMedia('(prefers-color-scheme: dark)').matches;
}

export function resolvePreviewColorMode(activeMode: ThemeColorMode): 'light' | 'dark' {
    if (activeMode === 'dark') return 'dark';
    if (activeMode === 'light') return 'light';
    return getSystemPrefersDark() ? 'dark' : 'light';
}

export interface DarkModeState {
    readonly colorMode: ThemeColorMode;
    readonly isDark: boolean;
    readonly setColorMode: (mode: ThemeColorMode) => void;
    readonly toggle: () => void;
}

export function useDarkMode(overrideMode?: ThemeColorMode): DarkModeState {
    const [colorMode, setColorModeState] = useState<ThemeColorMode>(() => overrideMode ?? readStoredMode());
    const [systemPrefersDark, setSystemPrefersDark] = useState(getSystemPrefersDark);

    useEffect(() => {
        const mql = window.matchMedia('(prefers-color-scheme: dark)');
        const handler = (e: MediaQueryListEvent) => setSystemPrefersDark(e.matches);
        mql.addEventListener('change', handler);
        return () => mql.removeEventListener('change', handler);
    }, []);

    useEffect(() => {
        if (overrideMode != null) {
            setColorModeState(overrideMode);
        }
    }, [overrideMode]);

    const setColorMode = useCallback((mode: ThemeColorMode) => {
        setColorModeState(mode);
        try {
            localStorage.setItem(STORAGE_KEY, mode);
        } catch {
            // localStorage unavailable
        }
    }, []);

    const isDark = useMemo(() => {
        if (colorMode === 'system') return systemPrefersDark;
        return colorMode === 'dark';
    }, [colorMode, systemPrefersDark]);

    const toggle = useCallback(() => {
        setColorMode(isDark ? 'light' : 'dark');
    }, [isDark, setColorMode]);

    return { colorMode, isDark, setColorMode, toggle };
}
