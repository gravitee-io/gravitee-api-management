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

import { resolveFoundation } from '../defaults/foundation-defaults';
import type {
    CustomVariableDefinition,
    ElementModeTokens,
    FoundationTokens,
    PortalThemeDocument,
    ThemeColorMode,
} from '../types';
import { getTheme, saveTheme } from '../storage/theme.storage';
import { createDefaultThemeDocument } from '../storage/migrate-legacy-theme';

export interface UsePortalThemeReturn {
    readonly theme: PortalThemeDocument;
    readonly loading: boolean;
    readonly getResolvedFoundation: (mode: 'light' | 'dark') => FoundationTokens;
    readonly updateFoundationToken: (
        mode: 'light' | 'dark',
        key: keyof FoundationTokens,
        value: string,
    ) => void;
    readonly clearFoundationToken: (mode: 'light' | 'dark', key: keyof FoundationTokens) => void;
    readonly updateElementToken: (
        elementId: string,
        mode: 'light' | 'dark',
        property: string,
        value: string,
        variant?: string,
    ) => void;
    readonly clearElementToken: (
        elementId: string,
        mode: 'light' | 'dark',
        property: string,
        variant?: string,
    ) => void;
    readonly updateColorMode: (mode: ThemeColorMode) => void;
    readonly addCustomVariable: (variable: CustomVariableDefinition) => void;
    readonly updateCustomVariable: (name: string, patch: Partial<Omit<CustomVariableDefinition, 'name'>>) => void;
    readonly removeCustomVariable: (name: string) => void;
    readonly bindInstanceOverride: (instanceId: string, prop: string, customVarName: string) => void;
    readonly unbindInstanceOverride: (instanceId: string, prop: string) => void;
    readonly getInstanceOverride: (instanceId: string) => Record<string, string>;
    readonly replaceTheme: (partial: Partial<PortalThemeDocument>) => void;
    readonly save: () => Promise<void>;
    readonly reset: () => void;
}

function deleteModeToken(
    modeTokens: Record<string, string>,
    property: string,
): Record<string, string> {
    const next = { ...modeTokens };
    delete next[property];
    return next;
}

export function usePortalTheme(portalId: string): UsePortalThemeReturn {
    const [theme, setTheme] = useState<PortalThemeDocument>(() => createDefaultThemeDocument(portalId));
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

    const getResolvedFoundation = useCallback(
        (mode: 'light' | 'dark') => resolveFoundation(theme.foundation[mode], mode),
        [theme.foundation],
    );

    const updateFoundationToken = useCallback((
        mode: 'light' | 'dark',
        key: keyof FoundationTokens,
        value: string,
    ) => {
        setTheme(prev => ({
            ...prev,
            foundation: {
                ...prev.foundation,
                [mode]: { ...prev.foundation[mode], [key]: value },
            },
        }));
    }, []);

    const clearFoundationToken = useCallback((
        mode: 'light' | 'dark',
        key: keyof FoundationTokens,
    ) => {
        setTheme(prev => {
            const modeTokens = { ...prev.foundation[mode] };
            delete modeTokens[key];
            return {
                ...prev,
                foundation: {
                    ...prev.foundation,
                    [mode]: modeTokens,
                },
            };
        });
    }, []);

    const updateElementToken = useCallback((
        elementId: string,
        mode: 'light' | 'dark',
        property: string,
        value: string,
        variant?: string,
    ) => {
        setTheme(prev => {
            const elements = { ...prev.elements };
            if (variant) {
                const existing = (elements[elementId] ?? {}) as Record<string, ElementModeTokens>;
                const variantTokens = existing[variant] ?? { light: {}, dark: {} };
                elements[elementId] = {
                    ...existing,
                    [variant]: {
                        ...variantTokens,
                        [mode]: { ...variantTokens[mode], [property]: value },
                    },
                };
            } else {
                const existing = (elements[elementId] ?? { light: {}, dark: {} }) as ElementModeTokens;
                elements[elementId] = {
                    ...existing,
                    [mode]: { ...existing[mode], [property]: value },
                };
            }
            return { ...prev, elements };
        });
    }, []);

    const clearElementToken = useCallback((
        elementId: string,
        mode: 'light' | 'dark',
        property: string,
        variant?: string,
    ) => {
        setTheme(prev => {
            const elements = { ...prev.elements };
            if (variant) {
                const existing = (elements[elementId] ?? {}) as Record<string, ElementModeTokens>;
                const variantTokens = existing[variant];
                if (!variantTokens) {
                    return prev;
                }
                elements[elementId] = {
                    ...existing,
                    [variant]: {
                        ...variantTokens,
                        [mode]: deleteModeToken(variantTokens[mode] ?? {}, property),
                    },
                };
            } else {
                const existing = (elements[elementId] ?? { light: {}, dark: {} }) as ElementModeTokens;
                elements[elementId] = {
                    ...existing,
                    [mode]: deleteModeToken(existing[mode] ?? {}, property),
                };
            }
            return { ...prev, elements };
        });
    }, []);

    const updateColorMode = useCallback((activeMode: ThemeColorMode) => {
        setTheme(prev => ({ ...prev, activeMode }));
    }, []);

    const addCustomVariable = useCallback((variable: CustomVariableDefinition) => {
        setTheme(prev => ({
            ...prev,
            customVariables: [...prev.customVariables, variable],
        }));
    }, []);

    const updateCustomVariable = useCallback((name: string, patch: Partial<Omit<CustomVariableDefinition, 'name'>>) => {
        setTheme(prev => ({
            ...prev,
            customVariables: prev.customVariables.map(cv =>
                cv.name === name ? { ...cv, ...patch } : cv,
            ),
        }));
    }, []);

    const removeCustomVariable = useCallback((name: string) => {
        setTheme(prev => ({
            ...prev,
            customVariables: prev.customVariables.filter(cv => cv.name !== name),
        }));
    }, []);

    const bindInstanceOverride = useCallback((instanceId: string, prop: string, customVarName: string) => {
        setTheme(prev => ({
            ...prev,
            instanceOverrides: {
                ...prev.instanceOverrides,
                [instanceId]: {
                    ...(prev.instanceOverrides[instanceId] ?? {}),
                    [prop]: customVarName,
                },
            },
        }));
    }, []);

    const unbindInstanceOverride = useCallback((instanceId: string, prop: string) => {
        setTheme(prev => {
            const current = prev.instanceOverrides[instanceId];
            if (!current || !(prop in current)) {
                return prev;
            }
            const nextInstance = { ...current };
            delete nextInstance[prop];
            const nextOverrides = { ...prev.instanceOverrides };
            if (Object.keys(nextInstance).length === 0) {
                delete nextOverrides[instanceId];
            } else {
                nextOverrides[instanceId] = nextInstance;
            }
            return { ...prev, instanceOverrides: nextOverrides };
        });
    }, []);

    const getInstanceOverride = useCallback((instanceId: string) => {
        return theme.instanceOverrides[instanceId] ?? {};
    }, [theme.instanceOverrides]);

    const replaceTheme = useCallback((partial: Partial<PortalThemeDocument>) => {
        setTheme(prev => ({ ...prev, ...partial }));
    }, []);

    const save = useCallback(async () => {
        await saveTheme(theme);
    }, [theme]);

    const reset = useCallback(() => {
        setTheme(createDefaultThemeDocument(portalId));
    }, [portalId]);

    return {
        theme,
        loading,
        getResolvedFoundation,
        updateFoundationToken,
        clearFoundationToken,
        updateElementToken,
        clearElementToken,
        updateColorMode,
        addCustomVariable,
        updateCustomVariable,
        removeCustomVariable,
        bindInstanceOverride,
        unbindInstanceOverride,
        getInstanceOverride,
        replaceTheme,
        save,
        reset,
    };
}
