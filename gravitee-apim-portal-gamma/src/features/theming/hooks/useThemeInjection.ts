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
import { useEffect, type RefObject } from 'react';

import type { PortalTheme, ThemeTokens, CustomVariable } from '../types';
import { resolveActiveTokens } from '../utils/resolve-tokens';

function toKebab(str: string): string {
    return str.replace(/([a-z])([A-Z])/g, '$1-$2').toLowerCase();
}

function buildCssVariables(
    tokens: ThemeTokens,
    customVariables: readonly CustomVariable[],
    isDark: boolean,
): Map<string, string> {
    const vars = new Map<string, string>();

    for (const [key, value] of Object.entries(tokens.colors)) {
        vars.set(`--portal-color-${toKebab(key)}`, value);
    }

    for (const [key, value] of Object.entries(tokens.typography)) {
        vars.set(`--portal-font-${toKebab(key)}`, String(value));
    }

    for (const [key, value] of Object.entries(tokens.spacing)) {
        vars.set(`--portal-spacing-${toKebab(key)}`, value);
    }

    for (const [key, value] of Object.entries(tokens.layout)) {
        vars.set(`--portal-layout-${toKebab(key)}`, value);
    }

    for (const cv of customVariables) {
        const safeName = cv.name.replace(/[^a-zA-Z0-9-_]/g, '-').toLowerCase();
        vars.set(`--portal-custom-${safeName}`, isDark ? cv.darkValue : cv.lightValue);
    }

    return vars;
}

export function useThemeInjection(
    rootRef: RefObject<HTMLElement | null>,
    theme: PortalTheme | null | undefined,
    resolvedDark: boolean,
): void {
    useEffect(() => {
        const el = rootRef.current;
        if (!el || !theme) return;

        const tokens = resolveActiveTokens(theme, resolvedDark);
        const vars = buildCssVariables(tokens, theme.customVariables, resolvedDark);

        for (const [prop, value] of vars) {
            el.style.setProperty(prop, value);
        }

        if (resolvedDark) {
            el.classList.add('dark');
        } else {
            el.classList.remove('dark');
        }

        return () => {
            for (const prop of vars.keys()) {
                el.style.removeProperty(prop);
            }
            el.classList.remove('dark');
        };
    }, [rootRef, theme, resolvedDark]);
}
