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
import { useCallback, useEffect, useState } from 'react';

import type { PortalTheme, ThemeTokens, ThemeTokenCategory, CustomVariable, ThemeColorMode } from '../types';
import { getTheme, saveTheme } from '../storage/theme.storage';
import { createDefaultTheme } from '../storage/default-theme';

export interface UsePortalThemeReturn {
    readonly theme: PortalTheme;
    readonly loading: boolean;
    readonly updateToken: <C extends ThemeTokenCategory>(
        mode: 'light' | 'dark',
        category: C,
        key: keyof ThemeTokens[C],
        value: string | number,
    ) => void;
    readonly updateColorMode: (mode: ThemeColorMode) => void;
    readonly addCustomVariable: (variable: CustomVariable) => void;
    readonly updateCustomVariable: (id: string, patch: Partial<Omit<CustomVariable, 'id'>>) => void;
    readonly removeCustomVariable: (id: string) => void;
    readonly replaceTheme: (partial: Partial<PortalTheme>) => void;
    readonly save: () => Promise<void>;
    readonly reset: () => void;
}

export function usePortalTheme(portalId: string): UsePortalThemeReturn {
    const [theme, setTheme] = useState<PortalTheme>(() => createDefaultTheme(portalId));
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        let cancelled = false;
        setLoading(true);
        void getTheme(portalId).then(loaded => {
            if (!cancelled) {
                setTheme(loaded);
                setLoading(false);
            }
        });
        return () => { cancelled = true; };
    }, [portalId]);

    const updateToken = useCallback(<C extends ThemeTokenCategory>(
        mode: 'light' | 'dark',
        category: C,
        key: keyof ThemeTokens[C],
        value: string | number,
    ) => {
        setTheme(prev => ({
            ...prev,
            tokens: {
                ...prev.tokens,
                [mode]: {
                    ...prev.tokens[mode],
                    [category]: {
                        ...prev.tokens[mode][category],
                        [key]: value,
                    },
                },
            },
        }));
    }, []);

    const updateColorMode = useCallback((activeMode: ThemeColorMode) => {
        setTheme(prev => ({ ...prev, activeMode }));
    }, []);

    const addCustomVariable = useCallback((variable: CustomVariable) => {
        setTheme(prev => ({
            ...prev,
            customVariables: [...prev.customVariables, variable],
        }));
    }, []);

    const updateCustomVariable = useCallback((id: string, patch: Partial<Omit<CustomVariable, 'id'>>) => {
        setTheme(prev => ({
            ...prev,
            customVariables: prev.customVariables.map(cv =>
                cv.id === id ? { ...cv, ...patch } : cv,
            ),
        }));
    }, []);

    const removeCustomVariable = useCallback((id: string) => {
        setTheme(prev => ({
            ...prev,
            customVariables: prev.customVariables.filter(cv => cv.id !== id),
        }));
    }, []);

    const replaceTheme = useCallback((partial: Partial<PortalTheme>) => {
        setTheme(prev => ({ ...prev, ...partial }));
    }, []);

    const save = useCallback(async () => {
        await saveTheme(theme);
    }, [theme]);

    const reset = useCallback(() => {
        setTheme(createDefaultTheme(portalId));
    }, [portalId]);

    return {
        theme,
        loading,
        updateToken,
        updateColorMode,
        addCustomVariable,
        updateCustomVariable,
        removeCustomVariable,
        replaceTheme,
        save,
        reset,
    };
}
